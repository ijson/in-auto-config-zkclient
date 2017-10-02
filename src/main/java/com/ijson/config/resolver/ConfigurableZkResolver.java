
package com.ijson.config.resolver;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.ijson.config.api.IZkResolver;
import com.ijson.config.base.Config;
import com.ijson.config.helper.ConfigHelper;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class ConfigurableZkResolver implements IZkResolver {
    public static final String CONFIG_ENABLE_ZOOKEEPER = "config.enableZookeeper";
    public static final String ZOOKEEPER_SERVERS = "zookeeper.servers";
    public static final String ZOOKEEPER_AUTHENTICATION = "zookeeper.authentication";
    public static final String ZOOKEEPER_AUTHENTICATION_TYPE = "zookeeper.authenticationType";
    public static final String ZOOKEEPER_BASE_PATH = "zookeeper.basePath";
    private boolean enable = true;
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
    public void resolve() {
        Config app = ConfigHelper.getApplicationConfig();
        // 本地配置禁用zookeeper,就直接返回了
        if (!app.getBool(CONFIG_ENABLE_ZOOKEEPER, true)) {
            enable = false;
            return;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 复制本地配置
        try {
            out.write(app.getContent());
        } catch (IOException e) {
            log.error("cannot clone from appConfig", e);
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

    private void appendEnvironments(ByteArrayOutputStream out) {
        List<String> keys =
                Lists.newArrayList(CONFIG_ENABLE_ZOOKEEPER, ZOOKEEPER_SERVERS, ZOOKEEPER_AUTHENTICATION, ZOOKEEPER_AUTHENTICATION_TYPE, ZOOKEEPER_BASE_PATH);
        for (String i : keys) {
            try {
                append(out, System.getProperty(i));
            } catch (IOException e) {
                log.error("cannot append {}", i, e);
            }
        }
    }

    private void appendAutoConfig(ByteArrayOutputStream out) {
        Path cmsConfig = ConfigHelper.getConfigPath().resolve("in-zookeeper");
        if (cmsConfig.toFile().exists()) {
            out.write('\n');
            try {
                Files.copy(cmsConfig, out);
            } catch (IOException e) {
                log.error("cannot load from {}", cmsConfig, e);
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
