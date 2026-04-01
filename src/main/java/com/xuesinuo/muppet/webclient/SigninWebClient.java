package com.xuesinuo.muppet.webclient;

import org.springframework.stereotype.Component;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SigninWebClient {

    private final WebClient webClient;
    private final ServerAccessConfig serverAccessConfig;

    public Future<HttpResponse<Buffer>> signin(JsonObject body) {
        if (!serverAccessConfig.isConfigured()) {
            return Future.failedFuture("Server signin URL is not configured.");
        }
        var request = webClient.postAbs(serverAccessConfig.signinUrl())
                .putHeader("Content-Type", "application/json");
        String token = serverAccessConfig.token();
        if (!token.isBlank()) {
            request.putHeader("Muppet-Token", token);
        }
        return request.sendJsonObject(body);
    }
}
