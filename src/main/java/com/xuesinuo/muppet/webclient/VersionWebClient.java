package com.xuesinuo.muppet.webclient;

import org.springframework.stereotype.Component;

import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class VersionWebClient {

    private final WebClient webClient;

    public Future<WebClientResult<String>> queryLatestVersion() {
        log.info("server-api version request: host=www.xuesinuo.com, path=/muppet-print/version");
        return webClient.get(443, "www.xuesinuo.com", "/muppet-print/version")
                .ssl(true)
                .send()
                .map(response -> {
                    String responseBody = response.bodyAsString();
                    log.info("server-api version response: status={}, body={}", response.statusCode(), responseBody);
                    WebClientResult<String> result;
                    if (response.statusCode() == 200) {
                        result = WebClientResult.<String>success(responseBody);
                    } else {
                        result = WebClientResult.<String>fail("SYSTEM_ERROR", "HTTP status: " + response.statusCode());
                    }
                    if (!result.isSuccess()) {
                        log.error("server-api version business failed: code={}, message={}", result.getCode(), result.getMessage());
                    }
                    return result;
                })
                .recover(error -> {
                    log.error("server-api version request exception", error);
                    return Future.succeededFuture(WebClientResult.<String>fail("SYSTEM_ERROR", error.getMessage()));
                });
    }
}
