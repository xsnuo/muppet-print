package com.xuesinuo.muppet;

import java.io.IOException;
import java.awt.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import javax.print.PrintService;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.BindException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import com.microsoft.playwright.Playwright;
import com.xuesinuo.muppet.tool.PrinterUtil;
import com.xuesinuo.muppet.ui.MuppetPrinterUi;
import com.xuesinuo.muppet.ui.SigninLocalPrinterUi;
import com.xuesinuo.muppet.ui.SignedUi;
import com.xuesinuo.muppet.vertx.WebVerticle;

@SpringBootApplication
public class UiStarter {

    private static final String SINGLE_INSTANCE_LOCK_DIR = ".muppet-print";
    private static final String SINGLE_INSTANCE_LOCK_FILE = "instance.lock";
    private static final long WEB_STARTUP_TIMEOUT_MS = 8000;

    public static volatile int appState = 0; // 0:停止 1:正在启动 2:运行 3:正在关闭
    public static volatile ApplicationContext springContext;

    public static final String port = "58080";
    public static String errorMessage = "";
    private static final Properties appConfig = loadAppConfig();
    private static final boolean signinEnable = Boolean.parseBoolean(getConfigValue("release.signin.enable", "false"));
    private static final HttpClient httpClient = HttpClient.newBuilder().build();
    private static MuppetPrinterUi muppetPrinterUi;

    // 系统托盘相关
    private static TrayIcon trayIcon;
    private static SystemTray systemTray;
    private static FileChannel singleInstanceLockChannel;
    private static FileLock singleInstanceFileLock;
    private static Image appIcon;

