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
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.xuesinuo.muppet.webclient.GroupOption;
import com.xuesinuo.muppet.webclient.GroupWebClient;
import com.xuesinuo.muppet.webclient.SigninPrinterRequest;
import com.xuesinuo.muppet.webclient.SigninWebClient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SigninLocalPrinterUi {

    private final SigninWebClient signinWebClient;
    private final GroupWebClient groupWebClient;

    private Dialog dialog;
    private volatile boolean opened;

    private Choice groupChoice;
    private TextField macField;
    private TextField pcNameField;
    private TextField urlField;
    private TextField pageWidthField;
    private TextField pageHeightField;
    private Choice printerChoice;
    private Button urlModeButton;
    private Label errorLabel;
    private Button saveButton;

    private String currentMac;
    private String currentHostName;
    private String currentLocalUrl;
    private String currentIpUrl;
    private boolean useIpUrl;
    private List<GroupOption> currentGroupOptions = List.of();

    public void open(Frame owner, String mac,
            String hostName, String localUrl, List<String> printers) {
        if (dialog == null) {
            initDialog(owner);
        }

        currentMac = mac;
        currentHostName = hostName;
        currentLocalUrl = localUrl;
        currentIpUrl = buildIpUrl(localUrl);
        useIpUrl = false;
        currentGroupOptions = new ArrayList<>();

        macField.setText(mac == null ? "" : mac);
        pcNameField.setText(hostName == null ? "" : hostName);
        updateUrlModeUi();
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

        groupChoice.removeAll();
        groupChoice.add("Loading groups...");
        groupChoice.select(0);

        saveButton.setEnabled(false);
        loadGroups();
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
        int dialogWidth = Math.max(82 * hfs, 560);
        int dialogHeight = Math.max(44 * hfs, 350);
        dialog.setSize(dialogWidth, dialogHeight);

        groupChoice = new Choice();
        macField = createReadonlyField("");
        pcNameField = createReadonlyField("");
        urlField = new TextField();
        pageWidthField = new TextField();
        pageHeightField = new TextField();
        printerChoice = new Choice();

        addLabel(dialog, "group", 2, 1, 14, hfs, titleBarHeight);
        setBounds(groupChoice, 18, 1, 60, hfs, titleBarHeight);
        dialog.add(groupChoice);

        addLabel(dialog, "mac", 2, 6, 14, hfs, titleBarHeight);
        setBounds(macField, 18, 6, 60, hfs, titleBarHeight);
        dialog.add(macField);

        addLabel(dialog, "pc name", 2, 11, 14, hfs, titleBarHeight);
        setBounds(pcNameField, 18, 11, 60, hfs, titleBarHeight);
        dialog.add(pcNameField);

        addLabel(dialog, "printer name", 2, 16, 14, hfs, titleBarHeight);
        setBounds(printerChoice, 18, 16, 60, hfs, titleBarHeight);
        dialog.add(printerChoice);

        addLabel(dialog, "url", 2, 21, 14, hfs, titleBarHeight);
        setBounds(urlField, 18, 21, 44, hfs, titleBarHeight);
        dialog.add(urlField);

        urlModeButton = new Button("use IP");
        urlModeButton.setBounds(64 * hfs, 21 * hfs + titleBarHeight, 14 * hfs, 4 * hfs);
        urlModeButton.addActionListener(e -> toggleUrlMode());
        dialog.add(urlModeButton);

        addLabel(dialog, "page width", 2, 26, 14, hfs, titleBarHeight);
        setBounds(pageWidthField, 18, 26, 16, hfs, titleBarHeight);
        dialog.add(pageWidthField);

        addLabel(dialog, "page height", 38, 26, 16, hfs, titleBarHeight);
        setBounds(pageHeightField, 56, 26, 16, hfs, titleBarHeight);
        dialog.add(pageHeightField);

        errorLabel = new Label("");
        errorLabel.setForeground(Color.RED);
        errorLabel.setBounds(2 * hfs, 31 * hfs + titleBarHeight, 76 * hfs, 4 * hfs);
        dialog.add(errorLabel);

        saveButton = new Button("Save");
        saveButton.setBounds(50 * hfs, 36 * hfs + titleBarHeight, 12 * hfs, 4 * hfs);
        saveButton.addActionListener(e -> handleSave());
        dialog.add(saveButton);

        Button cancelButton = new Button("Cancel");
        cancelButton.setBounds(64 * hfs, 36 * hfs + titleBarHeight, 12 * hfs, 4 * hfs);
        cancelButton.addActionListener(e -> closeDialog());
        dialog.add(cancelButton);

        AwtUiSupport.applyDefaultFont(dialog);
    }

    private void loadGroups() {
        groupWebClient.queryGroups()
                .onSuccess(result -> EventQueue.invokeLater(() -> {
                    if (!result.isSuccess() || result.getData() == null || result.getData().isEmpty()) {
                        errorLabel.setText("Load groups failed.");
                        saveButton.setEnabled(false);
                        groupChoice.removeAll();
                        groupChoice.add("Load groups failed");
                        groupChoice.select(0);
                        return;
                    }
                    currentGroupOptions = result.getData();
                    groupChoice.removeAll();
                    groupChoice.add("Please select group");
                    for (GroupOption groupOption : currentGroupOptions) {
                        if (groupOption.getLabel() != null && !groupOption.getLabel().isBlank()) {
                            groupChoice.add(groupOption.getLabel());
                        }
                    }
                    if (groupChoice.getItemCount() <= 1) {
                        errorLabel.setText("Load groups failed.");
                        saveButton.setEnabled(false);
                        groupChoice.removeAll();
                        groupChoice.add("Load groups failed");
                        groupChoice.select(0);
                        return;
                    }
                    groupChoice.select(0);
                    saveButton.setEnabled(true);
                    errorLabel.setText("");
                }));
    }

    private void handleSave() {
        errorLabel.setText("");

        String printerError = validatePrinter(printerChoice);
        if (printerError != null) {
            errorLabel.setText(printerError);
            return;
        }

        String groupError = validateGroup(groupChoice);
        if (groupError != null) {
            errorLabel.setText(groupError);
            return;
        }

        String urlError = validateUrl(urlField.getText());
        if (urlError != null) {
            errorLabel.setText(urlError);
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

        SigninPrinterRequest requestBody = new SigninPrinterRequest();
        requestBody.setGroup(resolveSelectedGroupValue());
        requestBody.setMac(currentMac);
        requestBody.setPcName(currentHostName);
        requestBody.setPrinterName(printerChoice.getSelectedItem());
        requestBody.setUrl(urlField.getText() == null ? "" : urlField.getText().trim());
        requestBody.setPageWidth(new BigDecimal(pageWidthField.getText().trim()));
        requestBody.setPageHeight(new BigDecimal(pageHeightField.getText().trim()));

        saveButton.setEnabled(false);
        signinWebClient.signin(requestBody)
                .onSuccess(response -> {
                    if (response.isSuccess()) {
                        EventQueue.invokeLater(this::closeDialog);
                        return;
                    }
                    EventQueue.invokeLater(() -> {
                        errorLabel.setText(parseErrorMessage(response.getMessage()));
                        saveButton.setEnabled(true);
                    });
                });
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

    private String validateGroup(Choice choice) {
        if (choice.getSelectedIndex() <= 0) {
            return "Please select group.";
        }
        String value = resolveSelectedGroupValue();
        if (value == null || value.isBlank()) {
            return "Please select group.";
        }
        return null;
    }

    private String resolveSelectedGroupValue() {
        int selectedIndex = groupChoice.getSelectedIndex();
        if (selectedIndex <= 0) {
            return "";
        }
        int optionIndex = selectedIndex - 1;
        if (optionIndex < 0 || optionIndex >= currentGroupOptions.size()) {
            return "";
        }
        String value = currentGroupOptions.get(optionIndex).getValue();
        return value == null ? "" : value;
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

    private String parseErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "error";
        }
        return message;
    }

    private void toggleUrlMode() {
        useIpUrl = !useIpUrl;
        updateUrlModeUi();
    }

    private void updateUrlModeUi() {
        String text = useIpUrl ? currentIpUrl : currentLocalUrl;
        urlField.setText(text == null ? "" : text);
        if (urlModeButton != null) {
            urlModeButton.setLabel(useIpUrl ? "use PC" : "use IP");
        }
    }

    private String validateUrl(String value) {
        if (value == null || value.isBlank()) {
            return "Please enter url.";
        }
        String trimmed = value.trim();
        String prefix;
        if (trimmed.startsWith("http://")) {
            prefix = "http://";
        } else if (trimmed.startsWith("https://")) {
            prefix = "https://";
        } else {
            return "Url must start with http:// or https://.";
        }
        String rest = trimmed.substring(prefix.length());
        if (rest.isBlank()) {
            return "Url must include non-blank content after protocol.";
        }
        return null;
    }

    private String buildIpUrl(String sourceUrl) {
        return AwtUiSupport.buildIpUrl(sourceUrl, extractPort(sourceUrl));
    }

    private String extractPort(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return "";
        }
        int schemeIndex = sourceUrl.indexOf("://");
        String hostAndPath = schemeIndex >= 0 ? sourceUrl.substring(schemeIndex + 3) : sourceUrl;
        int slashIndex = hostAndPath.indexOf('/');
        String hostPort = slashIndex >= 0 ? hostAndPath.substring(0, slashIndex) : hostAndPath;
        int colonIndex = hostPort.lastIndexOf(':');
        if (colonIndex < 0 || colonIndex == hostPort.length() - 1) {
            return "";
        }
        String port = hostPort.substring(colonIndex + 1).trim();
        return port.matches("\\d+") ? port : "";
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
