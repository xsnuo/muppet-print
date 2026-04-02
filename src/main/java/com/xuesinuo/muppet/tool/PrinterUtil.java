package com.xuesinuo.muppet.tool;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.xuesinuo.muppet.config.exceptions.ServiceException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.print.*;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 打印机工具类
 * 
 * 提供打印机列表获取和 HTML 静默打印功能
 */
@Slf4j
public class PrinterUtil {

    /**
     * 打印机信息
     */
    @AllArgsConstructor
    @Data
    public static class PrinterInfo {
        public String id; // 打印机唯一标识
        public String name; // 打印机名称
        public String description; // 打印机描述
    }

    /**
     * 获取本地可用的打印机列表
     * 
     * @return 打印机信息列表
     */
    public static List<PrinterInfo> listPrinters() {
        List<PrinterInfo> printers = new ArrayList<>();
        try {
            // 获取所有可用的打印服务
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            for (PrintService service : services) {
                String name = service.getName();
                // 尝试获取打印机描述信息
                String description = "";
                try {
                    Object desc = service.getAttribute(javax.print.attribute.standard.PrinterInfo.class);
                    if (desc != null) {
                        description = desc.toString();
                    }
                } catch (Exception e) {
                    // 忽略获取描述失败的情况
                }
                printers.add(new PrinterInfo(name, name, description));
                log.debug("Found printer: {}", name);
            }
            log.info("Found {} printer(s)", printers.size());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return printers;
    }

    /**
     * 将 HTML 静默打印到指定打印机 使用 Playwright (Chrome 内核) 直接通过 CDP 协议打印到物理打印机
     * 
     * @param html            HTML 内容
     * @param imports         相关依赖文件内容映射，key 为文件名，value 为文件内容
     * @param printerNameOrId 打印机名称或 ID
     * @param pageWidthMm     页面宽度（毫米）
     * @param pageHeightMm    页面高度（毫米）
     * @param waitJsReady     是否等待 JS 设置 window.printReady 为 true 再打印
     * @return 打印是否成功
     */
    public static void printHtml(
            String html,
            Map<String, String> imports,
            String printerNameOrId,
            double pageWidthMm,
            double pageHeightMm,
            Boolean waitJsReady) {
        String lockKey = printerNameOrId == null ? "__default_printer__" : printerNameOrId;
        synchronized (lockKey.intern()) {
            Path tempDir = null;
            try {
                // 1. 准备工作目录和 HTML 文件
                tempDir = Files.createTempDirectory("muppetprint_");
                Path htmlFile = tempDir.resolve("index.html");
                Files.writeString(htmlFile, ensureUtf8MetaCharset(html), StandardCharsets.UTF_8);
                if (imports != null) {
                    for (Map.Entry<String, String> importEntry : imports.entrySet()) {
                        Path importPath = tempDir.resolve(importEntry.getKey());
                        if (importPath.getParent() != null) {
                            Files.createDirectories(importPath.getParent());
                        }
                        Files.writeString(importPath, importEntry.getValue(), StandardCharsets.UTF_8);
                    }
                }

                // 2. 复制资源文件到临时目录
                copyImportsToTempDirectory(tempDir);

                // 3. 确定打印机名称
                String printerName = printerNameOrId;
                if (printerName == null || printerName.isBlank()) {
                    PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
                    if (defaultService == null) {
                        throw new IllegalStateException("No default printer found");
                    }
                    printerName = defaultService.getName();
                }

                log.info("Target printer: {}", printerName);
                log.info("Printing HTML with Chromium to printer: {}", printerName);

                // 4. 使用 Playwright 通过 CDP 协议直接打印
                printWithChromeCDP(htmlFile, printerName, pageWidthMm, pageHeightMm, tempDir, waitJsReady);
            } catch (Exception e) {
                if (e instanceof java.lang.IllegalStateException
                        && e.getMessage() != null
                        && (e.getMessage().contains("Printer not found") || e.getMessage().contains("No default printer found"))) {
                    throw new ServiceException(e.getMessage());
                }
                throw new RuntimeException(e);
            } finally {
                if (tempDir != null) {
                    log.info("临时文件在: {}", tempDir.toAbsolutePath());
                    cleanupDir(tempDir);
                    log.info("临时文件已清理: {}", tempDir.toAbsolutePath());
                }
            }
        }
    }

    /**
     * 打印PDF（二进制文件）
     * 
     * @param pdfData         PDF文件的字节数组
     * @param printerNameOrId 打印机名称或ID
     */
    public static void printPdf(byte[] pdfData, String printerName) {
        String lockKey = printerName == null || printerName.isBlank() ? "__default_printer__" : printerName;
        synchronized (lockKey.intern()) {
            // 查找打印机
            PrintService targetPrinter = findPrinter(printerName);
            if (targetPrinter == null) {
                if (printerName == null || printerName.isBlank()) {
                    throw new IllegalStateException("No default printer found");
                }
                throw new IllegalStateException("Printer not found: " + printerName);
            }
            log.info("Sending PDF to printer: {}", targetPrinter.getName());
            // 使用 Java Print Service 直接打印 PDF 数据
            boolean success = printPdfData(pdfData, targetPrinter);
            if (!success) {
                throw new ServiceException("print pdf failed: " + targetPrinter.getName());
            }
        }
    }

    private static void copyImportsToTempDirectory(Path tempDir) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
                    PrinterUtil.class.getClassLoader());
            Resource[] resources = resolver.getResources("classpath*:imports/**");
            Set<String> copiedRelativePaths = new HashSet<>();

