package com.xuesinuo.muppet.ui;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
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

import org.springframework.stereotype.Component;

import io.vertx.core.json.JsonObject;

@Component
public class SigninLocalPrinterUi {

    private Dialog dialog;
    private volatile boolean opened;

    private TextField macField;
    private TextField pcNameField;
    private TextField urlField;
    private TextField pageWidthField;
    private TextField pageHeightField;
    private Choice printerChoice;
    private Label errorLabel;
    private Button saveButton;

    private String currentMac;
    private String currentHostName;
    private String currentLocalUrl;
    private URI currentSigninUri;
    private String currentToken;
    private HttpClient currentHttpClient;

    public void open(Frame owner, HttpClient httpClient, URI signinUri, String token, String mac,
            String hostName, String localUrl, List<String> printers) {
        if (dialog == null) {
            initDialog(owner);
        }

        currentHttpClient = httpClient;
        currentSigninUri = signinUri;
        currentToken = token;
        currentMac = mac;
        currentHostName = hostName;
        currentLocalUrl = localUrl;

        macField.setText(mac == null ? "" : mac);
        pcNameField.setText(hostName == null ? "" : hostName);
        urlField.setText(localUrl == null ? "" : localUrl);
        pageWidthField.setText("");
        pageHeightField.setText("");
        errorLabel.setText("");

        printerChoice.removeAll();
        printerChoice.add("Please select printer name");
        if (printers != null) {
            for (String printerName : printers) {
                if (printerName != null && !printerName.isBlank()) {
                    printerChoice.add(printerName);
                }
            }
        }
        printerChoice.select(0);

        saveButton.setEnabled(true);
        opened = true;
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    public boolean isOpened() {
        return opened;
    }

    private void initDialog(Frame owner) {
        dialog = new Dialog(owner, "Signin local printer", true);
        if (owner.getIconImage() != null) {
            dialog.setIconImage(owner.getIconImage());
        }
        dialog.setResizable(false);
        dialog.setLayout(null);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeDialog();
            }
        });
        dialog.addNotify();

        Insets insets = dialog.getInsets();
        int titleBarHeight = insets.top;
        int hfs = getHalfFontSize();
        int dialogWidth = Math.max(82 * hfs, 700);
        int dialogHeight = Math.max(43 * hfs, 420);
        dialog.setSize(dialogWidth, dialogHeight);

        macField = createReadonlyField("");
        pcNameField = createReadonlyField("");
        urlField = createReadonlyField("");
        pageWidthField = new TextField();
        pageHeightField = new TextField();
        printerChoice = new Choice();

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

        errorLabel = new Label("");
        errorLabel.setForeground(Color.RED);
        errorLabel.setBounds(2 * hfs, 26 * hfs + titleBarHeight, 76 * hfs, 4 * hfs);
        dialog.add(errorLabel);

        saveButton = new Button("Save");
        saveButton.setBounds(50 * hfs, 31 * hfs + titleBarHeight, 12 * hfs, 4 * hfs);
        saveButton.addActionListener(e -> handleSave());
        dialog.add(saveButton);

        Button cancelButton = new Button("Cancel");
        cancelButton.setBounds(64 * hfs, 31 * hfs + titleBarHeight, 12 * hfs, 4 * hfs);
        cancelButton.addActionListener(e -> closeDialog());
        dialog.add(cancelButton);
    }

    private void handleSave() {
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

        if (currentSigninUri == null) {
            errorLabel.setText("Server signin URL is not configured.");
            return;
        }

        JsonObject body = new JsonObject();
        body.put("mac", currentMac);
        body.put("pcName", currentHostName);
        body.put("printerName", printerChoice.getSelectedItem());
        body.put("url", currentLocalUrl);
        body.put("pageWidth", new BigDecimal(pageWidthField.getText().trim()));
        body.put("pageHeight", new BigDecimal(pageHeightField.getText().trim()));

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(currentSigninUri)
                .POST(HttpRequest.BodyPublishers.ofString(body.encode()))
                .header("Content-Type", "application/json");
        if (currentToken != null && !currentToken.isBlank()) {
            requestBuilder.header("Muppet-Token", currentToken);
        }

        saveButton.setEnabled(false);
        new Thread(() -> {
            try {
                HttpResponse<String> response = currentHttpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonObject responseJson = tryParseJsonObject(response.body());
                    if (responseJson != null && "SUCCESS".equals(responseJson.getString("code"))) {
                        EventQueue.invokeLater(this::closeDialog);
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
    }

    private void closeDialog() {
        opened = false;
        if (dialog != null) {
            dialog.setVisible(false);
        }
    }

    private TextField createReadonlyField(String value) {
        TextField textField = new TextField(value == null ? "" : value);
        textField.setEditable(false);
        return textField;
    }

    private String validatePrinter(Choice choice) {
        if (choice.getSelectedIndex() <= 0) {
            return "Please select printer name.";
        }
        return null;
    }

    private String validatePageNumber(String fieldName, String value) {
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

    private String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "Value";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private String parseFailureMessage(String body) {
        JsonObject jsonObject = tryParseJsonObject(body);
        if (jsonObject != null) {
            String message = jsonObject.getString("message");
            if (message != null && !message.isBlank()) {
                return message;
            }
        }
        return "error";
    }

    private JsonObject tryParseJsonObject(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return new JsonObject(body);
        } catch (Exception ignored) {
            return null;
        }
    }

    private int getHalfFontSize() {
        Font defaultFont = new Label("Loading...").getFont();
        int fontSize = defaultFont == null ? 12 : defaultFont.getSize();
        return fontSize / 2 + fontSize % 2;
    }

    private void addLabel(Dialog target, String text, int x, int y, int width, int hfs, int titleBarHeight) {
        Label label = new Label(text);
        label.setBounds(x * hfs, y * hfs + titleBarHeight, width * hfs, 4 * hfs);
        target.add(label);
    }

    private void setBounds(java.awt.Component component, int x, int y, int width, int hfs, int titleBarHeight) {
        component.setBounds(x * hfs, y * hfs + titleBarHeight, width * hfs, 4 * hfs);
    }
}
