package io.github.onyxyu.endurance.server.web;

import io.github.onyxyu.endurance.server.config.properties.ServiceProperties;
import io.github.onyxyu.endurance.server.web.dto.SystemInfoResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 供工程入口检查连接使用；不代表 Agent 功能已经实现。 */
@RestController
public class SystemInfoController {
    private final ServiceProperties service;

    public SystemInfoController(ServiceProperties service) {
        this.service = service;
    }

    @GetMapping("/api/system/info")
    public SystemInfoResponse info() {
        return new SystemInfoResponse(
                service.name(), service.version(), service.stage(),
                List.of("common", "api", "core", "models", "tools", "memory", "sandbox", "repository", "server"),
                Map.of("agentExecution", false, "modelAccess", false, "persistence", false, "sandboxExecution", false));
    }

}
