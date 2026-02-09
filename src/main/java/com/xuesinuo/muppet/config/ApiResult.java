package com.xuesinuo.muppet.config;

import java.io.Closeable;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

import io.vertx.core.json.Json;
import lombok.Data;

/**
 * API返回结果
 */
@Data
public class ApiResult<T> {
    /** 编号 */
    private ApiResultCode code = ApiResultCode.SYSTEM_ERROR;
    /** 提示信息 */
    private String message;
    /** 数据信息 */
    private T data;

    /**
     * 设置一个非成功的状态
     * 
     * @param code 不可以设置SUCCESS
     */
    public void setCode(ApiResultCode code) {
        if (code == ApiResultCode.SUCCESS) {
            throw new RuntimeException("ApiResultCode can not set Success. Please use ApiResult.ok().");
        }
        this.code = code;
    }

    /**
     * 快速构造一个成功的结果
     * 
     * @param <T>  返回的数据类型
     * @param data 数据
     * @return API返回结果
     */
    public static <T> String ok(T data) {
        ApiResult<T> apiResult = new ApiResult<>();
        apiResult.code = ApiResultCode.SUCCESS;
        if (data == null) {
            return Json.encode(apiResult);
        }
        checkDataType(data.getClass());
        apiResult.setData(data);
        return Json.encode(apiResult);
    }

    /**
     * 快速构造一个成功的结果
     * 
     * @return API返回结果
     */
    public static String ok() {
        ApiResult<?> apiResult = new ApiResult<>();
        apiResult.code = ApiResultCode.SUCCESS;
        return Json.encode(apiResult);
    }

    private static void checkDataType(Class<?> dataType) {
        String badResultTypeName = null;
        if (!Object.class.isAssignableFrom(dataType)) {// 非Object，即基本数据类型
            if (dataType != void.class) {
                badResultTypeName = dataType.toString();
            }
        } else if (CharSequence.class.isAssignableFrom(dataType)) {// 字符串
            badResultTypeName = "string";
        } else if (Character.class.isAssignableFrom(dataType)) {// 字符
            badResultTypeName = "char";
        } else if (TemporalAccessor.class.isAssignableFrom(dataType)) {// JAVA8 时间存取器
            badResultTypeName = "date";
        } else if (Date.class.isAssignableFrom(dataType)) {// 日期
            badResultTypeName = "date";
        } else if (Number.class.isAssignableFrom(dataType)) {// 数字
            badResultTypeName = "number";
        } else if (Boolean.class.isAssignableFrom(dataType)) {// 布尔
            badResultTypeName = "boolean";
        } else if (Iterable.class.isAssignableFrom(dataType)) {// 集合
            badResultTypeName = "collection(iterable)";
        } else if (dataType.isArray()) {// 数组
            badResultTypeName = "array";
        } else if (Closeable.class.isAssignableFrom(dataType)) {// 可关闭的
            badResultTypeName = "closeable";
        }
        if (badResultTypeName != null) {
            throw new RuntimeException("forbidden result type: " + badResultTypeName);
        }
    }
}