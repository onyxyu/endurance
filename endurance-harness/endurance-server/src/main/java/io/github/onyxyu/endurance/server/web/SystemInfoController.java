package io.github.onyxyu.endurance.server.web;

import io.github.onyxyu.endurance.server.config.properties.ServiceProperties;
import io.github.onyxyu.endurance.server.web.dto.response.ApiResponse;
import io.github.onyxyu.endurance.server.web.dto.response.SystemInfoResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 供工程入口检查连接使用；不代表 Agent 功能已经实现。
 */
@RestController
public class SystemInfoController {
    private final ServiceProperties service;
    private final Responses responses;

    public SystemInfoController(ServiceProperties service, Responses responses) {
        this.service = service;
        this.responses = responses;
    }

    @GetMapping("/api/system/info")
    public ApiResponse<SystemInfoResponse> info() {
        return responses.ok(new SystemInfoResponse(service.name(), service.version(), service.stage(),
                List.of("common", "api", "core", "models", "tools", "memory", "sandbox", "repository", "server"),
                Map.of("agentExecution", false, "modelAccess", false, "persistence", false, "sandboxExecution",
                        false)));
    }

}
