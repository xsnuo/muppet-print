package com.xuesinuo.muppet.ui;

import java.awt.Button;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@Component
public class SignedUi {

    private Dialog dialog;
    private volatile boolean opened;

    private Label macValueLabel;
    private Label pcNameValueLabel;
    private Label urlValueLabel;
    private Label errorLabel;
    private Panel tablePanel;

    private HttpClient currentHttpClient;
    private URI currentSignedUri;
    private String currentToken;
    private String currentLocalPcName;
    private String currentLocalUrl;
    private List<String> currentLocalPrinters = List.of();

    public void open(Frame owner, HttpClient httpClient, URI signedUri, String token, String localMac,
            String localPcName, String localUrl, List<String> localPrinters) {
        if (dialog == null) {
            initDialog(owner);
        }

        currentHttpClient = httpClient;
        currentSignedUri = signedUri;
        currentToken = token;
        currentLocalPcName = localPcName;
        currentLocalUrl = localUrl;
        currentLocalPrinters = localPrinters == null ? List.of() : localPrinters;

        macValueLabel.setText(localMac == null ? "" : localMac);
        pcNameValueLabel.setText(localPcName == null ? "" : localPcName);
        urlValueLabel.setText(localUrl == null ? "" : localUrl);
        tablePanel.removeAll();
        errorLabel.setText("Loading...");

        opened = true;
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);

