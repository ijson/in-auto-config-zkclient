package com.ijson.config.impl;

import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ijson.config.api.IChangeListener;
import com.ijson.config.api.IChangeableConfig;
import com.ijson.config.base.ChangeableConfig;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MergedConfig extends ChangeableConfig implements IChangeableConfig {
    private final List<IChangeableConfig> configs;

    public MergedConfig(List<IChangeableConfig> configs) {
        super(Joiner.on(',').join(Collections2.transform(configs, IChangeableConfig::getName)));

        IChangeListener listener = config -> merge();

        // 注册单个配置文件的更新回调功能
        for (IChangeableConfig c : configs) {
            c.addListener(listener, false);
        }

        // 同名配置，排在前面的优先，所以按照做一次排序反转
        this.configs = Lists.newArrayList(configs);
        Collections.reverse(this.configs);

        // 首次merge配置
        merge();
    }

    private void merge() {
        Map<String, String> m = Maps.newHashMap();
        for (IChangeableConfig c : this.configs) {
            m.putAll(c.getAll());
        }
        copyOf(m);
        notifyListeners();
    }

    @Override
    public String toString() {
        return "MergedConfig{" + "name=" + getName() + '}';
    }
}
