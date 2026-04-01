package com.xuesinuo.muppet.webclient;

import lombok.Data;

/**
 * WebClient统一返回结果
 */
@Data
public class WebClientResult<T> {

    /** 是否成功 */
    private boolean success;

    /** 业务结果码 */
    private String code;

    /** 结果说明 */
    private String message;

    /** 业务数据 */
    private T data;

    public static <T> WebClientResult<T> success(T data) {
        WebClientResult<T> result = new WebClientResult<>();
        result.setSuccess(true);
        result.setCode("SUCCESS");
        result.setData(data);
        return result;
    }

    public static <T> WebClientResult<T> fail(String code, String message) {
        WebClientResult<T> result = new WebClientResult<>();
        result.setSuccess(false);
        result.setCode(code == null || code.isBlank() ? "SYSTEM_ERROR" : code);
        result.setMessage(message == null ? "" : message);
        return result;
    }
}
