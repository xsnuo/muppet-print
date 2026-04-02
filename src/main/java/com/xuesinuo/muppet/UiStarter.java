package com.xuesinuo.muppet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.xuesinuo.muppet.ui.AwtUiSupport;

import javax.swing.UIManager;

@SpringBootApplication
public class UiStarter {

    public static void main(String[] args) {
        AwtUiSupport.initializeGlobalUiFont();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SpringApplication application = new SpringApplication(UiStarter.class);
        application.setHeadless(false);
        application.run(args);
    }
}
