package com.xuesinuo.muppet.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.BindException;
import java.net.NetworkInterface;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.print.PrintService;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import com.microsoft.playwright.Playwright;
import com.xuesinuo.muppet.tool.PrinterUtil;
import com.xuesinuo.muppet.vertx.WebVerticle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MuppetPrinterUi {

    private static final String SINGLE_INSTANCE_LOCK_DIR = ".muppet-print";
    private static final String SINGLE_INSTANCE_LOCK_FILE = "instance.lock";
    private static final long WEB_STARTUP_TIMEOUT_MS = 8000;
    private static final String DEFAULT_PORT = "58080";

    private final SigninLocalPrinterUi signinLocalPrinterUi;
    private final SignedUi signedUi;
    private final TrayUi trayUi;
    private final WebVerticle webVerticle;
    private final UiMessageService uiMessageService;
    private final ConfigurableApplicationContext applicationContext;

    @Value("${release.signin.enable:false}")
    private boolean signinEnable;

    private final JFrame frame = new JFrame();
    private final JLabel portTitelLabel = new JLabel("端口 Web Port:");
    private final JLabel portLabel = new JLabel("");
    private final JTextField portTextField = new JTextField(4);
    private final JLabel statusLabel = new JLabel("Status: Stopped");
    private final JButton runButton = new JButton("Run");
    private final JButton stopButton = new JButton("Stop");
    private final JButton signinLocalPrinterButton = new JButton("Signin local printer");
    private final JButton signedButton = new JButton("Signed");
    private final JLabel messageLabel = new JLabel("");
    private final JCheckBox autoStartCheckbox = new JCheckBox("auto start on boot");

    private volatile int appState = 0; // 0:停止 1:正在启动 2:运行 3:正在关闭

    private FileChannel singleInstanceLockChannel;
    private FileLock singleInstanceFileLock;
    private Image appIcon;

    @PostConstruct
    public void init() {
        if (!acquireSingleInstanceLock()) {
            showAlreadyRunningDialog();
            exitCurrentProcess();
            return;
        }

        initMainFrame();
        uiMessageService.bindRenderer(message -> EventQueue.invokeLater(() -> messageLabel.setText(message)));
        trayUi.init(frame, getAppIcon(), this::showMainFrame, this::hideMainFrame, this::shutdownApplication);
        checkPlaywrightAndAutoRun();
    }

    @PreDestroy
    public void destroy() {
        try {
            webVerticle.stop(3000);
        } catch (Exception ignored) {
        }
        trayUi.remove();
        releaseSingleInstanceLock();
        EventQueue.invokeLater(() -> {
            if (frame.isDisplayable()) {
                frame.dispose();
            }
        });
    }

    private void initMainFrame() {
        frame.setTitle("Muppet Printer - 中文测试");
        Image icon = getAppIcon();
        if (icon != null) {
            frame.setIconImage(icon);
        }
        frame.setResizable(false);
        frame.setLayout(null);
        frame.setSize(0, 0);
        frame.setVisible(true);

        Insets insets = frame.getInsets();
        int titleBarHeight = insets.top;
        int hfs = getHalfFontSize();

        frame.setVisible(false);
        frame.setSize(80 * hfs, 50 * hfs);

        portTitelLabel.setBounds(2 * hfs, 1 * hfs + titleBarHeight, 12 * hfs, 4 * hfs);
        frame.add(portTitelLabel);

        portLabel.setBounds(14 * hfs, 1 * hfs + titleBarHeight, 14 * hfs, 4 * hfs);
        portLabel.setVisible(false);
        frame.add(portLabel);

        portTextField.setBounds(14 * hfs, 1 * hfs + titleBarHeight, 14 * hfs, 4 * hfs);
        portTextField.setText(DEFAULT_PORT);
        frame.add(portTextField);

        statusLabel.setBounds(2 * hfs, 6 * hfs + titleBarHeight, 48 * hfs, 4 * hfs);
        frame.add(statusLabel);

        runButton.setBounds(2 * hfs, 11 * hfs + titleBarHeight, 20 * hfs, 4 * hfs);
        runButton.addActionListener(e -> startWebService());
        frame.add(runButton);

        stopButton.setBounds(24 * hfs, 11 * hfs + titleBarHeight, 20 * hfs, 4 * hfs);
        stopButton.addActionListener(e -> stopWebService());
        frame.add(stopButton);

        if (signinEnable) {
            signinLocalPrinterButton.setBounds(2 * hfs, 16 * hfs + titleBarHeight, 30 * hfs, 4 * hfs);
            signinLocalPrinterButton.addActionListener(e -> openSigninLocalPrinterDialog());
            frame.add(signinLocalPrinterButton);

            signedButton.setBounds(34 * hfs, 16 * hfs + titleBarHeight, 18 * hfs, 4 * hfs);
            signedButton.addActionListener(e -> openSignedDialog());
            frame.add(signedButton);
        }

        autoStartCheckbox.setBounds(2 * hfs, 21 * hfs + titleBarHeight, 28 * hfs, 4 * hfs);
        autoStartCheckbox.setSelected(isAutoStartEnabled());
        autoStartCheckbox.addItemListener(e -> {
            if (autoStartCheckbox.isSelected()) {
                enableAutoStart();
            } else {
                disableAutoStart();
            }
        });
        frame.add(autoStartCheckbox);

        messageLabel.setBounds(2 * hfs, 26 * hfs + titleBarHeight, 76 * hfs, 4 * hfs);
        messageLabel.setForeground(Color.RED);
        frame.add(messageLabel);

        frame.setVisible(true);
    }

    private void checkPlaywrightAndAutoRun() {
        uiMessageService.showMessage("Loading update. Please wait...");
        new Thread(() -> {
            try (Playwright playwright = Playwright.create()) {
                uiMessageService.showMessage("Update completed.");
                startWebService();
                uiMessageService.restoreDefaultMessage();
            } catch (Exception exception) {
                exception.printStackTrace();
                uiMessageService.showMessage("Setup failed!" + exception.getMessage());
            }
        }).start();
    }

    private void startWebService() {
        Integer port = parsePort();
        if (port == null) {
            return;
        }

        synchronized (this) {
            if (appState != 0) {
                return;
            }
            appState = 1;
        }
        EventQueue.invokeLater(() -> {
            statusLabel.setText("Status: Starting...");
            portTextField.setVisible(false);
            portLabel.setText(String.valueOf(port));
            portLabel.setVisible(true);
        });

        new Thread(() -> {
            try {
                webVerticle.start(port, WEB_STARTUP_TIMEOUT_MS);
                synchronized (this) {
                    appState = 2;
                }
                EventQueue.invokeLater(() -> statusLabel.setText("Status: Ready !"));
            } catch (Exception exception) {
                synchronized (this) {
                    appState = 0;
                }
                EventQueue.invokeLater(() -> {
                    statusLabel.setText("Status: Stopped");
                    portLabel.setVisible(false);
                    portTextField.setVisible(true);
                });
                if (isPortInUseError(exception)) {
                    uiMessageService.showMessage("Web port is already in use. Please change the port.");
                } else {
                    uiMessageService.showMessage("Startup failed: " + exception.getMessage());
                }
            }
        }).start();
    }

    private void stopWebService() {
        synchronized (this) {
            if (appState != 2) {
                EventQueue.invokeLater(() -> {
                    statusLabel.setText("Status: Stopped");
                    portLabel.setVisible(false);
                    portTextField.setVisible(true);
                });
                return;
            }
            appState = 3;
        }
        EventQueue.invokeLater(() -> statusLabel.setText("Status: Stopping..."));

        new Thread(() -> {
            try {
                webVerticle.stop(5000);
            } finally {
                synchronized (this) {
                    appState = 0;
                }
                EventQueue.invokeLater(() -> {
                    statusLabel.setText("Status: Stopped");
                    portLabel.setVisible(false);
                    portTextField.setVisible(true);
                });
            }
        }).start();
    }

    private Integer parsePort() {
        String portString = portTextField.getText();
        String message = "";
        Integer port = null;
        try {
            if (portString == null || portString.isBlank()) {
                throw new RuntimeException("Warning: Please enter a port");
            }
            port = Integer.parseInt(portString);
            if (port < 1024 || port > 65535) {
                throw new RuntimeException("Warning: Port must be an integer between 1024 and 65535");
            }
        } catch (NumberFormatException numberFormatException) {
            message = "Warning: Port must be an integer between 1024 and 65535";
        } catch (RuntimeException runtimeException) {
            message = runtimeException.getMessage();
        }

        if (!message.isBlank()) {
            showTemporaryMessage(message, 5000);
            return null;
        }
        return port;
    }

    private void showTemporaryMessage(String message, long timeoutMillis) {
        uiMessageService.showMessage(message);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (message.equals(uiMessageService.getCurrentMessage())) {
                    uiMessageService.restoreDefaultMessage();
                }
            }
        }, timeoutMillis);
    }

    private void openSigninLocalPrinterDialog() {
        String mac = getLocalMacAddress();
        String hostName = getLocalHostName();
        String localUrl = getExpectedLocalUrl();
        List<String> printers = getLocalPrinterNames();
        signinLocalPrinterUi.open(frame, mac, hostName, localUrl, printers);
    }

    private void openSignedDialog() {
        String localMac = getLocalMacAddress();
        String localPcName = getLocalHostName();
        String localUrl = getExpectedLocalUrl();
        List<String> localPrinters = getLocalPrinterNames();
        signedUi.open(frame, localMac, localPcName, localUrl, localPrinters);
    }

    private void shutdownApplication() {
        applicationContext.close();
    }

    private void showMainFrame() {
        trayUi.showFrame(frame);
    }

    private void hideMainFrame() {
        EventQueue.invokeLater(() -> frame.setVisible(false));
    }

    private String getCurrentWebPort() {
        String runningPort = portLabel.getText();
        if (runningPort != null && !runningPort.isBlank()) {
            return runningPort.trim();
        }
        String inputPort = portTextField.getText();
        if (inputPort != null && !inputPort.isBlank()) {
            return inputPort.trim();
        }
        return DEFAULT_PORT;
    }

    private String getLocalHostName() {
        return AwtUiSupport.resolveDisplayHostName();
    }

    private String getLocalMacAddress() {
        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                byte[] mac = networkInterface.getHardwareAddress();
                if (mac == null || mac.length == 0) {
                    continue;
                }
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < mac.length; i++) {
                    if (i > 0) {
                        builder.append("-");
                    }
                    builder.append(String.format("%02X", mac[i]));
                }
                return builder.toString();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private List<String> getLocalPrinterNames() {
        List<String> printerNames = new ArrayList<>();
        try {
            List<PrinterUtil.PrinterInfo> infos = PrinterUtil.listPrinters();
            for (PrinterUtil.PrinterInfo info : infos) {
                if (info != null && info.name != null && !info.name.isBlank()) {
                    printerNames.add(info.name);
                }
            }
        } catch (Exception ignored) {
        }
        if (printerNames.isEmpty()) {
            try {
                for (PrintService service : java.awt.print.PrinterJob.lookupPrintServices()) {
                    if (service != null && service.getName() != null && !service.getName().isBlank()) {
                        printerNames.add(service.getName());
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return printerNames;
    }

    private String getExpectedLocalUrl() {
        return AwtUiSupport.buildExpectedLocalUrl("http", AwtUiSupport.resolveLanHostName(), getCurrentWebPort());
    }

    private int getHalfFontSize() {
        Font defaultFont = new JLabel("Loading...").getFont();
        int fontSize = defaultFont == null ? 12 : defaultFont.getSize();
        return fontSize / 2 + fontSize % 2;
    }

    private boolean isAutoStartEnabled() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            try {
                String startup = System.getenv("APPDATA") + "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";
                String exePath = getNativeExePath();
                String exeName = new java.io.File(exePath).getName();
                java.io.File lnk = new java.io.File(startup, exeName + ".lnk");
                return lnk.exists();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else if (os.contains("mac")) {
            try {
                String userHome = System.getProperty("user.home");
                String plistPath = userHome + "/Library/LaunchAgents/com.xuesinuo.muppet.plist";
                return new java.io.File(plistPath).exists();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    private void enableAutoStart() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            try {
                String startup = System.getenv("APPDATA") + "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";
                String exePath = getNativeExePath();
                String exeName = new java.io.File(exePath).getName();
                String lnkPath = startup + "\\" + exeName + ".lnk";
                createWindowsShortcut(exePath, lnkPath);
            } catch (Exception e) {
                e.printStackTrace();
                uiMessageService.showMessage("Enable auto-start failed: " + e.getMessage());
            }
        } else if (os.contains("mac")) {
            try {
                String userHome = System.getProperty("user.home");
                String plistPath = userHome + "/Library/LaunchAgents/com.xuesinuo.muppet.plist";
                String appPath = getNativeAppPath();
                String plist = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
                        +
                        "<plist version=\"1.0\">\n" +
                        "<dict>\n" +
                        "    <key>Label</key>\n" +
                        "    <string>com.xuesinuo.muppet</string>\n" +
                        "    <key>ProgramArguments</key>\n" +
                        "    <array>\n" +
                        "        <string>open</string>\n" +
                        "        <string>-a</string>\n" +
                        "        <string>" + appPath + "</string>\n" +
                        "    </array>\n" +
                        "    <key>RunAtLoad</key>\n" +
                        "    <true/>\n" +
                        "</dict>\n" +
                        "</plist>\n";
                java.nio.file.Files.write(java.nio.file.Paths.get(plistPath), plist.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
                uiMessageService.showMessage("Enable auto-start failed: " + e.getMessage());
            }
        }
    }

    private void disableAutoStart() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            try {
                String startup = System.getenv("APPDATA") + "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";
                String exePath = getNativeExePath();
                String exeName = new java.io.File(exePath).getName();
                java.io.File lnk = new java.io.File(startup, exeName + ".lnk");
                if (lnk.exists()) {
                    lnk.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
                uiMessageService.showMessage("Disable auto-start failed: " + e.getMessage());
            }
        } else if (os.contains("mac")) {
            try {
                String userHome = System.getProperty("user.home");
                String plistPath = userHome + "/Library/LaunchAgents/com.xuesinuo.muppet.plist";
                java.io.File plist = new java.io.File(plistPath);
                if (plist.exists()) {
                    plist.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
                uiMessageService.showMessage("Disable auto-start failed: " + e.getMessage());
            }
        }
    }

    private String getNativeExePath() {
        String os = System.getProperty("os.name").toLowerCase();
        String exePath = getAppExePath();
        if (os.contains("win")) {
            String programFiles = System.getenv("ProgramFiles");
            String exe = programFiles + "\\MuppetPrint\\MuppetPrint.exe";
            if (new java.io.File(exe).exists()) {
                return exe;
            }
        }
        return exePath;
    }

    private String getNativeAppPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            String appBundle = "/Applications/MuppetPrint.app";
            if (new java.io.File(appBundle).exists()) {
                return appBundle;
            }
        }
        return getAppJarPath();
    }

    private String getAppExePath() {
        try {
            return new java.io.File(MuppetPrinterUi.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .getAbsolutePath();
        } catch (Exception e) {
            return System.getProperty("java.class.path");
        }
    }

    private String getAppJarPath() {
        try {
            return new java.io.File(MuppetPrinterUi.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .getAbsolutePath();
        } catch (Exception e) {
            return System.getProperty("java.class.path");
        }
    }

    private void createWindowsShortcut(String target, String lnkPath) throws Exception {
        String ps = "$WshShell = New-Object -ComObject WScript.Shell; " +
                "$Shortcut = $WshShell.CreateShortcut('" + lnkPath.replace("\\", "\\\\") + "'); " +
                "$Shortcut.TargetPath = '" + target.replace("\\", "\\\\") + "'; " +
                "$Shortcut.Save();";
        Process process = Runtime.getRuntime().exec(new String[] { "powershell", "-Command", ps });
        process.waitFor();
    }

    private synchronized Image getAppIcon() {
        if (appIcon != null) {
            return appIcon;
        }
        try (java.io.InputStream iconStream = MuppetPrinterUi.class.getResourceAsStream("/app.png")) {
            if (iconStream != null) {
                appIcon = ImageIO.read(iconStream);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return appIcon;
    }

    private void showAlreadyRunningDialog() {
        try {
            JDialog dialog = new JDialog((Frame) null, "Muppet Print", true);
            Image icon = getAppIcon();
            if (icon != null) {
                dialog.setIconImage(icon);
            }
            dialog.setLayout(new java.awt.BorderLayout());
            JLabel message = new JLabel("Muppet Print is already running.", SwingConstants.CENTER);
            JButton confirmButton = new JButton("OK");
            confirmButton.addActionListener(e -> dialog.dispose());

            Font defaultFont = message.getFont();
            if (defaultFont == null) {
                defaultFont = dialog.getFont();
            }
            int fs = defaultFont == null ? 12 : defaultFont.getSize();
            int hfs = fs / 2 + fs % 2;
            confirmButton.setPreferredSize(new Dimension(20 * hfs, 4 * hfs));

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(confirmButton);
            dialog.add(message, java.awt.BorderLayout.CENTER);
            dialog.add(buttonPanel, java.awt.BorderLayout.SOUTH);
            dialog.setSize(320, 140);
            dialog.setLocationRelativeTo(null);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    dialog.dispose();
                }
            });
            dialog.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void exitCurrentProcess() {
        new Thread(() -> {
            try {
                Thread.sleep(120);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            System.exit(0);
        }, "muppet-exit-thread").start();
    }

    private boolean acquireSingleInstanceLock() {
        try {
            Path lockDir = Paths.get(System.getProperty("user.home"), SINGLE_INSTANCE_LOCK_DIR);
            Files.createDirectories(lockDir);
            Path lockPath = lockDir.resolve(SINGLE_INSTANCE_LOCK_FILE);
            singleInstanceLockChannel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            singleInstanceFileLock = singleInstanceLockChannel.tryLock();
            if (singleInstanceFileLock == null) {
                singleInstanceLockChannel.close();
                singleInstanceLockChannel = null;
                return false;
            }
            return true;
        } catch (OverlappingFileLockException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private void releaseSingleInstanceLock() {
        if (singleInstanceFileLock != null) {
            try {
                singleInstanceFileLock.release();
            } catch (IOException ignored) {
            } finally {
                singleInstanceFileLock = null;
            }
        }
        if (singleInstanceLockChannel != null) {
            try {
                singleInstanceLockChannel.close();
            } catch (IOException ignored) {
            } finally {
                singleInstanceLockChannel = null;
            }
        }
    }

    private boolean isPortInUseError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BindException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
