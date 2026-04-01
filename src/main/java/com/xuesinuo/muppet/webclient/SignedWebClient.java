package com.xuesinuo.muppet.webclient;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignedWebClient {

    private final WebClient webClient;
    private final ServerAccessConfig serverAccessConfig;

    public Future<WebClientResult<List<SignedPrinterRecord>>> querySigned(String mac) {
        if (!serverAccessConfig.isConfigured()) {
            log.error("server-api signed failed: release.server.host is not configured");
            return Future.succeededFuture(WebClientResult.fail("SYSTEM_ERROR", "Server signed URL is not configured."));
        }
        String url = serverAccessConfig.signedUrl(mac);
        var request = webClient.getAbs(url);
        String token = serverAccessConfig.token();
        if (!token.isBlank()) {
            request.putHeader("Muppet-Token", token);
        }
        log.info("server-api signed request: url={}, mac={}", url, mac);
        return request.send()
                .map(response -> {
                    String responseBody = response.bodyAsString();
                    log.info("server-api signed response: status={}, body={}", response.statusCode(), responseBody);
                    WebClientResult<List<SignedPrinterRecord>> result = parseEnvelopePrinters(response.statusCode(), responseBody);
                    if (!result.isSuccess()) {
                        log.error("server-api signed business failed: code={}, message={}", result.getCode(), result.getMessage());
                    }
                    return result;
                })
                .recover(error -> {
                    log.error("server-api signed request exception", error);
                    return Future.succeededFuture(WebClientResult.fail("SYSTEM_ERROR", error.getMessage()));
                });
    }

    private WebClientResult<List<SignedPrinterRecord>> parseEnvelopePrinters(int statusCode, String body) {
        if (statusCode != 200) {
            return WebClientResult.fail("SYSTEM_ERROR", "HTTP status: " + statusCode);
        }
        JsonObject jsonObject;
        try {
            jsonObject = new JsonObject(body == null ? "" : body);
        } catch (Exception exception) {
            return WebClientResult.fail("SYSTEM_ERROR", "Invalid response body");
        }

        String code = jsonObject.getString("code", "SYSTEM_ERROR");
        String message = jsonObject.getString("message", "");
        if (!"SUCCESS".equals(code)) {
            return WebClientResult.fail(code, message);
        }

        JsonArray printersArray = null;
        JsonObject data = jsonObject.getJsonObject("data");
        if (data != null) {
            printersArray = data.getJsonArray("printers");
        }
        if (printersArray == null) {
            printersArray = jsonObject.getJsonArray("printers");
        }
        List<SignedPrinterRecord> printerRecords = new ArrayList<>();
        if (printersArray != null) {
            for (int i = 0; i < printersArray.size(); i++) {
                JsonObject item = printersArray.getJsonObject(i);
                if (item == null) {
                    continue;
                }
                SignedPrinterRecord record = new SignedPrinterRecord();
                record.setGroup(item.getString("group", ""));
                record.setMac(item.getString("mac", ""));
                record.setPcName(item.getString("pcName", ""));
                record.setPrinterName(item.getString("printerName", ""));
                record.setUrl(item.getString("url", ""));
                record.setPageWidth(item.getValue("pageWidth") == null ? "" : String.valueOf(item.getValue("pageWidth")));
                record.setPageHeight(item.getValue("pageHeight") == null ? "" : String.valueOf(item.getValue("pageHeight")));
                printerRecords.add(record);
            }
        }
        return WebClientResult.success(printerRecords);
    }
}
