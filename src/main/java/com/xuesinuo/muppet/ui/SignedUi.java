package com.xuesinuo.muppet.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.springframework.stereotype.Component;

import com.xuesinuo.muppet.webclient.GroupOption;
import com.xuesinuo.muppet.webclient.GroupWebClient;
import com.xuesinuo.muppet.webclient.SignedPrinterRecord;
import com.xuesinuo.muppet.webclient.SignedWebClient;
import com.xuesinuo.muppet.webclient.SignoutPrinterRequest;
import com.xuesinuo.muppet.webclient.SignoutWebClient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SignedUi {

    private final SignedWebClient signedWebClient;
    private final GroupWebClient groupWebClient;
    private final SignoutWebClient signoutWebClient;

    private JDialog dialog;
    private volatile boolean opened;

    private JLabel macValueLabel;
    private JLabel pcNameValueLabel;
    private JLabel urlValueLabel;
    private JLabel errorLabel;
    private JPanel tablePanel;

    private String currentLocalMac;
    private String currentLocalPcName;
    private String currentLocalUrl;
    private List<String> currentLocalPrinters = List.of();
    private Map<String, String> groupLabelByValue = new HashMap<>();

    public void open(Frame owner, String localMac,
            String localPcName, String localUrl, List<String> localPrinters) {
        if (dialog == null) {
            initDialog(owner);
        }

        currentLocalMac = localMac;
        currentLocalPcName = localPcName;
        currentLocalUrl = localUrl;
        currentLocalPrinters = localPrinters == null ? List.of() : localPrinters;
        groupLabelByValue = new HashMap<>();

        macValueLabel.setText(localMac == null ? "" : localMac);
        pcNameValueLabel.setText(localPcName == null ? "" : localPcName);
        urlValueLabel.setText(localUrl == null ? "" : localUrl);
        tablePanel.removeAll();
        errorLabel.setText("Loading...");

        opened = true;
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);

        loadGroupsAndSignedRecords();
    }

    public boolean isOpened() {
        return opened;
    }

    private void initDialog(Frame owner) {
        dialog = new JDialog(owner, "Signed", false);
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
        int titleBarHeight = 0;
        int hfs = getHalfFontSize();
        int dialogWidth = Math.max(182 * hfs, 1240);
        int dialogHeight = Math.max(66 * hfs, 540);
        dialog.setSize(dialogWidth, dialogHeight);

        addLabel(dialog, "mac", 2, 1, 10, hfs, titleBarHeight);
        macValueLabel = addValue(dialog, "", 14, 1, 102, hfs, titleBarHeight, Color.BLACK);

        addLabel(dialog, "pc name", 2, 6, 10, hfs, titleBarHeight);
        pcNameValueLabel = addValue(dialog, "", 14, 6, 102, hfs, titleBarHeight, Color.BLACK);

        addLabel(dialog, "url", 2, 11, 10, hfs, titleBarHeight);
        urlValueLabel = addValue(dialog, "", 14, 11, 164, hfs, titleBarHeight, Color.BLACK);

        JScrollPane tableScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tableScrollPane.setBounds(2 * hfs, 16 * hfs + titleBarHeight, 177 * hfs, 42 * hfs);
        tablePanel = new JPanel(null);
        tableScrollPane.setViewportView(tablePanel);
        dialog.add(tableScrollPane);

        errorLabel = new JLabel("Loading...");
        errorLabel.setForeground(Color.RED);
        errorLabel.setBounds(2 * hfs, 60 * hfs + titleBarHeight, 120 * hfs, 4 * hfs);
        dialog.add(errorLabel);

        JButton closeButton = new JButton("Close");
        closeButton.setBounds(167 * hfs, 60 * hfs + titleBarHeight, 12 * hfs, 4 * hfs);
        closeButton.addActionListener(e -> closeDialog());
        dialog.add(closeButton);

    }

    private void loadGroupsAndSignedRecords() {
        groupWebClient.queryGroups().onSuccess(result -> {
            if (result.isSuccess() && result.getData() != null) {
                Map<String, String> groupMap = new HashMap<>();
                for (GroupOption groupOption : result.getData()) {
                    String value = groupOption.getValue();
                    String label = groupOption.getLabel();
                    if (value != null && !value.isBlank() && label != null && !label.isBlank()) {
                        groupMap.put(value, label);
                    }
                }
                groupLabelByValue = groupMap;
            }
            loadSignedRecords();
        });
    }

    private void loadSignedRecords() {
        signedWebClient.querySigned(currentLocalMac)
                .onSuccess(response -> {
                    if (!response.isSuccess()) {
                        EventQueue.invokeLater(() -> errorLabel.setText(parseErrorMessage(response.getMessage())));
                        return;
                    }
                    List<SignedPrinterRecord> records = response.getData() == null ? List.of() : response.getData();
                    EventQueue.invokeLater(() -> {
                        errorLabel.setText("");
                        renderSignedTable(records);
                    });
                });
    }

    private void renderSignedTable(List<SignedPrinterRecord> records) {
        int hfs = getHalfFontSize();
        tablePanel.removeAll();

        int tableWidth = 176 * hfs;
        int rowHeight = 5 * hfs;
        int totalRows = Math.max(records.size() + 1, 3);
        int tableHeight = totalRows * rowHeight + 2 * hfs;
        tablePanel.setPreferredSize(new Dimension(tableWidth, tableHeight));
        tablePanel.setSize(tableWidth, tableHeight);

        addTableCell(tablePanel, "pc name", 2, 1, 28, hfs, Color.BLACK);
        addTableCell(tablePanel, "printer name", 31, 1, 28, hfs, Color.BLACK);
        addTableCell(tablePanel, "group", 60, 1, 24, hfs, Color.BLACK);
        addTableCell(tablePanel, "url", 85, 1, 40, hfs, Color.BLACK);
        addTableCell(tablePanel, "width", 126, 1, 8, hfs, Color.BLACK);
        addTableCell(tablePanel, "height", 135, 1, 8, hfs, Color.BLACK);
        addTableCell(tablePanel, "operation", 144, 1, 20, hfs, Color.BLACK);

        if (records.isEmpty()) {
            addTableCell(tablePanel, "No signed printers.", 2, 6, 60, hfs, Color.BLACK);
        }

        for (int i = 0; i < records.size(); i++) {
            SignedPrinterRecord record = records.get(i);
            int rowY = 6 + i * 5;

            Color pcColor = Objects.equals(currentLocalPcName, record.getPcName()) ? Color.BLACK : Color.RED;
            Color printerColor = currentLocalPrinters.contains(record.getPrinterName()) ? Color.BLACK : Color.RED;
            Color urlColor = Objects.equals(currentLocalUrl, record.getUrl()) ? Color.BLACK : Color.RED;
            String groupText = resolveGroupLabel(record.getGroup());

            addTableCell(tablePanel, record.getPcName(), 2, rowY, 28, hfs, pcColor);
            addTableCell(tablePanel, record.getPrinterName(), 31, rowY, 28, hfs, printerColor);
            addTableCell(tablePanel, groupText, 60, rowY, 24, hfs, Color.BLACK);
            addTableCell(tablePanel, record.getUrl(), 85, rowY, 40, hfs, urlColor);
            addTableCell(tablePanel, record.getPageWidth(), 126, rowY, 8, hfs, Color.BLACK);
            addTableCell(tablePanel, record.getPageHeight(), 135, rowY, 8, hfs, Color.BLACK);

            JButton deleteButton = new JButton("delete");
            deleteButton.setEnabled(true);
            deleteButton.setBounds(144 * hfs, rowY * hfs, 20 * hfs, 4 * hfs);
            deleteButton.addActionListener(e -> handleDelete(record));
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

    private void addTableCell(JPanel panel, String text, int x, int y, int width, int hfs, Color color) {
        JLabel label = new JLabel(text == null ? "" : text);
        label.setForeground(color);
        label.setBounds(x * hfs, y * hfs, width * hfs, 4 * hfs);
        panel.add(label);
    }

    private String parseErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "error";
        }
        return message;
    }

    private String resolveGroupLabel(String groupValue) {
        if (groupValue == null || groupValue.isBlank()) {
            return "";
        }
        String groupLabel = groupLabelByValue.get(groupValue);
        return groupLabel == null || groupLabel.isBlank() ? groupValue : groupLabel;
    }

    private void handleDelete(SignedPrinterRecord record) {
        if (record == null) {
            return;
        }
        SignoutPrinterRequest requestBody = new SignoutPrinterRequest();
        requestBody.setMac(record.getMac());
        requestBody.setPrinterName(record.getPrinterName());
        requestBody.setGroup(record.getGroup());

        errorLabel.setText("Deleting...");
        signoutWebClient.signout(requestBody).onSuccess(result -> EventQueue.invokeLater(() -> {
            if (result.isSuccess()) {
                errorLabel.setText("Delete success.");
                loadSignedRecords();
                return;
            }
            errorLabel.setText(parseErrorMessage(result.getMessage()));
        }));
    }

    private void addLabel(JDialog target, String text, int x, int y, int width, int hfs, int titleBarHeight) {
        JLabel label = new JLabel(text);
        label.setBounds(x * hfs, y * hfs + titleBarHeight, width * hfs, 4 * hfs);
        target.add(label);
    }

    private JLabel addValue(JDialog target, String text, int x, int y, int width, int hfs, int titleBarHeight,
            Color color) {
        JLabel label = new JLabel(text == null ? "" : text);
        label.setForeground(color);
        label.setBounds(x * hfs, y * hfs + titleBarHeight, width * hfs, 4 * hfs);
        target.add(label);
        return label;
    }

    private int getHalfFontSize() {
        Font defaultFont = new JLabel("Loading...").getFont();
        int fontSize = defaultFont == null ? 12 : defaultFont.getSize();
        return fontSize / 2 + fontSize % 2;
    }

}
