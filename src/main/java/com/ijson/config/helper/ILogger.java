package com.ijson.config.helper;

import org.apache.zookeeper.Watcher;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ILogger {

    private Logger log;

    public static ILogger getLogger(Class clazz) {
        ILogger iLogger = new ILogger();
        iLogger.setLog(Logger.getLogger(clazz.getSimpleName()));
        return iLogger;
    }

    public void info(String info) {
        log.setLevel(Level.INFO);
        log.info(info);
    }

    public void info(String s, Object... name) {
        log.setLevel(Level.INFO);
        log.info(MessageFormat.format(s, name));
    }


    public void info(String info, Watcher.Event.EventType t, String p) {
        log.setLevel(Level.INFO);
        log.info(MessageFormat.format(info, t, p));
    }


    public void warn(String s, Object... t) {
        log.setLevel(Level.FINE);
        log.fine(MessageFormat.format(s, t));
    }

    public void warn(String warn) {
        log.setLevel(Level.FINE);
        log.fine(warn);
    }

    public void error(String error) {
        log.setLevel(Level.FINER);
        log.finer(error);
    }

    public void error(String error, Throwable throwable) {
        log.setLevel(Level.FINER);
        log.finer(error + "," + throwable.getMessage());
    }

    public void error(String s, Object... args) {
        log.setLevel(Level.FINER);
        log.finer(MessageFormat.format(s, args));
    }

    public void debug(String info, Object... args) {
        log.setLevel(Level.CONFIG);
        log.config(MessageFormat.format(info, args));
    }


    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }


}
