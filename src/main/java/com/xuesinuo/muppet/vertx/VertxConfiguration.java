package com.xuesinuo.muppet.vertx;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.vertx.core.Vertx;

/**
 * Vertx配置
 * 
 * @author xuesinuo
 */
@Configuration
public class VertxConfiguration {
    @Bean
    public Vertx vertx() {
        return Vertx.vertx();
    }
}
