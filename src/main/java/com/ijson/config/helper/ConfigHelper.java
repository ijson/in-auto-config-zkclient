package com.ijson.config.helper;

import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ijson.config.base.Config;
import com.ijson.config.base.ConfigConstants;
import com.ijson.config.base.ProcessInfo;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.ijson.config.base.ConfigConstants.*;

/**
 * @author c
 */
public class ConfigHelper {

    public static final Logger log = LoggerFactory.getLogger(ConfigHelper.class);

    public static ThreadPoolExecutor EXECUTOR =
            new ThreadPoolExecutor(0, 1, 5, TimeUnit.MINUTES, new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setDaemon(true)
                    .setNameFormat("config-load-%d")
                    .build());

    private ConfigHelper() {
    }

    public static Path getConfigPath() {
        return LazyHolder2.CONFIG_PATH;
    }

    public static Config getApplicationConfig() {
        return LazyHolder3.CONFIG;
    }

    private static String getServerInnerIp() {
        if (DOCKER.equals(System.getenv(MACHINE_TYPE))) {
            try {
                return getHostName();
            } catch (Exception e) {
                return "";
            }
        }
        return null;
    }

    private static String getHostName() {
        if (System.getenv(COMPUTER_NAME) != null) {
            return System.getenv(COMPUTER_NAME);
        } else {
            return getHostNameForLinux();
        }
    }

    private static String getHostNameForLinux() {
        try {
            return (InetAddress.getLocalHost()).getHostName();
        } catch (UnknownHostException uhe) {
            /**
             * host = "hostname: hostname"
             */
            String host = uhe.getMessage();
            if (host != null) {
                int colon = host.indexOf(':');
                if (colon > 0) {
                    return host.substring(0, colon);
                }
            }
            return UNKNOWN_HOST;
        }
    }

    public static ProcessInfo getProcessInfo() {
        return LazyHolder4.PROCESS_INFO;
    }

    /**
     * <pre>
     * 1.扫描配置参数 config.path
     * 2.扫描类路径下的 autoconf 目录
     * 3.如果找不到就用java.io.tmpdir
     * </pre>
     *
     * @return factory
     */
    private static Path scanConfigPath() {
        Path basePath = scanProperty();
        if (basePath != null) {
            return basePath;
        }
        //查找若干文件以便找到classes根目录
        String files = ConfigConstants.CONFIG_FILES;
        for (String i : Splitter.on(COMMA).split(files)) {
            String s = scanResource(i);
            if (s != null) {
                basePath = new File(s).toPath().getParent().resolve(ConfigConstants.AUTO_CONF);
                File root = basePath.toFile();
                if (root.exists() || root.mkdir()) {
                    return basePath;
                }
            }
        }
        return new File(System.getProperty(ConfigConstants.TMP_DIR)).toPath();
    }

    /**
     * 看是否通过环境变量指明了本地文件cache的路径
     */
    private static Path scanProperty() {
        String localCachePath = System.getProperty(ConfigConstants.CONFIG_PATH);
        if (!Strings.isNullOrEmpty(localCachePath)) {
            File f = new File(localCachePath);
            f.mkdirs();
            return f.toPath();
        }
        return null;
    }

    /**
     * 在类路径下查找资源
     *
     * @param resource 资源名
     * @return 找到返回路径否则返回null
     */
    private static String scanResource(String resource) {
        try {
            Enumeration<URL> ps = Thread.currentThread().getContextClassLoader().getResources(resource);
            while (ps.hasMoreElements()) {
                URL url = ps.nextElement();
                String s = url.toString();
                if (s.startsWith(FILE_START_WITH)) {
                    String os = System.getProperty(OS_NAME);
                    if (os != null && os.toLowerCase().contains(WINDOWS)) {
                        return s.substring(6);
                    } else {
                        return s.substring(5);
                    }
                }
            }
        } catch (IOException e) {
            log.error("cannot find {} under classpath {}", resource, e);
        }
        return null;
    }

