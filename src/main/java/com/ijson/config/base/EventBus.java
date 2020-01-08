package com.ijson.config.base;

import com.google.common.collect.Sets;
import com.ijson.config.api.IChangeListener;
import com.ijson.config.api.IChangeable;
import com.ijson.config.api.IConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class EventBus implements IChangeable {

    public static final Logger log = LoggerFactory.getLogger(EventBus.class);

    private final Set<IChangeListener> listeners = Sets.newConcurrentHashSet();
    private final IConfig config;

    public EventBus(IConfig config) {
        this.config = config;
    }

    public void addListener(IChangeListener listener) {
        addListener(listener, true);
    }

    public void addListener(IChangeListener listener, boolean loadAfterRegister) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            if (loadAfterRegister) {
                try {
                    listener.changed(config);
                } catch (Exception e) {
                    log.error("cannot reload {} {}", config.getName(), e);
                }
            }
        }
    }

    public void removeListener(IChangeListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void notifyListeners() {
        for (IChangeListener i : listeners) {
            log.info("{} changed, notify {}", config.getName(), i);
            try {
                // 避免并发多线程加载导致冲突
                // synchronized (config) {
                i.changed(config);
                //}
            } catch (Exception e) {
                log.error("cannot reload {}  {}", config.getName(), e);
            }
        }
    }
}
