package com.ijson.config.impl;

import com.google.common.base.MoreObjects;
import com.google.common.io.Files;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.Uninterruptibles;
import com.ijson.config.helper.FileUpdateWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.ijson.config.base.ConfigConstants.CACHE_FILE_SIZE;

/**
 * @author *
 */
public class RemoteConfigWithCache extends RemoteConfig {


    public static final Logger log = LoggerFactory.getLogger(RemoteConfigWithCache.class);

    private static final ThreadFactory FACTORY =
            new ThreadFactoryBuilder().setDaemon(true).setNameFormat("config-load-%d").build();
    private final File cacheFile;
    /**
     * 延迟加载远程配置初始值,避免加载配置影响启动
     */
    private boolean loadedFromZookeeper = false;

    public RemoteConfigWithCache(String name, String basePath, List<String> paths, File cacheFile) {
        super(name, basePath, paths);
        this.cacheFile = cacheFile;
    }

    public File getCacheFile() {
        return cacheFile;
    }

    @Override
    public void loadAndWatchChanges() {
        //有本地配置就先从本地加载
        if (cacheFile.exists() && cacheFile.length() > CACHE_FILE_SIZE) {
            try {
                copyOf(Files.toByteArray(cacheFile));
                FACTORY.newThread(() -> {
                    //延迟加载zookeeper上的配置,避免服务启动过慢
                    Uninterruptibles.sleepUninterruptibly(15, TimeUnit.SECONDS);
                    initZookeeper();
                }).start();
            } catch (IOException e) {
                log.error("cannot read {}", cacheFile);
                initZookeeper();
            }
        } else {
            //本地没有则直接从zookeeper加载
            initZookeeper();
        }
        //注册本地配置变更通知回调
        FileUpdateWatcher.getInstance().watch(cacheFile.toPath(), (path, content) -> {
            log.info("local change: {}", path);
            refresh(content);
        });
    }

    private void refresh(byte[] content) {
        if (isChanged(content) && content != null && content.length > 0) {
            copyOf(content);
            try {
                //已经加载过,就不要再通过本地文件修改通知再加载1次了
                FileUpdateWatcher.getInstance().mask(cacheFile.toPath());
                Files.write(content, cacheFile);
            } catch (IOException e) {
                log.error("cannot write {}", cacheFile);
            }
            notifyListeners();
        }
    }

    @Override
    protected void reload(byte[] content) {
        //避免首次启动,远程配置不存在反而覆盖了本地配置
        boolean contentEmpty = content == null || content.length == 0;
        if (contentEmpty && !loadedFromZookeeper) {
            log.warn("{} deleted, wont clean local for safety", getPath());
            return;
        }
        loadedFromZookeeper = true;
        refresh(content);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", getName())
                .add("cacheFile", cacheFile)
                .add("zkPath", getPath())
                .toString();
    }
}
