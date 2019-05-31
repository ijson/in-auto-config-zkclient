package com.ijson.config.helper;


import javax.management.AttributeNotFoundException;
import javax.management.ObjectName;
import java.util.Collection;

import static com.ijson.config.base.ConfigConstants.Jmx.*;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Thread.sleep;


public class WebServer {

    private static ILogger log = ILogger.getLogger(WebServer.class);

    public static boolean isTomcat() {
        return JmxHelper.mbeanExists(catalina_type_server);
    }

    public static boolean isJBoss() {
        return JmxHelper.mbeanExists(jboss_system_type_server);
    }

    public static boolean isJetty() {
        return JmxHelper.mbeanExists(org_mortbay_jetty_type_server_id);
    }

    public static boolean isJetty5() {
        return JmxHelper.mbeanExists(org_mortbay_jetty);
    }

    public static boolean isResin() {
        return JmxHelper.mbeanExists(resin_type);
    }

    public static boolean isGlassfish() {
        return JmxHelper.mbeanExists("amx:pp=,type=domain-root") ||
                JmxHelper.mbeanExists("com.sun.appserv:j2eeType=J2EEServer,category=runtime,*");
    }

    public static Integer getTomcatHttpPort() throws Exception {
        Collection<ObjectName> connectors = getSelectors("*:type=Connector,*");

        int lowest = MAX_VALUE;
        for (final ObjectName connector : connectors) {
            try {
                String protocol = JmxHelper.queryString(connector, "protocol");
                Integer port = JmxHelper.queryInt(connector, "port");
                log.info("tomcat, protocol={0}, port={1}", protocol, port);
                if (protocol != null && protocol.toLowerCase().contains("http")) {
                    if (port == null) {
                        continue;
                    }
                    lowest = Math.min(lowest, port);
                }
            } catch (AttributeNotFoundException e) {
                // quietly skip this connector, it's probably the wrong kind
            }
        }

        // maybe there are no HTTP connectors?
        if (lowest == MAX_VALUE) {
            for (final ObjectName connector : connectors) {
                try {
                    lowest = Math.min(lowest, JmxHelper.queryInt(connector, "port"));
                } catch (AttributeNotFoundException e) {
                    // quietly skip this connector, it's probably the wrong kind
                }
            }
        }

        if (lowest == MAX_VALUE) {
            return null;
        }

        return lowest;
    }

    public static Integer getResinHttpPort() throws Exception {
        Collection<ObjectName> selectors = getSelectors("resin:type=Port,*");
        int highestHttp = -1, highestOther = -1;
        for (final ObjectName selector : selectors) {
            final Integer port = JmxHelper.queryInt(selector, "Port");
            if (port == null) {
                continue;
            }
            String protocol = JmxHelper.queryString(selector, "ProtocolName");
            String address = JmxHelper.queryString(selector, "Address");
            log.info("resin, protocol={0} address={1} port={2}", protocol, address, port);
            if (protocol == null || !"http".equalsIgnoreCase(protocol.trim())) {
                highestOther = max(highestOther, port);
            } else {
                highestHttp = max(highestHttp, port);
            }
        }

        // maybe there are no HTTP connectors?
        if (highestHttp != -1) {
            return highestHttp;
        } else if (highestOther != -1) {
            return highestOther;
        } else {
            return null;
        }
    }

    public static Integer getJettyHttpPort() throws Exception {
        Collection<ObjectName> selectors = getSelectors("org.mortbay.jetty.nio:type=selectchannelconnector,*");
        int lowest = MAX_VALUE;
        for (final ObjectName selector : selectors) {
            lowest = Math.min(lowest, JmxHelper.queryInt(selector, "port"));
        }

        if (lowest == MAX_VALUE) {
            return null;
        }

        return lowest;
    }

    public static Integer getJetty5HttpPort() throws Exception {
        Collection<ObjectName> selectors = getSelectors("org.mortbay:jetty=default,*");
        int lowest = MAX_VALUE;
        for (final ObjectName selector : selectors) {
            if (selector.toString().matches("org.mortbay:jetty=default,SocketListener=[0=9]")) {
                lowest = Math.min(lowest, JmxHelper.queryInt(selector, "port"));
            }
        }

        if (lowest == MAX_VALUE) {
            return null;
        }

        return lowest;
    }

    public static Integer getGlassfishHttpPort() throws Exception {
        Collection<ObjectName> selectors = getSelectors("com.sun.appserv:type=Selector,*");
        int lowest = MAX_VALUE;
        for (final ObjectName selector : selectors) {
            final String name = selector.toString();
            if (name.contains("http")) {
                lowest = min(lowest, parseInt(name.replaceAll("[^0-9]", "")));
            }
        }

        // maybe there are no HTTP connectors?
        if (lowest == MAX_VALUE) {
            for (final ObjectName selector : selectors) {
                final String name = selector.toString();
                lowest = min(lowest, parseInt(name.replaceAll("[^0-9]", "")));
            }
        }

        if (lowest == MAX_VALUE) {
            return null;
        }
        return lowest;
    }

    public static Collection<ObjectName> getSelectors(String name) throws Exception {
        Collection<ObjectName> selectors;
        for (int tries = 1; tries <= 30; tries++) {
            selectors = JmxHelper.queryNames(name);
            if (selectors.isEmpty()) {
                sleep(1000L);
                log.info("query({0}), try {1} times, waiting {2} seconds {3}", name, tries, tries);
            } else {
                return selectors;
            }
        }
        throw new IllegalStateException(name + ", selector MBeans were not loaded after 30 seconds, aborting");
    }

    /**
     * 根据协议和scheme获取服务端口号
     *
     * @return 端口号
     * @throws Exception 识别端口过程中可能出现的异常
     */
    public static Integer getHttpPort() throws Exception {
        if (WebServer.isTomcat() || WebServer.isJBoss()) {
            return WebServer.getTomcatHttpPort();
        }
        if (WebServer.isResin()) {
            return WebServer.getResinHttpPort();
        }
        if (WebServer.isJetty()) {
            return WebServer.getResinHttpPort();
        }
        if (WebServer.isJetty5()) {
            return WebServer.getJetty5HttpPort();
        }
        if (WebServer.isGlassfish()) {
            return WebServer.getGlassfishHttpPort();
        }
        log.error("cannot detect ContainerType");
        return null;
    }
}
