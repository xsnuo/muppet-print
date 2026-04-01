package com.xuesinuo.muppet.webclient;

import java.util.Map;

import org.springframework.stereotype.Component;

import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClient;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LogWebClient {

    private final WebClient webClient;
    private final ServerAccessConfig serverAccessConfig;

    public Future<Void> sendErrorLog(String version, String message) {
        if (!serverAccessConfig.isConfigured()) {
            return Future.succeededFuture();
        }
        var request = webClient.postAbs(serverAccessConfig.errorLogUrl())
                .putHeader("Content-Type", "application/json");
        String serverToken = serverAccessConfig.token();
        if (!serverToken.isEmpty()) {
            request.putHeader("Muppet-Token", serverToken);
        }
        return request.sendJson(Map.of(
                "level", "error",
                "version", version,
                "message", message)).mapEmpty();
    }
}
