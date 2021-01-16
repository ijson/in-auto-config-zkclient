package com.ijson.config;

import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.ijson.config.api.IChangeListener;
import com.ijson.config.api.IChangeableConfig;
import com.ijson.config.api.IConfigFactory;
import com.ijson.config.api.IZkResolver;
import com.ijson.config.base.AbstractConfigFactory;
import com.ijson.config.base.ChangeableConfig;
import com.ijson.config.base.ConfigConstants;
import com.ijson.config.base.ProcessInfo;
import com.ijson.config.helper.ConfigHelper;
import com.ijson.config.helper.ConfigZkResolver;
import com.ijson.config.impl.RemoteConfig;
import com.ijson.config.impl.RemoteConfigWithCache;
import com.ijson.config.helper.ZookeeperHelper;
import com.ijson.config.helper.FileUpdateWatcher;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by cuiyongxu on 17/8/26.
 * @author **
 */
public class ConfigFactory {

    private static final Logger log = LoggerFactory.getLogger(ConfigFactory.class);


    private ConfigFactory() {
    }

    private static IConfigFactory getInstance() {
        return LazyHolder.INSTANCE;
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


    private static class LazyHolder {
        private static final IConfigFactory INSTANCE = newFactory();

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
                    ZookeeperHelper.setCurator(
                            ConfigHelper.newClient(resolver.getServer(), resolver.getAuthType(), resolver.getAuth()));

                    // 找不到配置的本地路径,则只用远程zookeeper配置
                    if (System.getProperty(ConfigConstants.TMP_DIR).equals(configPath.toString())) {
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
            String key = "zkResolver";
            String cls = ConfigHelper.getApplicationConfig().get(key);
            if (Strings.isNullOrEmpty(cls)) {
                cls = System.getProperty(key, "com.ijson.config.helper.ConfigZkResolver");
            }
            try {
                return (IZkResolver) Class.forName(cls).newInstance();
            } catch (Exception e) {
                return new ConfigZkResolver();
            }
        }
    }

    private static class RemoteConfigWithCacheFactory extends RemoteConfigFactory {
        private final Path path;

        RemoteConfigWithCacheFactory(Path localConfigPath, ProcessInfo info) {
            super(info);
            this.path = localConfigPath;
        }

        /**
         * 本地cache根路径
         *
         * @return 路径
         */
        public Path getPath() {
            return path;
        }

        /**
         * 创建LocalConfig并增加更新回调功能
         *
         * @param name 配置名
         * @return 配置
         */
        @Override
        protected IChangeableConfig doCreate(String name) {
            ProcessInfo info = getInfo();
            String path = ZKPaths.makePath(info.getPath(), name);
            File cacheFile = this.path.resolve(name).toFile();
            RemoteConfigWithCache c = new RemoteConfigWithCache(name, path, info.orderedPath(), cacheFile);
            c.loadAndWatchChanges();
            return c;
        }
    }

    private static class RemoteConfigFactory extends AbstractConfigFactory {
        private final ProcessInfo info;

        RemoteConfigFactory(ProcessInfo info) {
            this.info = info;
        }

        /**
         * @return
         * @see ZookeeperHelper#getCurator()
         */
        @Deprecated
        public CuratorFramework getClient() {
            return ZookeeperHelper.getCurator();
        }

        @Deprecated
        public void setClient(CuratorFramework client) {
            ZookeeperHelper.setCurator(client);
        }

        ProcessInfo getInfo() {
            return info;
        }


        /**
         * 创建LocalConfig并增加更新回调功能
         *
         * @param name 配置名
         * @return 配置
         */
        @Override
        protected IChangeableConfig doCreate(String name) {
            String path = ZKPaths.makePath(info.getPath(), name);
            RemoteConfig c = new RemoteConfig(name, path, info.orderedPath());
            c.loadAndWatchChanges();
            return c;
        }
    }

    private static class LocalConfigFactory extends AbstractConfigFactory {
        private final Path path;

        LocalConfigFactory(Path localConfigPath) {
            this.path = localConfigPath;
        }

        /**
         * 本地cache根路径
         *
         * @return 路径
         */
        public Path getPath() {
            return path;
        }

        /**
         * 创建LocalConfig并增加更新回调功能
         *
         * @param name 配置名
         * @return 配置
         */
        @Override
        protected IChangeableConfig doCreate(String name) {
            Path p = path.resolve(name);
            final LocalConfig c = new LocalConfig(name, p);
            FileUpdateWatcher.getInstance().watch(p, (path1, content) -> {
                if (c.isChanged(content)) {
                    log.info("{} changed", path1);
                    c.copyOf(content);
                    c.notifyListeners();
                }
            });
            return c;
        }
    }


    private static class LocalConfig extends ChangeableConfig {

        private final Path path;

        LocalConfig(String name, Path path) {
            super(name);
            this.path = path;
            try {
                if (path.toFile().exists()) {
                    copyOf(Files.toByteArray(path.toFile()));
                }
            } catch (IOException e) {
                copyOf(new byte[0]);
                log.error("configName={}, path={}  exception: {}", name, path, e);
            }
        }

        public Path getPath() {
            return path;
        }

        @Override
        public String toString() {
            return "LocalConfig{" + "name=" + getName() + ", path=" + path + '}';
        }
    }


}