        loadSignedRecords();
    }

    public boolean isOpened() {
        return opened;
    }

    private void initDialog(Frame owner) {
        dialog = new Dialog(owner, "Signed", false);
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
        int dialogWidth = Math.max(120 * hfs, 980);
        int dialogHeight = Math.max(54 * hfs, 560);
        dialog.setSize(dialogWidth, dialogHeight);

        addLabel(dialog, "mac", 2, 1, 10, hfs, titleBarHeight);
        macValueLabel = addValue(dialog, "", 14, 1, 102, hfs, titleBarHeight, Color.BLACK);

        addLabel(dialog, "pc name", 2, 6, 10, hfs, titleBarHeight);
        pcNameValueLabel = addValue(dialog, "", 14, 6, 102, hfs, titleBarHeight, Color.BLACK);

        addLabel(dialog, "url", 2, 11, 10, hfs, titleBarHeight);
        urlValueLabel = addValue(dialog, "", 14, 11, 102, hfs, titleBarHeight, Color.BLACK);

        ScrollPane tableScrollPane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
        tableScrollPane.setBounds(2 * hfs, 16 * hfs + titleBarHeight, 116 * hfs, 25 * hfs);
        tablePanel = new Panel(null);
        tableScrollPane.add(tablePanel);
        dialog.add(tableScrollPane);

        errorLabel = new Label("Loading...");
        errorLabel.setForeground(Color.RED);
        errorLabel.setBounds(2 * hfs, 43 * hfs + titleBarHeight, 92 * hfs, 4 * hfs);
        dialog.add(errorLabel);

        Button closeButton = new Button("Close");
        closeButton.setBounds(104 * hfs, 43 * hfs + titleBarHeight, 12 * hfs, 4 * hfs);
        closeButton.addActionListener(e -> closeDialog());
        dialog.add(closeButton);
    }

    private void loadSignedRecords() {
        if (currentSignedUri == null) {
            EventQueue.invokeLater(() -> errorLabel.setText("Server signed URL is not configured."));
            return;
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(currentSignedUri).GET();
        if (currentToken != null && !currentToken.isBlank()) {
            requestBuilder.header("Muppet-Token", currentToken);
        }

        new Thread(() -> {
            try {
                HttpResponse<String> response = currentHttpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    EventQueue.invokeLater(() -> errorLabel.setText(parseFailureMessage(response.body())));
                    return;
                }
                List<SignedPrintRecord> records = parseSignedRecords(response.body());
                EventQueue.invokeLater(() -> {
                    errorLabel.setText("");
                    renderSignedTable(records);
                });
            } catch (Exception ignored) {
                EventQueue.invokeLater(() -> errorLabel.setText("error"));
            }
        }).start();
    }

    private void renderSignedTable(List<SignedPrintRecord> records) {
        int hfs = getHalfFontSize();
        tablePanel.removeAll();

        int tableWidth = 112 * hfs;
        int rowHeight = 5 * hfs;
        int totalRows = Math.max(records.size() + 1, 3);
        tablePanel.setPreferredSize(new Dimension(tableWidth, totalRows * rowHeight + 2 * hfs));

        addTableCell(tablePanel, "pc name", 2, 1, 16, hfs, Color.BLACK);
        addTableCell(tablePanel, "printer name", 19, 1, 20, hfs, Color.BLACK);
        addTableCell(tablePanel, "url", 41, 1, 30, hfs, Color.BLACK);
        addTableCell(tablePanel, "page width", 73, 1, 11, hfs, Color.BLACK);
        addTableCell(tablePanel, "page height", 86, 1, 11, hfs, Color.BLACK);
        addTableCell(tablePanel, "operation", 99, 1, 11, hfs, Color.BLACK);

        if (records.isEmpty()) {
            addTableCell(tablePanel, "No signed printers.", 2, 6, 60, hfs, Color.BLACK);
        }

        for (int i = 0; i < records.size(); i++) {
            SignedPrintRecord record = records.get(i);
            int rowY = 6 + i * 5;

            Color pcColor = Objects.equals(currentLocalPcName, record.pcName) ? Color.BLACK : Color.RED;
            Color printerColor = currentLocalPrinters.contains(record.printerName) ? Color.BLACK : Color.RED;
            Color urlColor = Objects.equals(currentLocalUrl, record.url) ? Color.BLACK : Color.RED;

            addTableCell(tablePanel, record.pcName, 2, rowY, 16, hfs, pcColor);
            addTableCell(tablePanel, record.printerName, 19, rowY, 20, hfs, printerColor);
            addTableCell(tablePanel, record.url, 41, rowY, 30, hfs, urlColor);
            addTableCell(tablePanel, record.pageWidth, 73, rowY, 11, hfs, Color.BLACK);
            addTableCell(tablePanel, record.pageHeight, 86, rowY, 11, hfs, Color.BLACK);

            Button deleteButton = new Button("delete");
            deleteButton.setEnabled(false);
            deleteButton.setBounds(99 * hfs, rowY * hfs, 11 * hfs, 4 * hfs);
            tablePanel.add(deleteButton);
        }

        tablePanel.validate();
        tablePanel.repaint();
    }

    private void closeDialog() {
        opened = false;
        if (dialog != null) {
            dialog.setVisible(false);
        }
    }

    private void addTableCell(Panel panel, String text, int x, int y, int width, int hfs, Color color) {
        Label label = new Label(text == null ? "" : text);
        label.setForeground(color);
        label.setBounds(x * hfs, y * hfs, width * hfs, 4 * hfs);
        panel.add(label);
    }

    private List<SignedPrintRecord> parseSignedRecords(String body) {
        List<SignedPrintRecord> records = new ArrayList<>();
        JsonObject jsonObject = tryParseJsonObject(body);
        if (jsonObject == null) {
            return records;
        }

        JsonArray printsArray = null;
        if (jsonObject.containsKey("code")) {
            if (!"SUCCESS".equals(jsonObject.getString("code"))) {
                return records;
            }
            JsonObject data = jsonObject.getJsonObject("data");
            if (data != null) {
                printsArray = data.getJsonArray("prints");
            }
        }
        if (printsArray == null) {
            printsArray = jsonObject.getJsonArray("prints");
        }
        if (printsArray == null) {
            return records;
        }

        for (int i = 0; i < printsArray.size(); i++) {
            JsonObject item = printsArray.getJsonObject(i);
            if (item == null) {
                continue;
            }
            SignedPrintRecord record = new SignedPrintRecord();
            record.pcName = item.getString("pcName", "");
            record.printerName = item.getString("printerName", "");
            record.url = item.getString("url", "");
            record.pageWidth = item.getValue("pageWidth") == null ? "" : String.valueOf(item.getValue("pageWidth"));
            record.pageHeight = item.getValue("pageHeight") == null ? "" : String.valueOf(item.getValue("pageHeight"));
            records.add(record);
        }
        return records;
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

    private void addLabel(Dialog target, String text, int x, int y, int width, int hfs, int titleBarHeight) {
        Label label = new Label(text);
        label.setBounds(x * hfs, y * hfs + titleBarHeight, width * hfs, 4 * hfs);
        target.add(label);
    }

    private Label addValue(Dialog target, String text, int x, int y, int width, int hfs, int titleBarHeight,
            Color color) {
        Label label = new Label(text == null ? "" : text);
        label.setForeground(color);
        label.setBounds(x * hfs, y * hfs + titleBarHeight, width * hfs, 4 * hfs);
        target.add(label);
        return label;
    }

    private int getHalfFontSize() {
        Font defaultFont = new Label("Loading...").getFont();
        int fontSize = defaultFont == null ? 12 : defaultFont.getSize();
        return fontSize / 2 + fontSize % 2;
    }

    private static class SignedPrintRecord {
        private String pcName;
        private String printerName;
        private String url;
        private String pageWidth;
        private String pageHeight;
    }
}
