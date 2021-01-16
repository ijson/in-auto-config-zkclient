package com.ijson.config.api;

/**
 * @author *
 * Created by cuiyongxu on 17/8/26.
 */
public interface IChangeableConfig extends IConfig, IChangeable {
    /**
     * 配置文件名
     *
     * @return 配置文件名
     */
    @Override
    String getName();

    /**
     * 配置组名
     *
     * @return 配置文件名
     */
    String getProfile();

}
