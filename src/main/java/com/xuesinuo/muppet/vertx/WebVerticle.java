package com.xuesinuo.muppet.vertx;

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

    @Override
    public void start() {
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port)
                .onSuccess(hs -> {
                    log.info("Vert.x run on port: " + hs.actualPort());
                });
    }
}
