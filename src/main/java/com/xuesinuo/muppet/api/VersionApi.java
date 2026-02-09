package com.xuesinuo.muppet.api;

import org.springframework.stereotype.Component;

import com.xuesinuo.muppet.config.ApiResult;

import io.vertx.ext.web.Router;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VersionApi {
    private final Router router;

    @PostConstruct
    public void start() {
        getVersion();
    }

    @Data
    @AllArgsConstructor
    public static class Version {
        private String version;
        private String revision;
    }

    /** 当前版本 */
    private static final Version VERSION = new Version("1.0.0", "1");

    /** 程序版本号 */
    private void getVersion() {
        router.route("/api/version").handler(http -> {
            http.response().write(ApiResult.ok(VERSION));
            http.next();
        });
    }
}
