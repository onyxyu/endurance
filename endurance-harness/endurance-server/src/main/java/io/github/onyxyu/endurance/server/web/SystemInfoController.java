package io.github.onyxyu.endurance.server.web;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 供工程入口检查连接使用；不代表 Agent 功能已经实现。 */
@RestController
public class SystemInfoController {
    private final String version;

    public SystemInfoController(@Value("${endurance.version}") String version) {
        this.version = version;
    }

    @GetMapping("/api/system/info")
    public SystemInfoResponse info() {
        return new SystemInfoResponse(
                "Endurance Harness", version, "bootstrap",
                List.of("common", "api", "core", "models", "tools", "memory", "sandbox", "repository", "server"),
                Map.of("agentExecution", false, "modelAccess", false, "persistence", false, "sandboxExecution", false));
    }

    public record SystemInfoResponse(
            String name,
            String version,
            String stage,
            List<String> modules,
            Map<String, Boolean> capabilities) {}
}
