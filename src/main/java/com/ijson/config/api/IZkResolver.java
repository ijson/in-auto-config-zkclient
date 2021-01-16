package com.ijson.config.api;

/**
 * @author *
 */
public interface IZkResolver {
    /**
     *  zk是否启用
     * @return 是否启用
     */
    boolean isEnable();

    /**
     * 获取zk server
     * @return zk server
     */
    String getServer();

    /**
     * 获取zk权限
     * @return  zk权限
     */
    String getAuth();

    /**
     * 获取zk权限类型
     * @return zk权限类型
     */
    String getAuthType();

    /**
     * 获取存储路径
     * @return 存储路径
     */
    String getBasePath();

    /**
     * 加载配置信息
     */
    void resolve();
}
