#!/usr/bin/env python3
"""验证生产 JAR 的配置、HTTP 边界、日志及 SIGTERM；只管理本脚本启动的进程。"""

import argparse
import json
import os
from pathlib import Path
import re
import secrets
import shlex
import shutil
import subprocess
import tempfile
import time
from urllib.error import HTTPError
from urllib.request import Request, urlopen
import zipfile
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]
VERSION = ET.parse(ROOT / "pom.xml").getroot().find("{http://maven.apache.org/POM/4.0.0}version").text
JAR = ROOT / f"endurance-server/target/endurance-server-{VERSION}.jar"


def request(port, path, token=None):
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    call = Request(f"http://127.0.0.1:{port}{path}", headers=headers)
    try:
        response = urlopen(call, timeout=3)
    except HTTPError as failure:
        response = failure
    with response:
        return response.status, response.headers, response.read().decode("utf-8")


def environment(local):
    result = os.environ.copy()
    result.update(ENDURANCE_POSTGRES_ENABLED="false", ENDURANCE_REDIS_ENABLED="false",
                  ENDURANCE_AUTH_ENABLED="false", ENDURANCE_OTLP_TRACES_ENABLED="false",
                  ENDURANCE_OTLP_METRICS_ENABLED="false", SPRING_PROFILES_ACTIVE="smoke")
    token = None
    if local:
        # 只读取字面量赋值，不执行 .env.local 中的 shell 代码。
        for line in (ROOT / ".env.local").read_text().splitlines():
            line = line.strip().removeprefix("export ")
            if not line or line.startswith("#"):
                continue
            key, value = line.split("=", 1)
            if not re.fullmatch(r"[A-Z][A-Z0-9_]*", key):
                raise ValueError("Unsupported local environment assignment")
            parts = shlex.split(value, comments=True)
            result[key] = parts[0] if parts else ""
        for key in ("ENDURANCE_POSTGRES_ENABLED", "ENDURANCE_REDIS_ENABLED"):
            if result.get(key) != "true":
                raise ValueError(f"Local smoke test requires {key}=true")
        # 仅本次测试使用的随机凭据，既不覆盖本地凭据，也不输出到日志。
        token = secrets.token_urlsafe(48)
        result.update(ENDURANCE_AUTH_ENABLED="true", ENDURANCE_API_TOKEN=token)
    return result, token


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--local", action="store_true", help="读取 .env.local，验收本地 PG/Redis 和认证")
    args = parser.parse_args()
    if not JAR.is_file():
        raise SystemExit("请先在后端目录执行 mvn clean verify")
    with zipfile.ZipFile(JAR) as archive:
        names = archive.namelist()
        assert not any("db/verification/" in name or "IntegrationTest" in name for name in names)
        assert not any("logback-classic" in name or "log4j-to-slf4j" in name or "/lombok-" in name for name in names)
    env, token = environment(args.local)
    with tempfile.TemporaryDirectory(prefix="endurance-smoke-") as directory:
        directory = Path(directory)
        output = directory / "process.log"
        env.update(ENDURANCE_PORT="0", ENDURANCE_LOG_PATH=str(directory / "logs"))
        with output.open("w") as log:
            process = subprocess.Popen(["java", "-jar", str(JAR)], cwd=ROOT, env=env,
                                       stdout=log, stderr=subprocess.STDOUT)
            try:
                end = time.monotonic() + 30
                port = None
                while time.monotonic() < end:
                    if process.poll() is not None:
                        raise AssertionError("服务在启动完成前退出")
                    match = re.search(r"Tomcat started on port (\d+)", output.read_text())
                    if match:
                        port = int(match.group(1))
                        break
                    time.sleep(0.1)
                assert port is not None, "服务启动超时"
                status, headers, body = request(port, "/api/system/info")
                assert status == 200
                envelope = json.loads(body)
                trace = headers["X-Trace-Id"]
                assert re.fullmatch(r"[0-9a-f]{32}", trace)
                assert envelope["success"] and envelope["code"] == "OK"
                assert envelope["traceId"] == trace
                assert envelope["data"]["stage"] == "foundation"
                assert not any(envelope["data"]["capabilities"].values())
                status, _, body = request(port, "/actuator/health")
                assert status == 200 and json.loads(body)["status"] == "UP"
                assert request(port, "/")[0] == 200
                status, headers, body = request(port, "/actuator/metrics")
                assert status == 401 and json.loads(body)["code"] == "AUTH_REQUIRED"
                assert json.loads(body)["traceId"] == headers["X-Trace-Id"]
                assert "Set-Cookie" not in headers
                if token:
                    assert request(port, "/actuator/metrics/executor.pool.core", token)[0] == 200
                    assert request(port, "/actuator/env", token)[0] == 403
                    # 测试控制器绝不能进入生产 JAR。
                    status, _, body = request(port, "/api/_test/echo", token)
                    assert status == 404 and json.loads(body)["code"] == "RESOURCE_NOT_FOUND"
                info = directory / "logs/info.log"
                end = time.monotonic() + 2
                while time.monotonic() < end and (not info.exists() or trace not in info.read_text()):
                    time.sleep(0.05)
                assert trace in info.read_text(), "日志缺少对应的 Trace ID"
                for category in ("info", "error", "socket"):
                    assert (directory / f"logs/{category}.log").is_file()
                if token:
                    assert token not in output.read_text() and token not in info.read_text()
                started = time.monotonic()
                process.terminate()
                process.wait(timeout=15)
                assert process.returncode in (0, 143, -15), "服务没有正常响应 SIGTERM"
                elapsed = time.monotonic() - started
                print(f"PASS: {'local PG/Redis/auth' if args.local else 'no external services'}; "
                      f"packaged JAR, HTTP, trace/logs, SIGTERM ({elapsed:.2f}s)")
            except BaseException:
                failure = ROOT / "endurance-server/target/smoke-failure.log"
                shutil.copyfile(output, failure)
                print(f"失败日志保存在 {failure}；凭据不会在终端回显")
                raise
            finally:
                if process.poll() is None:
                    process.terminate()
                    try:
                        process.wait(timeout=15)
                    except subprocess.TimeoutExpired:
                        process.kill()
                        process.wait(timeout=5)


if __name__ == "__main__":
    main()
