package com.xuesinuo.muppet.webclient;

import java.util.Map;

import org.springframework.stereotype.Component;

import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogWebClient {

    private final WebClient webClient;
    private final ServerAccessConfig serverAccessConfig;

    public Future<WebClientResult<Void>> sendErrorLog(String version, String message) {
        if (!serverAccessConfig.isConfigured()) {
            log.info("server-api log skip: release.server.host is not configured");
            return Future.succeededFuture(WebClientResult.<Void>success(null));
        }
        String url = serverAccessConfig.errorLogUrl();
        var request = webClient.postAbs(url)
                .putHeader("Content-Type", "application/json");
        String serverToken = serverAccessConfig.token();
        if (!serverToken.isEmpty()) {
            request.putHeader("Muppet-Token", serverToken);
        }
        Map<String, Object> requestBody = Map.of(
                "level", "error",
                "version", version,
                "message", message);
        log.info("server-api log request: url={}, body={}", url, requestBody);
        return request.sendJson(requestBody)
                .map(response -> {
                    String responseBody = response.bodyAsString();
                    log.info("server-api log response: status={}, body={}", response.statusCode(), responseBody);
                    WebClientResult<Void> result;
                    if (response.statusCode() == 200) {
                        result = WebClientResult.<Void>success(null);
                    } else {
                        result = WebClientResult.<Void>fail("SYSTEM_ERROR", "HTTP status: " + response.statusCode());
                    }
                    if (!result.isSuccess()) {
                        log.error("server-api log business failed: code={}, message={}", result.getCode(), result.getMessage());
                    }
                    return result;
                })
                .recover(error -> {
                    log.error("server-api log request exception", error);
                    return Future.succeededFuture(WebClientResult.<Void>fail("SYSTEM_ERROR", error.getMessage()));
                });
    }
}
