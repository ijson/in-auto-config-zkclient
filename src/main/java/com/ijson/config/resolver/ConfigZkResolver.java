package com.ijson.config.resolver;

import com.google.common.base.Strings;
import com.ijson.config.base.Config;
import com.ijson.config.helper.ConfigHelper;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


@Slf4j
public class ConfigZkResolver extends ConfigurableZkResolver {



    protected void customSettings(ByteArrayOutputStream out) {
        String key = "config.url";
        Config appConfig = ConfigHelper.getApplicationConfig();
        String configURL = appConfig.get(key);
        if (Strings.isNullOrEmpty(configURL)) {
            configURL = System.getProperty(key);
        }
        if (Strings.isNullOrEmpty(configURL)) {
            configURL = "http://config.ijson.com/in/config/api";
        }
        String name = appConfig.get("process.name");
        if (!Strings.isNullOrEmpty(appConfig.get("custom.zk.server.url"))) {
            configURL = appConfig.get("custom.zk.server.url");
        }
        if (Strings.isNullOrEmpty(name)) {
            name = "in-zookeeper";
        }
        String s = configURL + "?profile=" + appConfig.get("process.profile", "dev") + "&name=" + name;
        fetchContent(s, out);
    }

    private void fetchContent(String cmsUrl, ByteArrayOutputStream out) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(cmsUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.connect();
            out.write('\n');

            try (InputStream in = connection.getInputStream()) {
                int ch = in.read();
                while (ch != -1) {
                    out.write(ch);
                    ch = in.read();
                }
            }
        } catch (MalformedURLException e) {
            log.error("cannot parse url={}", cmsUrl, e);
        } catch (IOException e) {
            log.error("cannot load from url={}", cmsUrl, e);
        } finally {
            log.info("load zookeeper settings from {}", cmsUrl);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
