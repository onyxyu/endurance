package io.github.onyxyu.endurance.server.support.json;

/** JSON 转换错误；异常消息不包含原始载荷。 */
public class JsonConversionException extends RuntimeException {
    public JsonConversionException(Throwable cause) { super("JSON conversion failed", cause); }
}
