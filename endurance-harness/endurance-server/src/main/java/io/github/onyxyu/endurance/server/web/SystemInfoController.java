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
    private final ServiceProperties serviceProperties;
    private final Responses apiResponses;

    public SystemInfoController(ServiceProperties serviceProperties, Responses apiResponses) {
        this.serviceProperties = serviceProperties;
        this.apiResponses = apiResponses;
    }

    @GetMapping("/api/system/info")
    public ApiResponse<SystemInfoResponse> info() {
        return apiResponses.ok(
                new SystemInfoResponse(
                        serviceProperties.name(),
                        serviceProperties.version(),
                        serviceProperties.stage(),
                        List.of(
                                "common",
                                "api",
                                "core",
                                "models",
                                "tools",
                                "memory",
                                "sandbox",
                                "repository",
                                "server"
                        ),
                        Map.ofEntries(
                                Map.entry("agentExecution", false),
                                Map.entry("modelAccess", false),
                                Map.entry("persistence", false),
                                Map.entry("sandboxExecution", false)
                        )
                )
        );
    }

}
