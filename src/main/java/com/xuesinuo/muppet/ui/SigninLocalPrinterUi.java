package com.xuesinuo.muppet.ui;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import io.vertx.core.json.JsonObject;

public class SigninLocalPrinterUi {

    private SigninLocalPrinterUi() {
    }

    public static void show(Frame owner, HttpClient httpClient, URI signinUri, String token, String mac,
            String hostName, String localUrl, List<String> printers) {
        Dialog dialog = new Dialog(owner, "Signin local printer", true);
        if (owner.getIconImage() != null) {
            dialog.setIconImage(owner.getIconImage());
        }
        dialog.setResizable(false);
        dialog.setLayout(null);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dialog.dispose();
            }
        });
        dialog.addNotify();

        Insets insets = dialog.getInsets();
        int titleBarHeight = insets.top;
        int hfs = getHalfFontSize();
        int dialogWidth = Math.max(82 * hfs, 700);
        int dialogHeight = Math.max(43 * hfs, 420);
        dialog.setSize(dialogWidth, dialogHeight);
        dialog.setLocationRelativeTo(owner);

        TextField macField = createReadonlyField(mac);
        TextField pcNameField = createReadonlyField(hostName);
        TextField urlField = createReadonlyField(localUrl);
        TextField pageWidthField = new TextField();
        TextField pageHeightField = new TextField();
        Choice printerChoice = new Choice();
        printerChoice.add("Please select printer name");
        for (String printerName : printers) {
            printerChoice.add(printerName);
        }

        addLabel(dialog, "mac", 2, 1, 14, hfs, titleBarHeight);
        setBounds(macField, 18, 1, 60, hfs, titleBarHeight);
        dialog.add(macField);

        addLabel(dialog, "pc name", 2, 6, 14, hfs, titleBarHeight);
        setBounds(pcNameField, 18, 6, 60, hfs, titleBarHeight);
        dialog.add(pcNameField);

        addLabel(dialog, "printer name", 2, 11, 14, hfs, titleBarHeight);
        setBounds(printerChoice, 18, 11, 60, hfs, titleBarHeight);
        dialog.add(printerChoice);

        addLabel(dialog, "url", 2, 16, 14, hfs, titleBarHeight);
        setBounds(urlField, 18, 16, 60, hfs, titleBarHeight);
        dialog.add(urlField);

        addLabel(dialog, "page width", 2, 21, 14, hfs, titleBarHeight);
        setBounds(pageWidthField, 18, 21, 16, hfs, titleBarHeight);
        dialog.add(pageWidthField);

        addLabel(dialog, "page height", 38, 21, 16, hfs, titleBarHeight);
        setBounds(pageHeightField, 56, 21, 16, hfs, titleBarHeight);
        dialog.add(pageHeightField);

        Label errorLabel = new Label("");
        errorLabel.setForeground(Color.RED);
        errorLabel.setBounds(2 * hfs, 26 * hfs + titleBarHeight, 76 * hfs, 4 * hfs);
        dialog.add(errorLabel);

        Button saveButton = new Button("Save");
        saveButton.setBounds(50 * hfs, 31 * hfs + titleBarHeight, 12 * hfs, 4 * hfs);
        dialog.add(saveButton);

        Button cancelButton = new Button("Cancel");
        cancelButton.setBounds(64 * hfs, 31 * hfs + titleBarHeight, 12 * hfs, 4 * hfs);
        dialog.add(cancelButton);

        cancelButton.addActionListener(e -> dialog.dispose());
        saveButton.addActionListener(e -> {
            errorLabel.setText("");

            String printerError = validatePrinter(printerChoice);
            if (printerError != null) {
                errorLabel.setText(printerError);
                return;
            }

            String widthError = validatePageNumber("page width", pageWidthField.getText());
            if (widthError != null) {
                errorLabel.setText(widthError);
                return;
            }

            String heightError = validatePageNumber("page height", pageHeightField.getText());
            if (heightError != null) {
                errorLabel.setText(heightError);
                return;
            }

            if (signinUri == null) {
                errorLabel.setText("Server signin URL is not configured.");
                return;
            }

            JsonObject body = new JsonObject();
            body.put("mac", mac);
            body.put("pcName", hostName);
            body.put("printerName", printerChoice.getSelectedItem());
            body.put("url", localUrl);
            body.put("pageWidth", new BigDecimal(pageWidthField.getText().trim()));
            body.put("pageHeight", new BigDecimal(pageHeightField.getText().trim()));

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(signinUri)
                    .POST(HttpRequest.BodyPublishers.ofString(body.encode()))
                    .header("Content-Type", "application/json");
            if (token != null && !token.isBlank()) {
                requestBuilder.header("Muppet-Token", token);
            }

            saveButton.setEnabled(false);
            new Thread(() -> {
                try {
                    HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        JsonObject responseJson = tryParseJsonObject(response.body());
                        if (responseJson != null && "SUCCESS".equals(responseJson.getString("code"))) {
                            EventQueue.invokeLater(dialog::dispose);
                            return;
                        }
                    }
                    EventQueue.invokeLater(() -> {
                        errorLabel.setText(parseFailureMessage(response.body()));
                        saveButton.setEnabled(true);
                    });
                } catch (Exception ignored) {
                    EventQueue.invokeLater(() -> {
                        errorLabel.setText("error");
                        saveButton.setEnabled(true);
                    });
                }
            }).start();
        });

        dialog.setVisible(true);
    }

    private static TextField createReadonlyField(String value) {
        TextField textField = new TextField(value == null ? "" : value);
        textField.setEditable(false);
        return textField;
    }

    private static String validatePrinter(Choice printerChoice) {
        if (printerChoice.getSelectedIndex() <= 0) {
            return "Please select printer name.";
        }
        return null;
    }

    private static String validatePageNumber(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            return "Please enter " + fieldName + ".";
        }
        String trimmed = value.trim();
        if (!trimmed.matches("^\\d+(\\.\\d{1,2})?$")) {
            return capitalize(fieldName) + " must be a valid number.";
        }
        try {
            BigDecimal decimal = new BigDecimal(trimmed);
            if (decimal.compareTo(BigDecimal.ZERO) <= 0 || decimal.compareTo(new BigDecimal("10000")) > 0) {
                return capitalize(fieldName) + " must be a valid number.";
            }
        } catch (Exception ignored) {
            return capitalize(fieldName) + " must be a valid number.";
        }
        return null;
    }

    private static String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "Value";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String parseFailureMessage(String body) {
        JsonObject jsonObject = tryParseJsonObject(body);
        if (jsonObject != null) {
            String message = jsonObject.getString("message");
            if (message != null && !message.isBlank()) {
                return message;
            }
        }
        return "error";
    }

    private static JsonObject tryParseJsonObject(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return new JsonObject(body);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int getHalfFontSize() {
        Font defaultFont = new Label("Loading...").getFont();
        int fontSize = defaultFont == null ? 12 : defaultFont.getSize();
        return fontSize / 2 + fontSize % 2;
    }

    private static void addLabel(Dialog dialog, String text, int x, int y, int width, int hfs, int titleBarHeight) {
        Label label = new Label(text);
        label.setBounds(x * hfs, y * hfs + titleBarHeight, width * hfs, 4 * hfs);
        dialog.add(label);
    }

    private static void setBounds(Component component, int x, int y, int width, int hfs, int titleBarHeight) {
        component.setBounds(x * hfs, y * hfs + titleBarHeight, width * hfs, 4 * hfs);
    }
}