    public static void main(String[] args) {
        if (!acquireSingleInstanceLock()) {
            showAlreadyRunningDialog();
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(UiStarter::releaseSingleInstanceLock));
        java.awt.Image icon = getAppIcon();
        muppetPrinterUi = new MuppetPrinterUi(signinEnable, icon, UiStarter::startSpringApplication,
                UiStarter::stopSpringApplication, UiStarter::openSigninLocalPrinterDialog,
                UiStarter::openSignedDialog, UiStarter::isAutoStartEnabled, enabled -> {
                    if (enabled) {
                        enableAutoStart();
                    } else {
                        disableAutoStart();
                    }
                }, port);
        Frame frame = muppetPrinterUi.getFrame();

        // 托盘支持
        if (SystemTray.isSupported()) {
            systemTray = SystemTray.getSystemTray();
            if (icon == null) {
                // fallback icon
                icon = Toolkit.getDefaultToolkit().createImage(new byte[0]);
            }
            PopupMenu popupMenu = new PopupMenu();
            MenuItem openItem = new MenuItem("Open");
            openItem.addActionListener(e -> {
                frame.setVisible(true);
                frame.setState(Frame.NORMAL);
                frame.toFront();
            });
            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                System.exit(0);
            });
            popupMenu.add(openItem);
            popupMenu.addSeparator();
            popupMenu.add(exitItem);
            trayIcon = new TrayIcon(icon, "Muppet Printer", popupMenu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> {
                frame.setVisible(true);
                frame.setState(Frame.NORMAL);
                frame.toFront();
            });
            try {
                systemTray.add(trayIcon);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 关闭窗口时隐藏到托盘
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    frame.setVisible(false);
                }
            });
        } else {
            // 不支持托盘，直接关闭
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
        }

        // 检查Playwright插件
        muppetPrinterUi.getMessageLabel().setText("Loading update. Please wait...");
        new Thread(() -> {
            try (Playwright playwright = Playwright.create()) {
                muppetPrinterUi.getMessageLabel().setText("Update completed.");
                startSpringApplication();
                muppetPrinterUi.getMessageLabel().setText(errorMessage);
            } catch (Exception e) {
                e.printStackTrace();
                muppetPrinterUi.getMessageLabel().setText("Setup failed!" + e.getMessage());
                return;
            }
        }).start();
    }

    /**
     * 判断当前是否已设置开机自启动
     */
    private static boolean isAutoStartEnabled() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows: 检查启动文件夹是否有快捷方式
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
            // macOS: 检查LaunchAgent
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

    /**
     * 启用开机自启动
     */
    private static void enableAutoStart() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows: 复制exe的快捷方式到启动文件夹
            try {
                String startup = System.getenv("APPDATA") + "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";
                String exePath = getNativeExePath();
                String exeName = new java.io.File(exePath).getName();
                String lnkPath = startup + "\\" + exeName + ".lnk";
                createWindowsShortcut(exePath, lnkPath);
            } catch (Exception e) {
                e.printStackTrace();
                error("自启动设置失败: " + e.getMessage());
            }
        } else if (os.contains("mac")) {
            // macOS: 写入LaunchAgent plist，使用 open -a 启动 .app，保证 Dock 图标正常
            try {
                String userHome = System.getProperty("user.home");
                String plistPath = userHome + "/Library/LaunchAgents/com.xuesinuo.muppet.plist";
                String appPath = getNativeAppPath();
                String plist = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
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
                error("自启动设置失败: " + e.getMessage());
            }
        }
    }

    /**
     * 关闭开机自启动
     */
    private static void disableAutoStart() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            try {
                String startup = System.getenv("APPDATA") + "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";
                String exePath = getNativeExePath();
                String exeName = new java.io.File(exePath).getName();
                java.io.File lnk = new java.io.File(startup, exeName + ".lnk");
                if (lnk.exists()) lnk.delete();
            } catch (Exception e) {
                e.printStackTrace();
                error("自启动取消失败: " + e.getMessage());
            }
        } else if (os.contains("mac")) {
            try {
                String userHome = System.getProperty("user.home");
                String plistPath = userHome + "/Library/LaunchAgents/com.xuesinuo.muppet.plist";
                java.io.File plist = new java.io.File(plistPath);
                if (plist.exists()) plist.delete();
            } catch (Exception e) {
                e.printStackTrace();
                error("自启动取消失败: " + e.getMessage());
            }
        }
    }

    // 获取原生exe路径（Windows打包后）
    private static String getNativeExePath() {
        String os = System.getProperty("os.name").toLowerCase();
        String exePath = getAppExePath();
        if (os.contains("win")) {
            String programFiles = System.getenv("ProgramFiles");
            String exe = programFiles + "\\MuppetPrint\\MuppetPrint.exe";
            if (new java.io.File(exe).exists()) return exe;
        }
        return exePath;
    }

    // 获取原生app路径（macOS打包后）
    private static String getNativeAppPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            String appBundle = "/Applications/MuppetPrint.app";
            if (new java.io.File(appBundle).exists()) return appBundle;
        }
        // fallback: 返回 jar 路径
        return getAppJarPath();
    }

    // 获取当前应用exe路径（如果是exe/dmg打包）
    private static String getAppExePath() {
        try {
            return new java.io.File(UiStarter.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .getAbsolutePath();
        } catch (Exception e) {
            return System.getProperty("java.class.path");
        }
    }

    // 获取当前应用jar路径（macOS用）
    private static String getAppJarPath() {
        try {
            return new java.io.File(UiStarter.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .getAbsolutePath();
        } catch (Exception e) {
            return System.getProperty("java.class.path");
        }
    }

    // 创建Windows快捷方式（调用powershell）
    private static void createWindowsShortcut(String target, String lnkPath) throws Exception {
        String ps = "$WshShell = New-Object -ComObject WScript.Shell; " +
                "$Shortcut = $WshShell.CreateShortcut('" + lnkPath.replace("\\", "\\\\") + "'); " +
                "$Shortcut.TargetPath = '" + target.replace("\\", "\\\\") + "'; " +
                "$Shortcut.Save();";
        Process p = Runtime.getRuntime().exec(new String[] { "powershell", "-Command", ps });
        p.waitFor();
    }

    /**
     * UI上显示错误信息
     */
    public static void error(String msg) {
        if (muppetPrinterUi != null) {
            muppetPrinterUi.getMessageLabel().setText(msg);
        }
    }

    private static boolean acquireSingleInstanceLock() {
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

    private static void releaseSingleInstanceLock() {
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

    private static synchronized Image getAppIcon() {
        if (appIcon != null) {
            return appIcon;
        }
        try (java.io.InputStream iconStream = UiStarter.class.getResourceAsStream("/app.png")) {
            if (iconStream != null) {
                appIcon = ImageIO.read(iconStream);
            }
        } catch (Exception e) {
            e.printStackTrace();// 忽略图标加载失败
        }
        return appIcon;
    }

    private static void showAlreadyRunningDialog() {
        try {
            Dialog dialog = new Dialog((Frame) null, "Muppet Print", true);
            Image icon = getAppIcon();
            if (icon != null) {
                dialog.setIconImage(icon);
            }
            dialog.setLayout(new BorderLayout());
            Label message = new Label("Muppet Print is already running.", Label.CENTER);
            Button confirmButton = new Button("OK");
            confirmButton.addActionListener(e -> dialog.dispose());

            Font defaultFont = message.getFont();
            if (defaultFont == null) {
                defaultFont = dialog.getFont();
            }
            int fs = defaultFont == null ? 12 : defaultFont.getSize();
            int hfs = fs / 2 + fs % 2;
            confirmButton.setPreferredSize(new Dimension(20 * hfs, 4 * hfs));

            Panel buttonPanel = new Panel();
            buttonPanel.add(confirmButton);
            dialog.add(message, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
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

    /**
     * 启动Spring服务
     */
    public static void startSpringApplication() {
        Integer port = null;
        String message = "";
        String portString = muppetPrinterUi.getPortTextField().getText();
        try {
            if (portString == null || portString.isBlank()) {
                throw new RuntimeException("Warning: Please enter a port");
            }
            port = Integer.parseInt(portString);
            if (port < 1024 || port > 65535) {
                throw new RuntimeException("Warning: Port must be an integer between 1024 and 65535");
            }
        } catch (NumberFormatException nfe) {
            message = "Warning: Port must be an integer between 1024 and 65535";
        } catch (RuntimeException re) {
            message = re.getMessage();
        }
        if (!message.isBlank()) {
            final String msg = message;
            muppetPrinterUi.getMessageLabel().setText(msg);
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (msg.equals(muppetPrinterUi.getMessageLabel().getText())) {
                        muppetPrinterUi.getMessageLabel().setText(errorMessage);
                    }
                }
            }, 5000);
            return;
        }
        if (port == null) {
            return;
        }

        synchronized (UiStarter.class) {
            if (appState != 0) {
                return;
            }
            appState = 1;
            muppetPrinterUi.getStatusLabel().setText("Status: Starting...");
            muppetPrinterUi.getPortTextField().setVisible(false);
            muppetPrinterUi.getPortLabel().setText("" + port);
            muppetPrinterUi.getPortLabel().setVisible(true);
        }
        WebVerticle.port = port;
        new Thread(() -> {
            try {
                WebVerticle.resetStartupSignal();
                springContext = SpringApplication.run(UiStarter.class, new String[0]);
                WebVerticle.awaitStartupResult(WEB_STARTUP_TIMEOUT_MS);
                System.out.println("Startup complete: " + springContext);
                muppetPrinterUi.getStatusLabel().setText("Status: Ready !");
                appState = 2;
            } catch (Exception ex) {
                if (springContext != null) {
                    try {
                        SpringApplication.exit(springContext);
                    } catch (Exception ignored) {
                    }
                    springContext = null;
                }
                appState = 0;
                muppetPrinterUi.getStatusLabel().setText("Status: Stopped");
                muppetPrinterUi.getPortLabel().setVisible(false);
                muppetPrinterUi.getPortTextField().setVisible(true);
                if (isPortInUseError(ex)) {
                    error("Web port is already in use. Please change the port.");
                } else {
                    error("Startup failed: " + ex.getMessage());
                }
            }
        }).start();
    }

    private static boolean isPortInUseError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BindException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 停止Spring服务
     */
    public static void stopSpringApplication() {
        synchronized (UiStarter.class) {
            if (appState != 2) {
                muppetPrinterUi.getStatusLabel().setText("Status: Stopped");
                muppetPrinterUi.getPortLabel().setVisible(false);
                muppetPrinterUi.getPortTextField().setVisible(true);
                return;
            }
            appState = 3;
            muppetPrinterUi.getStatusLabel().setText("Status: Stopping...");
        }
        new Thread(() -> {
            SpringApplication.exit(springContext);
            springContext = null;
            appState = 0;
            muppetPrinterUi.getStatusLabel().setText("Status: Stopped");
            muppetPrinterUi.getPortLabel().setVisible(false);
            muppetPrinterUi.getPortTextField().setVisible(true);
        }).start();
    }

    private static Properties loadAppConfig() {
        try {
            YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
            yaml.setResources(new ClassPathResource("application.yml"));
            Properties properties = yaml.getObject();
            return properties == null ? new Properties() : properties;
        } catch (Exception ignored) {
            return new Properties();
        }
    }

    private static String getConfigValue(String key, String defaultValue) {
        if (springContext != null) {
            String contextValue = springContext.getEnvironment().getProperty(key);
            if (contextValue != null) {
                return contextValue.trim();
            }
        }
        String fileValue = appConfig.getProperty(key);
        if (fileValue == null) {
            return defaultValue;
        }
        return fileValue.trim();
    }

    private static String getCurrentWebPort() {
        String runningPort = muppetPrinterUi.getPortLabel().getText();
        if (runningPort != null && !runningPort.isBlank()) {
            return runningPort.trim();
        }
        String inputPort = muppetPrinterUi.getPortTextField().getText();
        if (inputPort != null && !inputPort.isBlank()) {
            return inputPort.trim();
        }
        return port;
    }

    private static String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return "localhost";
        }
    }

    private static String getLocalMacAddress() {
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

    private static List<String> getLocalPrinterNames() {
        List<String> printerNames = new ArrayList<>();
        try {
            List<PrinterUtil.PrinterInfo> infos = PrinterUtil.listPrinters();
            for (PrinterUtil.PrinterInfo info : infos) {
                if (info != null && info.getName() != null && !info.getName().isBlank()) {
                    printerNames.add(info.getName());
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

    private static String getExpectedLocalUrl() {
        return "http://" + getLocalHostName() + ":" + getCurrentWebPort();
    }

    private static String normalizeServerPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        String value = prefix.trim();
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static URI buildServerUri(String pathAndQuery) {
        String host = getConfigValue("release.server.host", "");
        if (host.isBlank()) {
            return null;
        }
        String hostPart = host;
        if (!hostPart.startsWith("http://") && !hostPart.startsWith("https://")) {
            hostPart = "https://" + hostPart;
        }
        if (hostPart.endsWith("/")) {
            hostPart = hostPart.substring(0, hostPart.length() - 1);
        }
        return URI.create(hostPart + pathAndQuery);
    }

    private static void openSigninLocalPrinterDialog() {
        String mac = getLocalMacAddress();
        String hostName = getLocalHostName();
        String localUrl = getExpectedLocalUrl();
        List<String> printers = getLocalPrinterNames();
        URI signinUri = buildServerUri(normalizeServerPrefix(getConfigValue("release.server.prefix", "")) + "/muppet/signin");
        SigninLocalPrinterUi.show(muppetPrinterUi.getFrame(), httpClient, signinUri,
                getConfigValue("release.server.token", ""), mac, hostName, localUrl, printers);
    }

    private static void openSignedDialog() {
        String localMac = getLocalMacAddress();
        String localPcName = getLocalHostName();
        String localUrl = getExpectedLocalUrl();
        List<String> localPrinters = getLocalPrinterNames();

        String encodedMac = URLEncoder.encode(localMac, StandardCharsets.UTF_8);
        String queryPath = "/muppet/signed?mac=" + encodedMac;
        URI signedUri = buildServerUri(queryPath);
        SignedUi.show(muppetPrinterUi.getFrame(), httpClient, signedUri, getConfigValue("release.server.token", ""),
                localMac, localPcName, localUrl, localPrinters);
    }
}
