package com.ijson.config.impl;

import com.google.common.base.MoreObjects;
import com.ijson.config.base.ChangeableConfig;
import com.ijson.config.helper.ZookeeperHelper;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.ijson.config.helper.ZookeeperHelper.getCurator;

/**
 * @author *
 */
public class RemoteConfig extends ChangeableConfig {

    public static final Logger log = LoggerFactory.getLogger(RemoteConfig.class);

    private final String path;
    private final List<String> paths;
    private final Watcher leafWatcher = new Watcher() {
        @Override
        public void process(WatchedEvent event) {
            Event.EventType t = event.getType();
            String p = event.getPath();
            log.info("event: {}, path: {}", t, p);
            switch (t) {
                case NodeDataChanged:
                    loadFromZookeeper();
                    break;
                case NodeDeleted:
                    getCurator().clearWatcherReferences(this);
                    //loadFromZookeeper();
                    break;
                default:
                    log.warn("skip {}, {}", t, p);
            }
        }
    };
    private final Watcher baseWatcher = new Watcher() {
        @Override
        public void process(WatchedEvent event) {
            Event.EventType t = event.getType();
            String p = event.getPath();
            log.info("event: {}, path: {}", t, p);
            switch (t) {
                case NodeCreated:
                case NodeDataChanged:
                case NodeChildrenChanged:
                    loadFromZookeeper();
                    break;
                case NodeDeleted:
                    getCurator().clearWatcherReferences(this);
                    loadFromZookeeper();
                    break;
                default:
                    log.warn("skip {}, {}", t, p);
            }
        }
    };
    private final ConnectionStateListener stateListener = (client1, newState) -> {
        if (newState.equals(ConnectionState.RECONNECTED)) {
            initZookeeper();
        }
    };

    public RemoteConfig(String name, String path, List<String> paths) {
        super(name);
        this.path = path;
        this.paths = paths;
    }

    /**
     * 和zookeeper建立连接和添加watcher
     */
    void initZookeeper() {
        try {
            getCurator().getConnectionStateListenable().addListener(stateListener);
            if (!getCurator().getZookeeperClient().isConnected()) {
                log.info("try connect zookeeper, name: {}", getName());
                getCurator().blockUntilConnected();
            }
            if (ZookeeperHelper.exists(getCurator(), path, baseWatcher) != null) {
                loadFromZookeeper();
            }
        } catch (InterruptedException e) {
            log.error("cannot init '{}', path:{} {}", getName(), path, e);
        }
    }

    public void loadAndWatchChanges() {
        initZookeeper();
    }

    protected void loadFromZookeeper() {
        log.info("{}, path:{}, order:{}", getName(), path, paths);
        List<String> children = ZookeeperHelper.getChildren(getCurator(), path, baseWatcher);
        boolean found = false;
        //按照特定顺序逐个查找配置
        if (children != null && !children.isEmpty()) {
            log.info("path:{}, children:{}", path, children);
            for (String i : paths) {
                if (!children.contains(i)) {
                    continue;
                }
                // 设置config实际使用的profile
                setProfile(i);
                String p = ZKPaths.makePath(path, i);
                try {
                    byte[] content = ZookeeperHelper.getData(getCurator(), p, leafWatcher);
                    if (content != null && content.length > 0) {
                        log.info("{}, load from path:{}", getName(), p);
                        reload(content);
                        found = true;
                        break;
                    }
                } catch (Exception e) {
                    log.error("cannot load {} from zookeeper, path{}  {}", getName(), path, e);
                }
            }
        } else if (ZookeeperHelper.exists(getCurator(), path) != null) {
            byte[] content = ZookeeperHelper.getData(getCurator(), path, baseWatcher);
            if (content != null && content.length > 0) {
                reload(content);
                found = true;
            }
        }
        if (!found) {
            ZookeeperHelper.exists(getCurator(), path, baseWatcher);
            log.warn("cannot find {} in zookeeper, path: {}", getName(), path);
            reload(new byte[0]);
        }
    }

    protected void reload(byte[] content) {
        //只有真正发生变化的时候才触发重新加载
        if (isChanged(content)) {
            copyOf(content);
            notifyListeners();
        }
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("name", getName()).add("path", path).toString();
    }
}
