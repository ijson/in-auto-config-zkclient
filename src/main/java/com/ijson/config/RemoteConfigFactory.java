package com.ijson.config;

import com.ijson.config.api.IChangeableConfig;
import com.ijson.config.base.AbstractConfigFactory;
import com.ijson.config.base.ProcessInfo;
import com.ijson.config.helper.ZookeeperUtil;
import com.ijson.config.impl.RemoteConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

public class RemoteConfigFactory extends AbstractConfigFactory {
    private final ProcessInfo info;

    public RemoteConfigFactory(ProcessInfo info) {
        this.info = info;
    }

    /**
     * @return
     * @see ZookeeperUtil#getCurator()
     */
    @Deprecated
    public CuratorFramework getClient() {
        return ZookeeperUtil.getCurator();
    }

    @Deprecated
    public void setClient(CuratorFramework client) {
        ZookeeperUtil.setCurator(client);
    }

    public ProcessInfo getInfo() {
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
