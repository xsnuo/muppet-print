package com.xuesinuo.muppet.ui;

import java.awt.Frame;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.springframework.stereotype.Component;

@Component
public class TrayUi {

    private TrayIcon trayIcon;
    private SystemTray systemTray;

    public void init(Frame frame, Image icon, Runnable onOpen, Runnable onHide, Runnable onExit) {
        if (SystemTray.isSupported()) {
            systemTray = SystemTray.getSystemTray();
            Image trayImage = icon;
            if (trayImage == null) {
                trayImage = Toolkit.getDefaultToolkit().createImage(new byte[0]);
            }
            PopupMenu popupMenu = new PopupMenu();
            MenuItem openItem = new MenuItem("Open");
            openItem.addActionListener(e -> onOpen.run());
            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> onExit.run());
            popupMenu.add(openItem);
            popupMenu.addSeparator();
            popupMenu.add(exitItem);
            trayIcon = new TrayIcon(trayImage, "Muppet Printer", popupMenu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> onOpen.run());
            try {
                systemTray.add(trayIcon);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    onHide.run();
                }
            });
            return;
        }

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onExit.run();
            }
        });
    }

    public void remove() {
        if (systemTray != null && trayIcon != null) {
            systemTray.remove(trayIcon);
            trayIcon = null;
        }
    }

    public void showFrame(Frame frame) {
        EventQueue.invokeLater(() -> {
            frame.setVisible(true);
            frame.setExtendedState(Frame.NORMAL);
            // 通过临时置顶确保窗口在 macOS 下可靠回到前台。
            frame.setAlwaysOnTop(true);
            frame.toFront();
            frame.requestFocus();
            frame.setAlwaysOnTop(false);
        });
    }
}
