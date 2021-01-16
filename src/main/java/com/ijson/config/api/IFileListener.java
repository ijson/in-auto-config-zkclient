package com.ijson.config.api;

import java.nio.file.Path;

/**
 * @author *
 */
public interface IFileListener {
    /**
     * 文件修改通知
     *
     * @param path    文件路径
     * @param content 文件内容
     */
    void changed(Path path, byte[] content);
}
