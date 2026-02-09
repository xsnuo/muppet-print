package com.xuesinuo.muppet.tool;

import java.util.List;
import java.util.Map;

/**
 * PrinterUtil 使用示例
 */
public class PrinterUtilExample {

    public static void main(String[] args) {
        System.out.println("=== 打印机工具示例 ===\n");

        // 示例 1: 获取打印机列表
        System.out.println("1. 获取可用打印机列表：");
        List<PrinterUtil.PrinterInfo> printers = PrinterUtil.listPrinters();

        if (printers.isEmpty()) {
            System.out.println("   未找到可用的打印机");
            return;
        }

        for (PrinterUtil.PrinterInfo printer : printers) {
            System.out.println("   - " + printer);
        }
        System.out.println();

        // 示例 2: 准备 HTML 内容（与 PdfGeneratorExample 相同的样式）
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;700&display=swap" rel="stylesheet">
                    <style>
                        body {
                            font-family: 'Roboto', Arial, sans-serif;
                            margin: 20mm;
                            padding: 0;
                            background: white;
                        }
                        h1 {
                            color: #2c3e50;
                            border-bottom: 3px solid #3498db;
                            padding-bottom: 10px;
                        }
                        .content {
                            margin: 20px 0;
                            line-height: 1.6;
                        }
                        .logo {
                            width: 80px;
                            height: 80px;
                            margin: 20px 0;
                        }
                        .footer {
                            margin-top: 40px;
                            font-size: 12px;
                            color: #7f8c8d;
                            border-top: 1px solid #bdc3c7;
                            padding-top: 10px;
                        }
                    </style>
                </head>
                <body>
                    <h1>打印测试文档</h1>
                    <div class="content">
                        <p>这是一个使用 <strong>PrinterUtil</strong> 工具进行静默打印的示例文档。</p>
                        <p>支持的特性：</p>
                        <ul>
                            <li>内联 CSS 样式</li>
                            <li>网络字体（Google Fonts）</li>
                            <li>本地资源文件（如图片）</li>
                            <li>自定义页面尺寸</li>
                            <li>零页边距（通过 CSS 控制实际边距）</li>
                        </ul>
                    </div>
                    <img src="logo.png" alt="Logo" class="logo">
                    <div class="footer">
                        打印时间: """ + java.time.LocalDateTime.now() + """
                    </div>
                </body>
                </html>
                """;

        // 示例 4: 打印到指定打印机
        System.out.println("2. 执行打印：");
        if (!printers.isEmpty()) {
            // 使用第一台可用打印机
            String printerName = printers.get(0).name;
            System.out.println("   目标打印机: " + printerName);
            System.out.println("   页面尺寸: 210mm x 297mm (A4)");
            PrinterUtil.printHtml(
                    html,
                    Map.of(),
                    printerName,
                    210, // A4 宽度
                    297, // A4 高度
                    false);
            System.out.println("   ✓ 打印任务已提交");
        }
        System.out.println();

    }
}