            for (Resource resource : resources) {
                if (resource == null || !resource.isReadable()) {
                    continue;
                }

                String relative = resolveRelativeImportsPath(resource);
                if (relative == null || relative.isBlank()) {
                    continue;
                }

                if (!copiedRelativePaths.add(relative)) {
                    continue;
                }

                Path target = tempDir.resolve(relative).normalize();
                if (!target.startsWith(tempDir)) {
                    continue;
                }

                Files.createDirectories(target.getParent());
                try (var in = resource.getInputStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            if (!copiedRelativePaths.isEmpty()) {
                log.info("Copied {} imports file(s) from classpath", copiedRelativePaths.size());
                return;
            }

            log.warn("No readable resources found under classpath*:imports/**");
        } catch (Exception e) {
            log.warn("Failed to copy imports from classpath source", e);
        }
    }

    private static String ensureUtf8MetaCharset(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        String normalized = html.toLowerCase();
        if (normalized.contains("charset=utf-8") || normalized.contains("charset=\"utf-8\"")
                || normalized.contains("charset='utf-8'") || normalized.contains("<meta charset=")) {
            return html;
        }

        String metaTag = "<meta charset=\"UTF-8\">\n";
        int headIndex = normalized.indexOf("<head>");
        if (headIndex >= 0) {
            int insertIndex = headIndex + "<head>".length();
            return html.substring(0, insertIndex) + "\n" + metaTag + html.substring(insertIndex);
        }

        int htmlIndex = normalized.indexOf("<html>");
        if (htmlIndex >= 0) {
            int insertIndex = htmlIndex + "<html>".length();
            return html.substring(0, insertIndex) + "\n<head>\n" + metaTag + "</head>\n" + html.substring(insertIndex);
        }
        return "<head>\n" + metaTag + "</head>\n" + html;
    }

