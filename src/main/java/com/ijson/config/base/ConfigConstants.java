package com.ijson.config.base;

import com.google.common.collect.Lists;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by cuiyongxu on 18/5/19.
 */
public interface ConfigConstants {

    String profile = "local";
    Charset UTF8 = Charset.forName("UTF-8");
    Charset GBK = Charset.forName("GBK");

    String configFiles = "autoconf,log4j.properties,logback.xml,application.properties";
    String autoconf = "autoconf";
    String tmpdir = "java.io.tmpdir";
    String configPath = "config.path";
    String docker = "DOCKER";
    String machine_type = "MACHINE_TYPE";
    String osName = "os.name";
    String windows = "windows";
    String fileStartWith = "file:/";
    String process_profile = "process.profile";
    String process_name = "process.profile";
    String unknown = "unknown";
    String spring_profiles_active = "spring.profiles.active";
    String develop = "develop";
    String application_suffix = "application-";
    String before_properties = ".properties";
    String cluster_ip = "CLUSTER_IP";
    String tomcat_port = "TOMCAT_PORT";
    String process_ip = "process.ip";
    String process_port = "process.port";
    String computer_name = "COMPUTERNAME";
    String unknown_host = "UnknownHost";
    String object_name_base = "com.javamonitor:type=";
    List<String> applicationFiles = Lists.newArrayList("application-default.properties", "application.properties");

    interface ConfKeys {
        String config_enable_zookeeper = "zk.enable";
        String zookeeper_servers = "zk.servers";
        String zookeeper_authentication = "zk.auth";
        String zookeeper_authentication_type = "zk.authType";
        String zookeeper_base_path = "zk.basePath";
        String in_zookeeper = "in-zookeeper";
        String config_url = "config.url";
    }


    interface Ijson {
        String configUrl = "http://config.ijson.com/in/config/api";
    }

    interface Jmx {
        String catalina_type_server = "Catalina:type=Server";
        String jboss_system_type_server = "jboss.system:type=Server";
        String org_mortbay_jetty_type_server_id = "org.mortbay.jetty:type=server,id=0";
        String org_mortbay_jetty = "org.mortbay:jetty=default";
        String resin_type = "resin:type=Resin";
    }
}
