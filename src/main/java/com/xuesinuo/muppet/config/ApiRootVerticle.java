package com.xuesinuo.muppet.config;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.xuesinuo.muppet.UiStarter;
import com.xuesinuo.muppet.api.VersionApi;
import com.xuesinuo.muppet.config.exceptions.ParamException;
import com.xuesinuo.muppet.config.exceptions.ServiceException;

import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiRootVerticle {

    private final Router router;
    private final WebClient webClient;

    @Value("${release.server.host:}")
    private String releaseServerHost;

    @Value("${release.server.prefix:}")
    private String releaseServerPrefix;

    @Value("${release.server.token:}")
    private String releaseServerToken;

    @PostConstruct
    public void start() {
        router.route("/api/*").order(Integer.MIN_VALUE)
                .handler(BodyHandler.create().setBodyLimit(1024 * 1024))// 请求体大小限制
                .handler(http -> {
                    http.response().setChunked(true).putHeader("Content-Type", "application/json");
                    log.info("API => " + http.request().uri());
                    http.next();
                });

        router.route("/api/*").order(Integer.MAX_VALUE)
                .handler(http -> {
                    http.end();
                });

        router.route("/api/*").order(Integer.MIN_VALUE)
                .failureHandler(http -> {
                    ApiResult<?> apiResult = new ApiResult<>();
                    Throwable t = http.failure();
                    if (t != null) {
                        if (t instanceof ParamException) {
                            apiResult.setCode(ApiResultCode.PARAM_ERROR);
                            apiResult.setMessage("ParamException: " + t.getMessage());
                            http.response().setStatusCode(200).send(Json.encode(apiResult));
                            return;
                        }
                        if (t instanceof ServiceException) {
                            apiResult.setCode(ApiResultCode.SERVICE_ERROR);
                            apiResult.setMessage("ServiceException: " + t.getMessage());
                            http.response().setStatusCode(200).send(Json.encode(apiResult));
                            return;
                        }
                    }
                    String errorId = UUID.randomUUID().toString().substring(0, 8);
                    StringBuilder logBuilder = new StringBuilder();
                    logBuilder.append("MuppetApi error [" + errorId + "]");
                    if (t != null) {
                        logBuilder.append(String.valueOf(t.getMessage())).append("\n");
                        for (StackTraceElement element : t.getStackTrace()) {
                            logBuilder.append(element.toString()).append("\n");
                        }
                    } else {
                        logBuilder.append("Unknown failure\n");
                    }
                    String serverHost = safeTrim(releaseServerHost);
                    if (!serverHost.isEmpty()) {
                        try {
                            var request = webClient.post(443, serverHost, buildReleaseLogPath()).ssl(true);
                            String serverToken = safeTrim(releaseServerToken);
                            if (!serverToken.isEmpty()) {
                                request.putHeader("Muppet-Token", serverToken);
                            }
                            request.sendJson(Map.of(
                                    "level", "error",
                                    "version", VersionApi.VERSION,
                                    "message", logBuilder.toString()))
                                    .onFailure(error -> UiStarter.error("send error log failed."));
                        } catch (Exception error) {
                            UiStarter.error("send error log failed.");
                        }
                    }
                    apiResult.setCode(ApiResultCode.SYSTEM_ERROR);
                    apiResult.setMessage("System error (" + errorId + ").");
                    http.response().setStatusCode(500).send(Json.encode(apiResult));
                });
        // router.errorHandler(500, http -> {});
    }

    private String buildReleaseLogPath() {
        String prefix = safeTrim(releaseServerPrefix);
        if (prefix.isEmpty()) {
            return "/muppet/log";
        }
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix + "/muppet/log";
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
