package com.xuesinuo.muppet.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AwtUiSupport {

    private static final String DEFAULT_WEB_PORT = "58080";
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://)([^/?#:]+)(:\\d+)?([^?#]*)?(\\?[^#]*)?(#.*)?$",
            Pattern.CASE_INSENSITIVE);
    private static volatile Font defaultUiFont;

    private AwtUiSupport() {
    }

    public static void applyDefaultFont(Component component) {
        if (component == null) {
            return;
        }
        Font font = resolveDefaultUiFont(component.getFont());
        applyFontRecursively(component, font);
    }

    private static void applyFontRecursively(Component component, Font font) {
        if (component == null || font == null) {
            return;
        }
        component.setFont(font);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyFontRecursively(child, font);
            }
        }
    }

    private static Font resolveDefaultUiFont(Font baseFont) {
        Font cachedFont = defaultUiFont;
        if (cachedFont != null) {
            return deriveFont(cachedFont, baseFont);
        }

        synchronized (AwtUiSupport.class) {
            cachedFont = defaultUiFont;
            if (cachedFont != null) {
                return deriveFont(cachedFont, baseFont);
            }

            String family = pickUiFontFamily();
            int size = baseFont == null || baseFont.getSize() <= 0 ? 12 : baseFont.getSize();
            int style = baseFont == null ? Font.PLAIN : baseFont.getStyle();
            defaultUiFont = new Font(family, style, size);
            return defaultUiFont;
        }
    }

    private static Font deriveFont(Font font, Font baseFont) {
        if (font == null) {
            return baseFont;
        }
        if (baseFont == null) {
            return font;
        }
        return font.deriveFont(baseFont.getStyle(), baseFont.getSize2D());
    }

    private static String pickUiFontFamily() {
        Set<String> availableFamilies = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames(Locale.ROOT));
        for (String candidate : preferredFontFamilies()) {
            if (availableFamilies.contains(candidate)) {
                return candidate;
            }
        }
        return Font.DIALOG;
    }

    private static Set<String> preferredFontFamilies() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        Set<String> families = new LinkedHashSet<>();
        if (osName.contains("win")) {
            families.add("Microsoft YaHei UI");
            families.add("Microsoft YaHei");
            families.add("SimSun");
            families.add("SimHei");
        } else if (osName.contains("mac")) {
            families.add("PingFang SC");
            families.add("Hiragino Sans GB");
            families.add("Heiti SC");
        } else {
            families.add("Noto Sans CJK SC");
            families.add("WenQuanYi Zen Hei");
            families.add("Source Han Sans SC");
        }
        families.add(Font.DIALOG);
        return families;
    }

    public static String resolveDisplayHostName() {
        String hostName = safeHostName(resolveInetHostName());
        if (!hostName.isBlank()) {
            return hostName;
        }
        String envHostName = safeHostName(System.getenv("COMPUTERNAME"));
        if (!envHostName.isBlank()) {
            return envHostName;
        }
        envHostName = safeHostName(System.getenv("HOSTNAME"));
        if (!envHostName.isBlank()) {
            return envHostName;
        }
        return "localhost";
    }

    public static String resolveLanHostName() {
        String displayHostName = safeHostName(resolveDisplayHostName());
        if (looksReachableHost(displayHostName)) {
            return normalizeLanHostSuffix(displayHostName);
        }

        String canonicalHostName = safeHostName(resolveCanonicalHostName());
        if (looksReachableHost(canonicalHostName)) {
            return normalizeLanHostSuffix(canonicalHostName);
        }
        return "localhost";
    }

    private static String resolveInetHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String resolveCanonicalHostName() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String safeHostName(String hostName) {
        if (hostName == null) {
            return "";
        }
        return hostName.trim();
    }

    private static boolean looksReachableHost(String hostName) {
        if (hostName == null || hostName.isBlank()) {
            return false;
        }
        String normalized = hostName.trim().toLowerCase(Locale.ROOT);
        return !"localhost".equals(normalized) && !normalized.startsWith("127.");
    }

    private static String normalizeLanHostSuffix(String hostName) {
        if (hostName == null || hostName.isBlank()) {
            return "localhost";
        }
        String normalized = hostName.trim();
        if (normalized.toLowerCase(Locale.ROOT).endsWith(".local")) {
            return normalized;
        }
        return normalized + ".local";
    }

    public static String buildExpectedLocalUrl(String scheme, String hostName, String port) {
        String safeScheme = (scheme == null || scheme.isBlank()) ? "http" : scheme.trim();
        String safeHost = (hostName == null || hostName.isBlank()) ? "localhost" : hostName.trim();
        String safePort = (port == null || port.isBlank()) ? DEFAULT_WEB_PORT : port.trim();
        return safeScheme + "://" + safeHost + ":" + safePort;
    }

    public static String buildIpUrl(String sourceUrl, String fallbackPort) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return sourceUrl;
        }
        String ip = resolveLocalIpv4();
        String safeFallbackPort = normalizePort(fallbackPort);
        Matcher matcher = URL_PATTERN.matcher(sourceUrl.trim());
        if (!matcher.matches()) {
            if (safeFallbackPort != null) {
                return "http://" + ip + ":" + safeFallbackPort;
            }
            return "http://" + ip;
        }

        String scheme = matcher.group(1);
        String portSegment = matcher.group(3);
        String path = matcher.group(4) == null ? "" : matcher.group(4);
        String query = matcher.group(5) == null ? "" : matcher.group(5);
        String fragment = matcher.group(6) == null ? "" : matcher.group(6);

        if (portSegment == null || portSegment.isBlank()) {
            String effectivePort = safeFallbackPort == null ? DEFAULT_WEB_PORT : safeFallbackPort;
            portSegment = ":" + effectivePort;
        }
        return scheme + ip + (portSegment == null ? "" : portSegment) + path + query + fragment;
    }

    private static String normalizePort(String port) {
        if (port == null || port.isBlank()) {
            return null;
        }
        String trimmed = port.trim();
        if (!trimmed.matches("\\d+")) {
            return null;
        }
        return trimmed;
    }

    public static String resolveLocalIpv4() {
        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                var addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address.isLoopbackAddress() || address.isLinkLocalAddress()) {
                        continue;
                    }
                    String hostAddress = address.getHostAddress();
                    if (hostAddress != null && hostAddress.contains(".")) {
                        return hostAddress;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {
            return "127.0.0.1";
        }
    }
}