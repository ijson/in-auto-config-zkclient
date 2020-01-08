package com.ijson.config.base;

import com.ijson.config.api.IChangeListener;
import com.ijson.config.api.IChangeable;
import com.ijson.config.api.IChangeableConfig;
import com.ijson.config.util.ZookeeperUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class ChangeableConfig extends Config implements IChangeableConfig {

    public static final Logger log = LoggerFactory.getLogger(ChangeableConfig.class);

    private final String name;
    private final IChangeable eventBus;
    private String profile = ConfigConstants.profile;

    public ChangeableConfig(String name) {
        this.name = name;
        this.eventBus = new EventBus(this);
    }

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

    public void addListener(IChangeListener listener) {
        eventBus.addListener(listener);
    }

    public void addListener(IChangeListener listener, boolean loadAfterRegister) {
        eventBus.addListener(listener, loadAfterRegister);
    }

    public void removeListener(IChangeListener listener) {
        eventBus.removeListener(listener);
    }

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
        log.debug("change detecting before: {} after:{}", ZookeeperUtil.newString(old), ZookeeperUtil.newString(now));
        return !Arrays.equals(now, old);
    }
}
