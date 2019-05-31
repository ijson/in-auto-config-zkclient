package com.ijson.config.impl;

import com.google.common.base.MoreObjects;
import com.ijson.config.base.ChangeableConfig;
import com.ijson.config.helper.ILogger;
import com.ijson.config.util.ZookeeperUtil;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.util.List;

import static com.ijson.config.util.ZookeeperUtil.getCurator;

public class RemoteConfig extends ChangeableConfig {

    private static ILogger log = ILogger.getLogger(RemoteConfig.class);

    private final String path;
    private final List<String> paths;
    private final Watcher leafWatcher = new Watcher() {
        @Override
        public void process(WatchedEvent event) {
            Event.EventType t = event.getType();
            String p = event.getPath();
            log.info("event: {0}, path: {1}", t, p);
            switch (t) {
                case NodeDataChanged:
                    loadFromZookeeper();
                    break;
                case NodeDeleted:
                    getCurator().clearWatcherReferences(this);
                    //loadFromZookeeper();
                    break;
                default:
                    log.warn("skip {0}, {1}", t, p);
            }
        }
    };
    private final Watcher baseWatcher = new Watcher() {
        public void process(WatchedEvent event) {
            Event.EventType t = event.getType();
            String p = event.getPath();
            log.info("event: {0}, path: {1}", t, p);
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
                    log.warn("skip {0}, {1}", t, p);
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
                log.info("try connect zookeeper, name: {0}", getName());
                getCurator().blockUntilConnected();
            }
            if (ZookeeperUtil.exists(getCurator(), path, baseWatcher) != null) {
                loadFromZookeeper();
            }
        } catch (InterruptedException e) {
            log.error("cannot init '{0}', path:{1} {2}", getName(), path, e);
        }
    }

    public void loadAndWatchChanges() {
        initZookeeper();
    }

    protected void loadFromZookeeper() {
        log.info("{0}, path:{1}, order:{2}", getName(), path, paths);
        List<String> children = ZookeeperUtil.getChildren(getCurator(), path, baseWatcher);
        boolean found = false;
        //按照特定顺序逐个查找配置
        if (children != null && !children.isEmpty()) {
            log.info("path:{0}, children:{1}", path, children);
            for (String i : paths) {
                if (!children.contains(i)) {
                    continue;
                }
                // 设置config实际使用的profile
                setProfile(i);
                String p = ZKPaths.makePath(path, i);
                try {
                    byte[] content = ZookeeperUtil.getData(getCurator(), p, leafWatcher);
                    if (content != null && content.length > 0) {
                        log.info("{0}, load from path:{1}", getName(), p);
                        reload(content);
                        found = true;
                        break;
                    }
                } catch (Exception e) {
                    log.error("cannot load {0} from zookeeper, path{1}  {2}", getName(), path, e);
                }
            }
        } else if (ZookeeperUtil.exists(getCurator(), path) != null) {
            byte[] content = ZookeeperUtil.getData(getCurator(), path, baseWatcher);
            if (content != null && content.length > 0) {
                reload(content);
                found = true;
            }
        }
        if (!found) {
            ZookeeperUtil.exists(getCurator(), path, baseWatcher);
            log.warn("cannot find {0} in zookeeper, path: {1}", getName(), path);
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