    /**
     * 扫描配置根目录或者类路径下的application.properties文件并解析
     *
     * @return 加载的配置信息
     */
    private static Config scanApplicationConfig() {
        List<String> names = APPLICATION_FILES;
        String envProfile = System.getProperty(PROCESS_PROFILE);
        String springProfile = System.getProperty(SPRING_PROFILES_ACTIVE, DEVELOP);
        String profile = MoreObjects.firstNonNull(envProfile, springProfile);
        if (!Strings.isNullOrEmpty(profile)) {
            String name = APPLICATION_SUFFIX + profile + BEFORE_PROPERTIES;
            if (!names.contains(name)) {
                names.add(0, name);
            }
        }

        Config fileConfig = new Config();
        // 扫描类路径下的配置文件
        for (String i : names) {
            String path = scanResource(i);
            if (path != null) {
                try {
                    log.info("load applicationConfig from {}", path);
                    fileConfig.copyOf(Files.readAllBytes(Paths.get(path)));
                    break;
                } catch (IOException e) {
                    log.error("cannot load from {} {}", path, e);
                }
            }
        }
        Map<String, String> defaults = Maps.newHashMap();
        defaults.put(PROCESS_PROFILE, profile);
        Map<String, String> props = Maps.newHashMap();
        Set<String> keys = System.getProperties().stringPropertyNames();
        for (String key : keys) {
            // 避免windows环境下,路径反斜线导致解析失败
            String val = System.getProperty(key);
            if (val.indexOf('\\') < 0) {
                props.put(key, val);
            }
        }
        // 以配置文件中配置的profile为准
        String fileProfile = fileConfig.get(PROCESS_PROFILE);
        if (!Strings.isNullOrEmpty(fileProfile) && Strings.isNullOrEmpty(envProfile)) {
            props.put(PROCESS_PROFILE, fileProfile);
        }
        Config c = new Config();
        //查找顺序:系统默认 < 文件配置 < 环境变量配置
        c.putAll(defaults).putAll(fileConfig.getAll()).putAll(props);
        return c;
    }

    private static String get(Config config, String key, String defVal) {
        String val = System.getProperty(key);
        if (val != null) {
            return val;
        }
        val = config.get(key);
        if (val != null) {
            return val;
        }
        return defVal;
    }

    private static ProcessInfo scanProcessInfo() {
        Config config = getApplicationConfig();
        ProcessInfo info = new ProcessInfo();
        info.setName(config.get(PROCESS_PROFILE));
        info.setProfile(config.get(PROCESS_PROFILE));
        info.setIp(config.get(PROCESS_IP, getServerInnerIp()));
        String s = get(config, PROCESS_PORT, null);
        if (Strings.isNullOrEmpty(s)) {
            try {
                Integer port = WebServer.getHttpPort();
                if (port != null) {
                    info.setPort(port.toString());
                }
            } catch (Exception ignored) {
            }
        } else {
            info.setPort(s);
        }
        log.info("process.name = {}", info.getName());
        log.info("process.profile = {}", info.getProfile());
        log.info("process.ip = {}", info.getIp());
        log.info("process.port = {}", info.getPort());
        return info;
    }

    public static CuratorFramework newClient(String connectString) throws InterruptedException {
        return newClient(connectString, null, null);
    }

    public static CuratorFramework newClient(String connectString,
                                             String scheme,
                                             String password) throws InterruptedException {
        log.info("zookeeper.servers={}", connectString);
        RetryPolicy policy = new BoundedExponentialBackoffRetry(1000, 60000, 25);
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();
        builder.connectString(connectString).connectionTimeoutMs(8000).sessionTimeoutMs(60000).retryPolicy(policy);
        if (!Strings.isNullOrEmpty(scheme)) {
            builder.authorization(scheme, newBytes(password));
        }
        CuratorFramework client = builder.build();
        client.start();
        return client;
    }


    private static class LazyHolder2 {
        private static final Path CONFIG_PATH = scanConfigPath();
    }


    private static class LazyHolder3 {
        private static final Config CONFIG = scanApplicationConfig();
    }

    private static byte[] newBytes(String s) {
        if (s == null) {
            return null;
        }
        return s.getBytes(UTF8);
    }

    private static class LazyHolder4 {
        private static final ProcessInfo PROCESS_INFO = scanProcessInfo();
    }
}
