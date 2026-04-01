package com.xuesinuo.muppet.webclient;

import org.springframework.stereotype.Component;

import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClient;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VersionWebClient {

    private final WebClient webClient;

    public Future<String> queryLatestVersion() {
        return webClient.get(443, "www.xuesinuo.com", "/muppet-print/version")
                .ssl(true)
                .send()
                .map(response -> response.bodyAsString());
    }
}
