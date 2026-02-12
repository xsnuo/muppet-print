package com.xuesinuo.muppet.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.xuesinuo.muppet.config.ApiResult;
import com.xuesinuo.muppet.config.exceptions.ParamException;
import com.xuesinuo.muppet.tool.PrinterUtil;
import com.xuesinuo.xtool.Np;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrinterApi {

    private final Router router;
    private final Vertx vertx;

    @PostConstruct
    public void start() {
        getAllPrinters();
        print();
        printPDF();
    }

    /** 获取所有打印机信息 */
    private void getAllPrinters() {
        router.route("/api/getAllPrinters").handler(http -> {
            vertx.executeBlocking(() -> {
                List<PrinterUtil.PrinterInfo> printers = PrinterUtil.listPrinters();
                return printers;
            }).onSuccess(printers -> {
                Map<String, Object> data = Map.of("printers", printers);
                http.response().write(ApiResult.ok(data));
            }).onFailure(error -> http.fail(error))
                    .onComplete(r -> http.next());
        });
    }

    @Data
    public static class PrintParam {
        private String html;
        private String printerNameOrId;
        private Double pageWidth;
        private Double pageHeight;
        private Map<String, String> imports;
        private Boolean waitJsReady;
    }

    /** 打印 */
    private void print() {
        router.route("/api/print").handler(http -> {
            PrintParam printParam = Np.i(http.body())
                    .x(x -> x.asString())
                    .x(x -> Json.decodeValue(x, PrintParam.class))
                    .o(new PrintParam());
            String html = printParam.getHtml();
            String printerNameOrId = printParam.getPrinterNameOrId();
            Double pageWidth = printParam.getPageWidth();
            Double pageHeight = printParam.getPageHeight();
            Map<String, String> imports = printParam.getImports();
            if (html == null || html.isBlank()
                    || printerNameOrId == null || printerNameOrId.isBlank()
                    || pageWidth == null || pageHeight == null) {
                throw new ParamException("must provide: html, printerNameOrId, pageWidth, pageHeight");
            }
            vertx.executeBlocking(() -> {
                PrinterUtil.printHtml(html, imports, printParam.getPrinterNameOrId(), printParam.getPageWidth(),
                        printParam.getPageHeight(), printParam.getWaitJsReady());
                return null;
            }).onSuccess(r -> http.response().write(ApiResult.ok()))
                    .onFailure(error -> http.fail(error))
                    .onComplete(r -> http.next());
        });
    }

    /** 打印PDF文件 */
    private void printPDF() {
        router.route("/api/printPDF").handler(http -> {
            // 获取类似SpringMvc中MutipartFile的参数
            http.request().setExpectMultipart(true);
            http.request().endHandler(v -> {
                String printerNameOrId = http.request().getFormAttribute("printerNameOrId");
                for (io.vertx.ext.web.FileUpload fileUpload : http.fileUploads()) {
                    String tempFilePath = fileUpload.uploadedFileName();
                    // 读取文件内容
                    try {
                        byte[] pdfData = Files.readAllBytes(Path.of(tempFilePath));
                        if (pdfData == null || printerNameOrId == null || printerNameOrId.isBlank()) {
                            throw new ParamException("must provide: pdf file and printerNameOrId");
                        }
                        PrinterUtil.printPdf(pdfData, printerNameOrId);
                    } catch (IOException e) {
                        throw new RuntimeException("/api/printPDF读取PDF文件失败", e);
                    }
                }
                http.response().write(ApiResult.ok());
            });
        });
    }
}
