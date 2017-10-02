package com.ijson.config.api;

public interface IZkResolver {
  boolean isEnable();

  String getServer();

  String getAuth();

  String getAuthType();

  String getBasePath();

  void resolve();
}
