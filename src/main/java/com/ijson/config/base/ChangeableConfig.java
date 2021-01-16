package com.ijson.config.base;

import com.google.common.collect.Sets;
import com.ijson.config.api.IChangeListener;
import com.ijson.config.api.IChangeable;
import com.ijson.config.api.IChangeableConfig;
import com.ijson.config.api.IConfig;
import com.ijson.config.helper.ZookeeperHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Set;

/**
 * @author cuiyongxu
 */
public class ChangeableConfig extends Config implements IChangeableConfig {

    public static final Logger log = LoggerFactory.getLogger(ChangeableConfig.class);

    private final String name;
    private final IChangeable eventBus;
    private String profile = ConfigConstants.PROFILE;

    public ChangeableConfig(String name) {
        this.name = name;
        this.eventBus = new EventBus(this);
    }

    @Override
    public String getProfile() {
        return profile;
    }

    protected void setProfile(String profile) {
        this.profile = profile;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addListener(IChangeListener listener) {
        eventBus.addListener(listener);
    }

    @Override
    public void addListener(IChangeListener listener, boolean loadAfterRegister) {
        eventBus.addListener(listener, loadAfterRegister);
    }

    @Override
    public void removeListener(IChangeListener listener) {
        eventBus.removeListener(listener);
    }

    @Override
    public void notifyListeners() {
        eventBus.notifyListeners();
    }

    /**
     * 判断新接收到的数据和以前相比是否发生了变化
     *
     * @param now 新数据
     * @return 逐字节对比，不一样就返回true
     */
    public boolean isChanged(byte[] now) {
        if (now == null) {
            return true;
        }
        byte[] old = getContent();
        log.debug("change detecting before: {} after:{}", ZookeeperHelper.newString(old), ZookeeperHelper.newString(now));
        return !Arrays.equals(now, old);
    }


    private static class EventBus implements IChangeable {


        private final Set<IChangeListener> listeners = Sets.newConcurrentHashSet();
        private final IConfig config;

        public EventBus(IConfig config) {
            this.config = config;
        }

        @Override
        public void addListener(IChangeListener listener) {
            addListener(listener, true);
        }

        @Override
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

        @Override
        public void removeListener(IChangeListener listener) {
            if (listener != null) {
                listeners.remove(listener);
            }
        }

        @Override
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

}
