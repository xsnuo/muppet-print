package com.xuesinuo.muppet.vertx;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.handler.CorsHandler;
import lombok.RequiredArgsConstructor;

/**
 * Vertx Web Router配置
 * 
 * @author xuesinuo
 */
@Configuration
@RequiredArgsConstructor
public class VertxRouterConfiguration {
    private final Vertx vertx;

    @Bean
    public Router router() {
        Router router = Router.router(vertx);
        // 添加CORS处理器，允许所有来源跨域
        router.route().handler(
            CorsHandler.create()
                .addOrigin("*")
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.POST)
                .allowedMethod(HttpMethod.PUT)
                .allowedMethod(HttpMethod.DELETE)
                .allowedMethod(HttpMethod.OPTIONS)
                .allowedHeader("Content-Type")
                .allowedHeader("Authorization")
                .allowCredentials(true)
        );
        return router;
    }
}
