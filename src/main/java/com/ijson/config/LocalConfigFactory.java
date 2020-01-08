package com.ijson.config;

import com.ijson.config.api.IChangeableConfig;
import com.ijson.config.base.AbstractConfigFactory;
import com.ijson.config.helper.ConfigHelper;
import com.ijson.config.impl.LocalConfig;
import com.ijson.config.watcher.FileUpdateWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class LocalConfigFactory extends AbstractConfigFactory {


    public static final Logger log = LoggerFactory.getLogger(LocalConfigFactory.class);
    private final Path path;

    public LocalConfigFactory(Path localConfigPath) {
        this.path = localConfigPath;
    }

    public static LocalConfigFactory getInstance() {
        return LazyHolder.instance;
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

    private static class LazyHolder {
        private static final LocalConfigFactory instance = new LocalConfigFactory(ConfigHelper.getConfigPath());
    }
}
