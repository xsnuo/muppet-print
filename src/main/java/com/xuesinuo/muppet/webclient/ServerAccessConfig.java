package com.xuesinuo.muppet.webclient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ServerAccessConfig {

    @Value("${release.server.host:}")
    private String releaseServerHost;

    @Value("${release.server.token:}")
    private String releaseServerToken;

    public boolean isConfigured() {
        return !safeTrim(releaseServerHost).isBlank();
    }

    public String signinUrl() {
        return buildServerUrl("/muppet/signin");
    }

    public String signedUrl(String mac) {
        String encodedMac = URLEncoder.encode(safeTrim(mac), StandardCharsets.UTF_8);
        return buildServerUrl("/muppet/signed?mac=" + encodedMac);
    }

    public String errorLogUrl() {
        return buildServerUrl("/muppet/log");
    }

    public String groupsUrl() {
        return buildServerUrl("/muppet/groups");
    }

    public String signoutUrl() {
        return buildServerUrl("/muppet/signout");
    }

    public String token() {
        return safeTrim(releaseServerToken);
    }

    private String buildServerUrl(String pathAndQuery) {
        String host = safeTrim(releaseServerHost);
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
        return URI.create(hostPart + pathAndQuery).toString();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
