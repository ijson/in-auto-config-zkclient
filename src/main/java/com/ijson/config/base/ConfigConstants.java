package com.ijson.config.base;

import com.google.common.collect.Lists;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author **
 * Created by cuiyongxu on 18/5/19.
 */
public interface ConfigConstants {

    String PROFILE = "local";
    Charset UTF8 = StandardCharsets.UTF_8;

    String CONFIG_FILES = "autoconf,log4j.properties,logback.xml,application.properties";
    String AUTO_CONF = "autoconf";
    String TMP_DIR = "java.io.tmpdir";
    String CONFIG_PATH = "config.path";
    String DOCKER = "DOCKER";
    String MACHINE_TYPE = "MACHINE_TYPE";
    String OS_NAME = "os.name";
    String WINDOWS = "windows";
    String FILE_START_WITH = "file:/";
    String PROCESS_PROFILE = "process.profile";
    String SPRING_PROFILES_ACTIVE = "spring.profiles.active";
    String DEVELOP = "develop";
    String APPLICATION_SUFFIX = "application-";
    String BEFORE_PROPERTIES = ".properties";
    String PROCESS_IP = "process.ip";
    String PROCESS_PORT = "process.port";
    String COMPUTER_NAME = "COMPUTERNAME";
    String UNKNOWN_HOST = "UnknownHost";
    List<String> APPLICATION_FILES = Lists.newArrayList("application-default.properties", "application.properties");
    Integer CACHE_FILE_SIZE = 2;
    Integer TRIES_GET_SERVER_NAME = 30;
    Character COMMA = ',';

    interface ConfKeys {
        String CONFIG_ENABLE_ZOOKEEPER = "zk.enable";
        String ZOOKEEPER_SERVERS = "zk.servers";
        String ZOOKEEPER_AUTHENTICATION = "zk.auth";
        String ZOOKEEPER_AUTHENTICATION_TYPE = "zk.authType";
        String ZOOKEEPER_BASE_PATH = "zk.basePath";
        String IN_ZOOKEEPER = "in-zookeeper";
        String CONFIG_URL = "config.url";
        String ZK_SERVER_URL = "custom.zk.server.url";
    }


    interface Ijson {
        String CONFIG_URL = "http://config.ijson.com/in/config/api";
    }

    interface Jmx {
        String CATALINA_TYPE_SERVER = "Catalina:type=Server";
        String JBOSS_SYSTEM_TYPE_SERVER = "jboss.system:type=Server";
        String ORG_MORTBAY_JETTY_TYPE_SERVER_ID = "org.mortbay.jetty:type=server,id=0";
        String ORG_MORTBAY_JETTY = "org.mortbay:jetty=default";
        String RESIN_TYPE = "resin:type=Resin";
    }
}
