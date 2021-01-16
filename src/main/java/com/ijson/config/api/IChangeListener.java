package com.ijson.config.api;

/**
 * @author *
 */
public interface IChangeListener {
    /**
     * 配置更新，回调注册的功能实现对应功能变更
     *
     * @param config 配置文件
     */
    void changed(IConfig config);
}
