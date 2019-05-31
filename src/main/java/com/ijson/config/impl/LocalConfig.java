package com.ijson.config.impl;

import com.google.common.io.Files;
import com.ijson.config.base.ChangeableConfig;
import com.ijson.config.helper.ILogger;

import java.io.IOException;
import java.nio.file.Path;

public class LocalConfig extends ChangeableConfig {

    private static ILogger log = ILogger.getLogger(LocalConfig.class);
    private final Path path;

    public LocalConfig(String name, Path path) {
        super(name);
        this.path = path;
        try {
            if (path.toFile().exists()) {
                copyOf(Files.toByteArray(path.toFile()));
            }
        } catch (IOException e) {
            copyOf(new byte[0]);
            log.error("configName={0}, path={1}  exception: {2}", name, path, e);
        }
    }

    public Path getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "LocalConfig{" + "name=" + getName() + ", path=" + path + '}';
    }
}
