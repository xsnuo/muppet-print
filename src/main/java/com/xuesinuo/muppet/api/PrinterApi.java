package com.xuesinuo.muppet.api;

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
}
