package com.xuesinuo.muppet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.xuesinuo.muppet.ui.AwtUiSupport;

@SpringBootApplication
public class UiStarter {

    public static void main(String[] args) {
        AwtUiSupport.initializeGlobalUiFont();
        SpringApplication application = new SpringApplication(UiStarter.class);
        application.setHeadless(false);
        application.run(args);
    }
}
