package com.xuesinuo.muppet.vertx;

import java.util.List;

import org.springframework.context.annotation.Configuration;

import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * Vertx配置
 * 
 * @author xuesinuo
 */
@Configuration
@RequiredArgsConstructor
public class VerticleConfiguration {
    private final Vertx vertx;
    private final List<Verticle> verticles;

    @PostConstruct
    public void deployVerticle() {
        for (Verticle verticle : verticles) {
            vertx.deployVerticle(verticle);
        }
    }
}
