package com.ijson.config.resolver;

import com.google.common.base.Strings;

import com.ijson.config.base.Config;
import com.ijson.config.helper.ConfigHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import lombok.extern.slf4j.Slf4j;

import static com.ijson.config.base.ConfigConstants.ConfKeys.config_url;
import static com.ijson.config.base.ConfigConstants.ConfKeys.in_zookeeper;
import static com.ijson.config.base.ConfigConstants.Ijson.configUrl;
import static com.ijson.config.base.ConfigConstants.develop;
import static com.ijson.config.base.ConfigConstants.process_name;
import static com.ijson.config.base.ConfigConstants.process_profile;


@Slf4j
public class ConfigZkResolver extends ConfigurableZkResolver {

    protected void readme() {
        java.util.Locale locale = Locale.getDefault();
        if (Locale.SIMPLIFIED_CHINESE.equals(locale)) {
            log.info("配置\t\t\t\t\t\t|默认值\t|描述");
            log.info("config.enableZookeeper\t|false\t|访问远程zookeeper服务器");
            log.info("config.url\t\t\t\t|http://config.ijson.com/in/config/api|zookeeper配置获取地址,可自行编写");
        } else if (Locale.ENGLISH.equals(locale)) {
            log.info("Config\t\t\t\t\t\t|Default\t|description");
            log.info("config.enableZookeeper\t|false\t|Access the remote zookeeper server");
            log.info("config.url\t\t\t\t|http://config.ijson.com/in/config/api|zookeeper Configuration to get the address, can be written");
        }
    }

    protected void customSettings(ByteArrayOutputStream out) {
        Config appConfig = ConfigHelper.getApplicationConfig();
        String configURL = appConfig.get(config_url);

        if (Strings.isNullOrEmpty(configURL)) {
            configURL = System.getProperty(config_url);
        }
        if (Strings.isNullOrEmpty(configURL)) {
            configURL = configUrl;
        }
        String name = appConfig.get(process_name);
        if (!Strings.isNullOrEmpty(appConfig.get("custom.zk.server.url"))) {
            configURL = appConfig.get("custom.zk.server.url");
        }
        if (Strings.isNullOrEmpty(name)) {
            name = in_zookeeper;
        }
        String s = configURL + "?profile=" + appConfig.get(process_profile, develop) + "&name=" + name;
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
