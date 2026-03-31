package com.xuesinuo.muppet.ui;

import java.util.function.Consumer;

import org.springframework.stereotype.Component;

@Component
public class UiMessageService {

    private volatile String defaultMessage = "";
    private volatile String currentMessage = "";
    private volatile Consumer<String> renderer = message -> {
    };

    public synchronized void bindRenderer(Consumer<String> renderer) {
        if (renderer == null) {
            this.renderer = message -> {
            };
            return;
        }
        this.renderer = renderer;
        this.renderer.accept(currentMessage);
    }

    public synchronized void setDefaultMessage(String message) {
        defaultMessage = normalize(message);
        showMessage(defaultMessage);
    }

    public synchronized void showMessage(String message) {
        currentMessage = normalize(message);
        renderer.accept(currentMessage);
    }

    public synchronized void restoreDefaultMessage() {
        showMessage(defaultMessage);
    }

    public String getCurrentMessage() {
        return currentMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    private String normalize(String message) {
        return message == null ? "" : message;
    }
}
