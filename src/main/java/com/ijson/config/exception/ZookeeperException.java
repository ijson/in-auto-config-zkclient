package com.ijson.config.exception;

/**
 * 访问Zookeeper出错
 */
public class ZookeeperException extends RuntimeException {
    public ZookeeperException(String s, Exception e) {
        super("访问Zookeeper异常", e);
    }
}
