package com.xuesinuo.muppet.webclient;

import lombok.Data;

/**
 * 注销已注册打印机请求参数
 */
@Data
public class SignoutPrinterRequest {

    /** 设备MAC地址 */
    private String mac;

    /** 打印机名称 */
    private String printerName;

    /** 分组编码 */
    private String group;
}
