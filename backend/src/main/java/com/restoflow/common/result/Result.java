package com.restoflow.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回结构：{ code, message, data }
 * <p>code 在 V1 恒为 0（预留字段），成败一律看 HTTP 状态码。
 * <p>泛型 T 表示 data 里装什么类型，例如 Result&lt;User&gt;、Result&lt;List&lt;Table&gt;&gt;。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    /** 成功：code = 0，message = "ok" */
    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data);
    }

    /** 失败：data 为 null，message 为给用户看的提示 */
    public static <T> Result<T> error(String message) {
        return new Result<>(0, message, null);
    }
}
