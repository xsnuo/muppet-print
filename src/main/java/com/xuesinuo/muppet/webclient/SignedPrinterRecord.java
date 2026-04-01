package com.xuesinuo.muppet.webclient;

import lombok.Data;

/**
 * 已注册打印机记录
 */
@Data
public class SignedPrinterRecord {

    /** 分组编码 */
    private String group;

    /** 设备MAC地址 */
    private String mac;

    /** 设备主机名 */
    private String pcName;

    /** 打印机名称 */
    private String printerName;

    /** 打印服务地址 */
    private String url;

    /** 页面宽度（毫米） */
    private String pageWidth;

    /** 页面高度（毫米） */
    private String pageHeight;
}
