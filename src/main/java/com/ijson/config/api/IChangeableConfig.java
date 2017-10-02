package com.ijson.config.api;

/**
 * Created by cuiyongxu on 17/8/26.
 */
public interface IChangeableConfig extends IConfig, IChangeable {
    /**
     * 配置文件名
     *
     * @return 配置文件名
     */
    String getName();

    /**
     * 配置组名
     *
     * @return 配置文件名
     */
    String getProfile();

}
