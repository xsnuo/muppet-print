package com.xuesinuo.muppet.webclient;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class GroupWebClient {

    private final WebClient webClient;
    private final ServerAccessConfig serverAccessConfig;

    public Future<WebClientResult<List<GroupOption>>> queryGroups() {
        if (!serverAccessConfig.isConfigured()) {
            log.error("server-api groups failed: release.server.host is not configured");
            return Future.succeededFuture(WebClientResult.fail("SYSTEM_ERROR", "Server groups URL is not configured."));
        }
        String url = serverAccessConfig.groupsUrl();
        var request = webClient.getAbs(url);
        String token = serverAccessConfig.token();
        if (!token.isBlank()) {
            request.putHeader("Muppet-Token", token);
        }
        log.info("server-api groups request: url={}", url);
        return request.send()
                .map(response -> {
                    String responseBody = response.bodyAsString();
                    log.info("server-api groups response: status={}, body={}", response.statusCode(), responseBody);
                    WebClientResult<List<GroupOption>> result = parseEnvelopeGroups(response.statusCode(), responseBody);
                    if (!result.isSuccess()) {
                        log.error("server-api groups business failed: code={}, message={}", result.getCode(), result.getMessage());
                    }
                    return result;
                })
                .recover(error -> {
                    log.error("server-api groups request exception", error);
                    return Future.succeededFuture(WebClientResult.fail("SYSTEM_ERROR", error.getMessage()));
                });
    }

    private WebClientResult<List<GroupOption>> parseEnvelopeGroups(int statusCode, String body) {
        if (statusCode != 200) {
            return WebClientResult.fail("SYSTEM_ERROR", "HTTP status: " + statusCode);
        }
        JsonObject jsonObject;
        try {
            jsonObject = new JsonObject(body == null ? "" : body);
        } catch (Exception exception) {
            return WebClientResult.fail("SYSTEM_ERROR", "Invalid response body");
        }

        String code = jsonObject.getString("code", "SYSTEM_ERROR");
        String message = jsonObject.getString("message", "");
        if (!"SUCCESS".equals(code)) {
            return WebClientResult.fail(code, message);
        }

        JsonArray groupArray = null;
        JsonObject data = jsonObject.getJsonObject("data");
        if (data != null) {
            groupArray = data.getJsonArray("list");
        }
        List<GroupOption> groupOptions = new ArrayList<>();
        if (groupArray != null) {
            for (int i = 0; i < groupArray.size(); i++) {
                JsonObject item = groupArray.getJsonObject(i);
                if (item == null) {
                    continue;
                }
                GroupOption groupOption = new GroupOption();
                groupOption.setLabel(item.getString("label", ""));
                groupOption.setValue(item.getString("value", ""));
                groupOptions.add(groupOption);
            }
        }
        return WebClientResult.success(groupOptions);
    }
}
