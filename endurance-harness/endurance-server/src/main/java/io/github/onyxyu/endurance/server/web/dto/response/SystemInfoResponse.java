package io.github.onyxyu.endurance.server.web.dto.response;

import java.util.List;
import java.util.Map;

/**
 * 工程信息的 HTTP 表示，不能作为运行核心或持久化实体使用。
 */
public record SystemInfoResponse(String name,
                                 String version,
                                 String stage,
                                 List<String> modules,
                                 Map<String, Boolean> capabilities) {
}
