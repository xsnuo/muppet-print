package com.xuesinuo.muppet.webclient;

import org.springframework.stereotype.Component;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignoutWebClient {

    private final WebClient webClient;
    private final ServerAccessConfig serverAccessConfig;

    public Future<WebClientResult<Void>> signout(SignoutPrinterRequest requestBody) {
        if (!serverAccessConfig.isConfigured()) {
            log.error("server-api signout failed: release.server.host is not configured");
            return Future.succeededFuture(WebClientResult.fail("SYSTEM_ERROR", "Server signout URL is not configured."));
        }
        String url = serverAccessConfig.signoutUrl();
        var request = webClient.postAbs(url)
                .putHeader("Content-Type", "application/json");
        String token = serverAccessConfig.token();
        if (!token.isBlank()) {
            request.putHeader("Muppet-Token", token);
        }
        JsonObject body = JsonObject.mapFrom(requestBody);
        log.info("server-api signout request: url={}, body={}", url, body.encode());
        return request.sendJsonObject(body)
                .map(response -> {
                    String responseBody = response.bodyAsString();
                    log.info("server-api signout response: status={}, body={}", response.statusCode(), responseBody);
                    WebClientResult<Void> result = parseEnvelopeNoData(response.statusCode(), responseBody);
                    if (!result.isSuccess()) {
                        log.error("server-api signout business failed: code={}, message={}", result.getCode(), result.getMessage());
                    }
                    return result;
                })
                .recover(error -> {
                    log.error("server-api signout request exception", error);
                    return Future.succeededFuture(WebClientResult.fail("SYSTEM_ERROR", error.getMessage()));
                });
    }

    private WebClientResult<Void> parseEnvelopeNoData(int statusCode, String body) {
        if (statusCode != 200) {
            return WebClientResult.fail("SYSTEM_ERROR", "HTTP status: " + statusCode);
        }
        try {
            JsonObject jsonObject = new JsonObject(body == null ? "" : body);
            String code = jsonObject.getString("code", "SYSTEM_ERROR");
            String message = jsonObject.getString("message", "");
            if ("SUCCESS".equals(code)) {
                return WebClientResult.success(null);
            }
            return WebClientResult.fail(code, message);
        } catch (Exception exception) {
            return WebClientResult.fail("SYSTEM_ERROR", "Invalid response body");
        }
    }
}
