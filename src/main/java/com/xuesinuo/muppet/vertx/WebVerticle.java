package com.xuesinuo.muppet.vertx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Component;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Web配置
 * 
 * @author xuesinuo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebVerticle extends AbstractVerticle {
    private final Router router;
    public static volatile int port = 8080;
    private static volatile CompletableFuture<Void> startupFuture = CompletableFuture.completedFuture(null);

    public static synchronized void resetStartupSignal() {
        startupFuture = new CompletableFuture<>();
    }

    public static void awaitStartupResult(long timeoutMillis) {
        try {
            startupFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeoutException) {
            throw new RuntimeException("Web startup timeout", timeoutException);
        } catch (Exception exception) {
            Throwable rootCause = exception.getCause() == null ? exception : exception.getCause();
            throw new RuntimeException("Web startup failed", rootCause);
        }
    }

    @Override
    public void start() {
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port)
                .onSuccess(hs -> {
                    log.info("Vert.x run on port: " + hs.actualPort());
                    startupFuture.complete(null);
                })
                .onFailure(error -> {
                    log.error("Vert.x startup failed on port: {}", port, error);
                    startupFuture.completeExceptionally(error);
                });
    }
}
