package com.xuesinuo.muppet.vertx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Component;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebVerticle {

    private final Vertx vertx;
    private final Router router;

    private volatile HttpServer httpServer;
    private volatile int runningPort = -1;

    @PostConstruct
    public void init() {
        httpServer = null;
        runningPort = -1;
    }

    public synchronized void start(int port, long timeoutMillis) {
        if (isRunning()) {
            if (runningPort == port) {
                return;
            }
            throw new RuntimeException("Web is already running on port: " + runningPort);
        }

        CompletableFuture<Void> startupFuture = new CompletableFuture<>();
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port)
                .onSuccess(server -> {
                    httpServer = server;
                    runningPort = server.actualPort();
                    System.out.println("Vert.x run on port: " + runningPort);
                    startupFuture.complete(null);
                })
                .onFailure(error -> {
                    error.printStackTrace();
                    startupFuture.completeExceptionally(error);
                });

        waitStartup(startupFuture, timeoutMillis);
    }

    public synchronized void stop(long timeoutMillis) {
        if (httpServer == null) {
            return;
        }

        HttpServer serverToClose = httpServer;
        CompletableFuture<Void> stopFuture = new CompletableFuture<>();
        serverToClose.close()
                .onSuccess(v -> stopFuture.complete(null))
                .onFailure(stopFuture::completeExceptionally);

        waitStop(stopFuture, timeoutMillis);
        httpServer = null;
        runningPort = -1;
    }

    public boolean isRunning() {
        return httpServer != null;
    }

    public int getRunningPort() {
        return runningPort;
    }

    @PreDestroy
    public void destroy() {
        try {
            stop(3000);
        } catch (Exception ignored) {
        }
    }

    private void waitStartup(CompletableFuture<Void> startupFuture, long timeoutMillis) {
        try {
            startupFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeoutException) {
            throw new RuntimeException("Web startup timeout", timeoutException);
        } catch (Exception exception) {
            Throwable rootCause = exception.getCause() == null ? exception : exception.getCause();
            throw new RuntimeException("Web startup failed", rootCause);
        }
    }

    private void waitStop(CompletableFuture<Void> stopFuture, long timeoutMillis) {
        try {
            stopFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeoutException) {
            throw new RuntimeException("Web stop timeout", timeoutException);
        } catch (Exception exception) {
            Throwable rootCause = exception.getCause() == null ? exception : exception.getCause();
            throw new RuntimeException("Web stop failed", rootCause);
        }
    }
}
