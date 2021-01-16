package com.ijson.config.helper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.nio.charset.Charset;
import java.util.List;

import static com.ijson.config.base.ConfigConstants.UTF8;

/**
 * @author *
 * zookeeper工具类
 */
public class ZookeeperHelper {

    private static CuratorFramework curator = null;

    private ZookeeperHelper() {
    }

    public static void setCurator(CuratorFramework curator) {
        ZookeeperHelper.curator = curator;
    }

    public static CuratorFramework getCurator() {
        return curator;
    }

    public static String newString(byte[] data) {
        return newString(data, UTF8);
    }

    public static String newString(byte[] data, Charset charset) {
        if (data == null) {
            return null;
        }
        return new String(data, charset);
    }

    public static byte[] newBytes(String s) {
        if (s == null) {
            return null;
        }
        return s.getBytes(UTF8);
    }

    public static byte[] newBytes(String s, Charset charset) {
        if (s == null) {
            return null;
        }
        return s.getBytes(charset);
    }

    public static String ensure(CuratorFramework client, String path) {
        try {
            client.create().creatingParentContainersIfNeeded().forPath(path);
        } catch (Exception e) {
            throw new ZookeeperException("ensure(" + path + ")", e);
        }
        return path;
    }

    public static Stat exists(CuratorFramework client, String path) {
        try {
            return client.checkExists().forPath(path);
        } catch (KeeperException.NoNodeException ignored) {
        } catch (Exception e) {
            throw new ZookeeperException("exists(" + path + ")", e);
        }
        return null;
    }

    public static Stat exists(CuratorFramework client, String path, Watcher watcher) {
        try {
            return client.checkExists().usingWatcher(watcher).forPath(path);
        } catch (KeeperException.NoNodeException ignored) {
        } catch (Exception e) {
            throw new ZookeeperException("exists(" + path + ")", e);
        }
        return null;
    }

    public static void create(CuratorFramework client, String path) {
        try {
            client.create().creatingParentsIfNeeded().forPath(path);
        } catch (Exception e) {
            throw new ZookeeperException("create(" + path + ")", e);
        }
    }

    public static void create(CuratorFramework client, String path, byte[] payload) {
        try {
            client.create().creatingParentsIfNeeded().forPath(path, payload);
        } catch (Exception e) {
            throw new ZookeeperException("create(" + path + ")", e);
        }
    }

    public static void create(CuratorFramework client, String path, byte[] payload, CreateMode mode) {
        try {
            client.create().creatingParentsIfNeeded().withMode(mode).forPath(path, payload);
        } catch (Exception e) {
            throw new ZookeeperException("create(" + path + ")", e);
        }
    }

    public static void create(CuratorFramework client, String path, byte[] payload, CreateMode mode, List<ACL> aclList) {
        try {
            client.create().creatingParentsIfNeeded().withMode(mode).withACL(aclList).forPath(path, payload);
        } catch (Exception e) {
            throw new ZookeeperException("create(" + path + ")", e);
        }
    }

    public static void delete(CuratorFramework client, String path) {
        try {
            client.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (KeeperException.NoNodeException ignored) {
        } catch (Exception e) {
            throw new ZookeeperException("delete(" + path + ")", e);
        }
    }

    public static void guaranteedDelete(CuratorFramework client, String path) {
        try {
            client.delete().guaranteed().deletingChildrenIfNeeded().forPath(path);
        } catch (KeeperException.NoNodeException ignored) {
        } catch (Exception e) {
            throw new ZookeeperException("guaranteedDelete(" + path + ")", e);
        }
    }

    public static byte[] getData(CuratorFramework client, String path) {
        try {
            return client.getData().forPath(path);
        } catch (KeeperException.NoNodeException ignored) {
        } catch (Exception e) {
            throw new ZookeeperException("getData(" + path + ")", e);
        }
        return null;
    }

    public static byte[] getData(CuratorFramework client, String path, Watcher watcher) {
        try {
            return client.getData().usingWatcher(watcher).forPath(path);
        } catch (KeeperException.NoNodeException ignored) {
        } catch (Exception e) {
            throw new ZookeeperException("getData(" + path + ")", e);
        }
        return null;
    }

    public static List<String> getChildren(CuratorFramework client, String path) {
        try {
            return client.getChildren().forPath(path);
        } catch (KeeperException.NoNodeException ignored) {
        } catch (Exception e) {
            throw new ZookeeperException("getChildren(" + path + ")", e);
        }
        return null;
    }

    public static List<String> getChildren(CuratorFramework client, String path, Watcher watcher) {
        try {
            return client.getChildren().usingWatcher(watcher).forPath(path);
        } catch (KeeperException.NoNodeException ignored) {
        } catch (Exception e) {
            throw new ZookeeperException("getChildren(" + path + ")", e);
        }
        return null;
    }

    public static void setData(CuratorFramework client, String path, byte[] payload) {
        try {
            client.setData().forPath(path, payload);
        } catch (Exception e) {
            throw new ZookeeperException("setData(" + path + ")", e);
        }
    }

    public static void setDataAsync(CuratorFramework client, String path, byte[] payload) {
        try {
            client.setData().inBackground().forPath(path, payload);
        } catch (Exception e) {
            throw new ZookeeperException("setDataAsync(" + path + ")", e);
        }
    }

    public static List<ACL> getAcl(CuratorFramework client, String path) {
        try {
            return client.getACL().forPath(path);
        } catch (KeeperException.NoNodeException ignored) {
        } catch (Exception e) {
            throw new ZookeeperException("getACL(" + path + ")", e);
        }
        return null;
    }

    public static void setAcl(CuratorFramework client, String path, List<ACL> acls) {
        try {
            client.setACL().withACL(acls).forPath(path);
        } catch (KeeperException.NoNodeException ignored) {
        } catch (Exception e) {
            throw new ZookeeperException("setACL(" + path + ")", e);
        }
    }

    private static class ZookeeperException extends RuntimeException {
        ZookeeperException(String s, Exception e) {
            super("访问Zookeeper异常", e);
        }
    }

}