    private static String resolveRelativeImportsPath(Resource resource) {
        try {
            String location = resource.getURL().toExternalForm();
            int marker = location.lastIndexOf("imports/");
            if (marker < 0) {
                return null;
            }

            String relative = location.substring(marker + "imports/".length());
            int queryIndex = relative.indexOf('?');
            if (queryIndex >= 0) {
                relative = relative.substring(0, queryIndex);
            }
            int fragmentIndex = relative.indexOf('#');
            if (fragmentIndex >= 0) {
                relative = relative.substring(0, fragmentIndex);
            }

            relative = URLDecoder.decode(relative, StandardCharsets.UTF_8);
            relative = relative.replace('\\', '/');
            while (relative.startsWith("/")) {
                relative = relative.substring(1);
            }

            if (relative.isBlank() || relative.endsWith("/")) {
                return null;
            }
            return relative;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 使用 Playwright (Chrome 内核) 渲染 HTML 并直接打印到物理打印机 通过 Playwright 生成 PDF 字节流，然后使用 Java Print Service API 发送到打印机
     */
    private static void printWithChromeCDP(Path htmlFile, String printerName,
            double pageWidthMm, double pageHeightMm, Path tempDir, Boolean waitJsReady) throws Exception {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true));
            Page page = browser.newPage();
            // 导航到 HTML 文件
            String fileUrl = "file://" + htmlFile.toAbsolutePath().toString();
            page.navigate(fileUrl);
            page.waitForLoadState();
            log.info("Page loaded, generating PDF for printing...");
            if (waitJsReady != null && waitJsReady) {
                page.waitForFunction("() => window.printReady === true");
            }
            // 使用 Playwright 生成 PDF 字节数组
            Page.PdfOptions options = new Page.PdfOptions()
                    .setWidth(mm(pageWidthMm))
                    .setHeight(mm(pageHeightMm))
                    .setPrintBackground(true)
                    .setMargin(new com.microsoft.playwright.options.Margin()
                            .setTop("0")
                            .setBottom("0")
                            .setLeft("0")
                            .setRight("0"));

            byte[] pdfData = page.pdf(options);
            log.info("PDF generated by Playwright, size: {} bytes", pdfData.length);
            // 保存PDF文件用于调试
            Path pdfFile = tempDir.resolve("print_output.pdf");
            Files.write(pdfFile, pdfData);
            log.info("===== PDF文件已保存: {} =====", pdfFile.toAbsolutePath());
            browser.close();
            printPdf(pdfData, printerName);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 查找指定名称的打印机
     */
    private static PrintService findPrinter(String printerName) {
        if (printerName == null || printerName.isBlank()) {
            return PrintServiceLookup.lookupDefaultPrintService();
        }

        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService service : services) {
            if (service.getName().equals(printerName) ||
                    service.getName().equalsIgnoreCase(printerName)) {
                return service;
            }
        }

        return null;
    }

    /**
     * 使用 Java Print Service API 打印 PDF 数据
     */
    private static boolean printPdfData(byte[] pdfData, PrintService printer) {
        // 在Windows上直接使用 javax.print 发送 PDF 原始字节常常不被驱动支持，
        // 改为使用 PDFBox + PrinterJob 进行页面渲染后打印，更稳定可靠。
        org.apache.pdfbox.pdmodel.PDDocument document = null;
        try {
            document = org.apache.pdfbox.pdmodel.PDDocument.load(pdfData);

            PrinterJob printerJob = PrinterJob.getPrinterJob();
            printerJob.setPrintService(printer);

            // 使用 PDFPageable 以页为单位打印，避免自己拆分页
            org.apache.pdfbox.printing.PDFPageable pageable = new org.apache.pdfbox.printing.PDFPageable(document);
            printerJob.setPageable(pageable);

            // 不弹出打印对话框，直接提交
            printerJob.print();

            log.info("Print job submitted successfully to {} with PDFBox", printer.getName());
            return true;
        } catch (Exception e) {
            log.error("Failed to print PDF via PDFBox", e);
            return false;
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (Exception ignore) {}
            }
        }
    }

    private static void cleanupDir(Path dir) {
        try {
            if (dir == null || !Files.exists(dir)) {
                return;
            }
            Files.walk(dir)
                    .sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("清理临时文件失败: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("清理临时目录失败: {}", dir, e);
        }
    }

    /**
     * 将毫米转换为 Playwright 的尺寸字符串
     */
    private static String mm(double v) {
        return String.format("%smm", stripTrailingZeros(v));
    }

    /**
     * 去除小数末尾的零
     */
    private static String stripTrailingZeros(double v) {
        String s = Double.toString(v);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }

}
