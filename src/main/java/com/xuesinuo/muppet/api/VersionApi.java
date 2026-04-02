package com.xuesinuo.muppet.api;

import java.util.HashMap;

import org.springframework.stereotype.Component;

import com.xuesinuo.muppet.UiStarter;
import com.xuesinuo.muppet.config.ApiResult;
import com.xuesinuo.muppet.ui.UiMessageService;
import com.xuesinuo.muppet.webclient.VersionWebClient;
import com.xuesinuo.xtool.Np;

import io.vertx.ext.web.Router;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VersionApi {
    private final Router router;
    private final VersionWebClient versionWebClient;
    private final UiMessageService uiMessageService;

    @PostConstruct
    public void start() {
        uiVersion();
        getVersion();
    }

    private void uiVersion() {
        versionWebClient.queryLatestVersion()
                .onSuccess(resp -> {
                    if (resp.isSuccess() && Np.i(resp.getData()).notEq(UiStarter.APP_VERSION)) {
                        uiMessageService.setDefaultMessage("New version available.");
                    }
                });
    }

    /** 程序版本号 */
    private void getVersion() {
        router.route("/api/version").handler(http -> {
            HashMap<String, Object> data = new HashMap<>();
            data.put("version", UiStarter.APP_VERSION);
            versionWebClient.queryLatestVersion()
                    .onSuccess(result -> {
                        if (result.isSuccess()) {
                            data.put("newVersion", result.getData());
                        }
                    })
                    .onComplete(resp -> {
                        http.response().write(ApiResult.ok(data));
                        http.next();
                    });

        });
    }
}
