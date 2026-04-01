package com.xuesinuo.muppet.webclient;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 注册本机打印机请求参数
 */
@Data
public class SigninPrinterRequest {

    /** 分组编码 */
    private String group;

    /** 设备MAC地址 */
    private String mac;

    /** 设备主机名 */
    private String pcName;

    /** 打印机名称 */
    private String printerName;

    /** 本地服务地址 */
    private String url;

    /** 页面宽度（毫米） */
    private BigDecimal pageWidth;

    /** 页面高度（毫米） */
    private BigDecimal pageHeight;
}
