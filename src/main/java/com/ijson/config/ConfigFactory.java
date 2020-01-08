package com.ijson.config;

import com.google.common.base.Strings;
import com.ijson.config.api.IChangeListener;
import com.ijson.config.api.IChangeableConfig;
import com.ijson.config.api.IConfigFactory;
import com.ijson.config.api.IZkResolver;
import com.ijson.config.base.ProcessInfo;
import com.ijson.config.helper.ConfigHelper;
import com.ijson.config.resolver.ConfigZkResolver;
import com.ijson.config.util.ZookeeperUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Created by cuiyongxu on 17/8/26.
 */
public class ConfigFactory {

    public static final Logger log = LoggerFactory.getLogger(ConfigFactory.class);


    private ConfigFactory() {
    }

    public static IConfigFactory getInstance() {
        return LazyHolder.instance;
    }

    public static IChangeableConfig getConfig(String name) {
        return getInstance().getConfig(name);
    }

    public static IChangeableConfig getConfig(String name, IChangeListener listener) {
        return getInstance().getConfig(name, listener);
    }

    public static IChangeableConfig getConfig(String name, IChangeListener listener, boolean loadAfterRegister) {
        return getInstance().getConfig(name, listener, loadAfterRegister);
    }

    public static boolean hasConfig(String name) {
        return getInstance().hasConfig(name);
    }


    private static class LazyHolder {
        private static final IConfigFactory instance = newFactory();

        private static IConfigFactory newFactory() {
            return doCreate();
        }

        private static IConfigFactory doCreate() {
            Path configPath = ConfigHelper.getConfigPath();
            IZkResolver resolver = getZkResolver();
            try {
                resolver.resolve();
                ProcessInfo processInfo = ConfigHelper.getProcessInfo();
                if (resolver.isEnable() && !Strings.isNullOrEmpty(resolver.getServer())) {
                    processInfo.setPath(resolver.getBasePath());
                    ZookeeperUtil.setCurator(
                            ConfigHelper.newClient(resolver.getServer(), resolver.getAuthType(), resolver.getAuth()));

                    // 找不到配置的本地路径,则只用远程zookeeper配置
                    if (System.getProperty("java.io.tmpdir").equals(configPath.toString())) {
                        return new RemoteConfigFactory(processInfo);
                    }

                    // 使用远程zookeeper配置并启用本地cache功能
                    return new RemoteConfigWithCacheFactory(configPath, processInfo);
                }
            } catch (Exception e) {
                log.error("cannot resolve zookeeper settings", e);
            }
            return new LocalConfigFactory(configPath);
        }

        private static IZkResolver getZkResolver() {
            String key = "zookeeperResolver";
            String cls = ConfigHelper.getApplicationConfig().get(key);
            if (Strings.isNullOrEmpty(cls)) {
                cls = System.getProperty(key, "com.ijson.resolver.ConfigZkResolver");
            }
            try {
                return (IZkResolver) Class.forName(cls).newInstance();
            } catch (Exception e) {
                return new ConfigZkResolver();
            }
        }
    }
}
