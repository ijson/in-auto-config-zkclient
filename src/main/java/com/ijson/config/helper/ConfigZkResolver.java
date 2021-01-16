package com.ijson.config.helper;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.ijson.config.api.IZkResolver;
import com.ijson.config.base.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.ijson.config.base.ConfigConstants.ConfKeys.*;
import static com.ijson.config.base.ConfigConstants.*;

/**
 * @author *
 */
public class ConfigZkResolver extends ConfigurableZkResolver {

    public static final Logger log = LoggerFactory.getLogger(ConfigZkResolver.class);

    @Override
    protected void customSettings(ByteArrayOutputStream out) {
        Config appConfig = ConfigHelper.getApplicationConfig();
        String configUrl = appConfig.get(Ijson.CONFIG_URL);

        if (Strings.isNullOrEmpty(configUrl)) {
            configUrl = System.getProperty(Ijson.CONFIG_URL);
        }
        if (Strings.isNullOrEmpty(configUrl)) {
            configUrl = Ijson.CONFIG_URL;
        }
        String name = appConfig.get(PROCESS_PROFILE);
        if (!Strings.isNullOrEmpty(appConfig.get(ZK_SERVER_URL))) {
            configUrl = appConfig.get(ZK_SERVER_URL);
        }
        if (Strings.isNullOrEmpty(name)) {
            name = IN_ZOOKEEPER;
        }
        String s = configUrl + "?profile=" + appConfig.get(PROCESS_PROFILE, DEVELOP) + "&name=" + name;
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
            log.error("cannot parse url={}  {}", cmsUrl, e);
        } catch (IOException e) {
            log.error("cannot load from url={}  {}", cmsUrl, e);
        } finally {
            log.info("load zookeeper settings from {}", cmsUrl);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


}

class ConfigurableZkResolver implements IZkResolver {


    public static final Logger log = LoggerFactory.getLogger(ConfigurableZkResolver.class);


    private boolean enable = false;
    private String servers;
    private String auth;
    private String authType;
    private String basePath;

    /**
     * <ul>
     * <li>从本地application-xxx.properties加载</li>
     * <li>从autoconf/cms-zookeeper加载</li>
     * <li>从环境变量加载</li>
     * </ul>
     */
    @Override
    public void resolve() {
        Config app = ConfigHelper.getApplicationConfig();
        if (app.getBool(CONFIG_ENABLE_ZOOKEEPER, false)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // 复制本地配置
            try {
                out.write(app.getContent());
            } catch (IOException e) {
                log.error("cannot clone from appConfig ", e);
            }

            // 从本地配置 autoconf/cms-zookeeper中加载配置
            appendAutoConfig(out);

            // 自定义加载配置
            customSettings(out);

            // 再把环境变量的设置导入进来覆盖
            appendEnvironments(out);

            Config config = new Config();
            config.copyOf(out.toByteArray());
            servers = config.get(ZOOKEEPER_SERVERS);
            if (Strings.isNullOrEmpty(servers)) {
                enable = false;
                return;
            }
            auth = config.get(ZOOKEEPER_AUTHENTICATION);
            authType = config.get(ZOOKEEPER_AUTHENTICATION_TYPE);
            basePath = config.get(ZOOKEEPER_BASE_PATH, "/in/config");
        }


    }

    private void appendEnvironments(ByteArrayOutputStream out) {
        List<String> keys =
                Lists.newArrayList(CONFIG_ENABLE_ZOOKEEPER, ZOOKEEPER_SERVERS, ZOOKEEPER_AUTHENTICATION, ZOOKEEPER_AUTHENTICATION_TYPE, ZOOKEEPER_BASE_PATH);
        for (String i : keys) {
            try {
                append(out, System.getProperty(i));
            } catch (IOException e) {
                log.error("cannot append {}  {}", i, e);
            }
        }
    }

    private void appendAutoConfig(ByteArrayOutputStream out) {
        Path cmsConfig = ConfigHelper.getConfigPath().resolve(IN_ZOOKEEPER);
        if (cmsConfig.toFile().exists()) {
            out.write('\n');
            try {
                Files.copy(cmsConfig, out);
            } catch (IOException e) {
                log.error("cannot load from {}  {}", cmsConfig, e);
            }
        }
    }

    /**
     * 可以继承并实现自定义的设置
     *
     * @param out 将自定义的配置写入此输出流
     */
    protected void customSettings(ByteArrayOutputStream out) {
    }

    protected void append(OutputStream out, String s) throws IOException {
        if (s != null) {
            out.write('\n');
            out.write(s.getBytes(Charsets.UTF_8));
        }
    }

    @Override
    public boolean isEnable() {
        return this.enable;
    }

    @Override
    public String getServer() {
        return this.servers;
    }

    @Override
    public String getAuth() {
        return this.auth;
    }

    @Override
    public String getAuthType() {
        return this.authType;
    }

    @Override
    public String getBasePath() {
        return this.basePath;
    }
}
