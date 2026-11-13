package io.github.onyxyu.endurance.common.json;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 复用 Spring 管理的 Jackson 3 Mapper；与 HTTP JSON 配置保持一致。
 */
public final class Json {
    private final JsonMapper mapper;

    public Json(JsonMapper mapper) {
        this.mapper = mapper;
    }

    // 对象与文本
    public String write(Object value) {
        return convert(() -> mapper.writeValueAsString(value));
    }

    public <T> T read(String json, Class<T> type) {
        return convert(() -> mapper.readValue(json, type));
    }

    public <T> T read(String json, TypeReference<T> type) {
        return convert(() -> mapper.readValue(json, type));
    }

    // 容器与泛型
    public <T> List<T> readList(String json, Class<T> elementType) {
        return convert(
                () -> mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, elementType)));
    }

    public Map<String, Object> readMap(String json) {
        return read(json, new TypeReference<>() {
        });
    }

    public <T> T convert(Object value, Class<T> type) {
        return convert(() -> mapper.convertValue(value, type));
    }

    public <T> T convert(Object value, TypeReference<T> type) {
        return convert(() -> mapper.convertValue(value, type));
    }

    // 树结构转换
    public JsonNode tree(String json) {
        return convert(() -> mapper.readTree(json));
    }

    public JsonNode toTree(Object value) {
        return convert(() -> mapper.valueToTree(value));
    }

    public <T> T fromTree(JsonNode tree, Class<T> type) {
        return convert(() -> mapper.treeToValue(tree, type));
    }

    private <T> T convert(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new JsonConversionException(exception);
        }
    }
}
