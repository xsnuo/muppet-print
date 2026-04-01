package com.xuesinuo.muppet.webclient;

import org.springframework.stereotype.Component;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SignedWebClient {

    private final WebClient webClient;
    private final ServerAccessConfig serverAccessConfig;

    public Future<HttpResponse<Buffer>> querySigned(String mac) {
        if (!serverAccessConfig.isConfigured()) {
            return Future.failedFuture("Server signed URL is not configured.");
        }
        var request = webClient.getAbs(serverAccessConfig.signedUrl(mac));
        String token = serverAccessConfig.token();
        if (!token.isBlank()) {
            request.putHeader("Muppet-Token", token);
        }
        return request.send();
    }
}
