package com.ijson.config.base;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ijson.config.api.IChangeListener;
import com.ijson.config.api.IChangeableConfig;
import com.ijson.config.api.IConfigFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * @author cuiyongxu
 */
@SuppressWarnings({"ALL", "AlibabaAbstractMethodOrInterfaceMethodMustUseJavadoc"})
public abstract class AbstractConfigFactory implements IConfigFactory {
    private final ConcurrentMap<String, IChangeableConfig> m = Maps.newConcurrentMap();

    @Override
    public IChangeableConfig getConfig(String name) {
        IChangeableConfig config = m.get(name);
        if (config == null) {
            synchronized (this) {
                config = m.get(name);
                if (config == null) {
                    config = newConfig(name);
                    IChangeableConfig real = m.putIfAbsent(name, config);
                    if (real != null) {
                        config = real;
                    }
                }
            }
        }
        return config;
    }

    @Override
    public IChangeableConfig getConfig(String name, IChangeListener listener) {
        return getConfig(name, listener, true);
    }

    @Override
    public IChangeableConfig getConfig(String name, IChangeListener listener, boolean loadAfterRegister) {
        IChangeableConfig config = getConfig(name);
        config.addListener(listener, loadAfterRegister);
        return config;
    }

    @Override
    public boolean hasConfig(String name) {
        return m.containsKey(name);
    }

    private IChangeableConfig newConfig(String name) {
        CharMatcher matcher = CharMatcher.anyOf(",; |");
        if (matcher.matchesAnyOf(name)) {
            List<String> names = Splitter.on(matcher).trimResults().omitEmptyStrings().splitToList(name);
            List<IChangeableConfig> list = Lists.newArrayList();
            list.addAll(names.stream().map(this::getConfig).collect(Collectors.toList()));
            return new MergedConfig(list);
        } else {
            return doCreate(name);
        }
    }

    protected abstract IChangeableConfig doCreate(String name);

    @Override
    public List<IChangeableConfig> getAllConfig() {
        return ImmutableList.copyOf(m.values());
    }


    private static class MergedConfig extends ChangeableConfig implements IChangeableConfig {
        private final List<IChangeableConfig> configs;

        MergedConfig(List<IChangeableConfig> configs) {
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
}
