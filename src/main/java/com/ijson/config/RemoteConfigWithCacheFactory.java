package com.ijson.config;

import com.ijson.config.api.IChangeableConfig;
import com.ijson.config.base.ProcessInfo;
import com.ijson.config.impl.RemoteConfigWithCache;
import org.apache.curator.utils.ZKPaths;

import java.io.File;
import java.nio.file.Path;

public class RemoteConfigWithCacheFactory extends RemoteConfigFactory {
    private final Path path;

    public RemoteConfigWithCacheFactory(Path localConfigPath, ProcessInfo info) {
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
