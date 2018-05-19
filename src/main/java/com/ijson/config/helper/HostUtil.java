package com.ijson.config.helper;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.ijson.config.base.ConfigConstants.*;

/**
 * Created by cuiyongxu on 17/8/27.
 */
@Slf4j
public class HostUtil {

    static String getHostName() {
        if (System.getenv(computer_name) != null) {
            return System.getenv(computer_name);
        } else {
            return getHostNameForLinux();
        }
    }

    private static String getHostNameForLinux() {
        try {
            return (InetAddress.getLocalHost()).getHostName();
        } catch (UnknownHostException uhe) {
            String host = uhe.getMessage(); // host = "hostname: hostname"
            if (host != null) {
                int colon = host.indexOf(':');
                if (colon > 0) {
                    return host.substring(0, colon);
                }
            }
            return unknown_host;
        }
    }

    /**
     * <pre>
     * 判断一个IP是不是内网IP段的IP
     * 10.0.0.0 – 10.255.255.255
     * 172.16.0.0 – 172.31.255.255
     * 192.168.0.0 – 192.168.255.255
     * </pre>
     *
     * @param ip ip地址
     * @return 如果是内网返回true，否则返回false
     */
    public static boolean isInnerIP(String ip) {
        try {
            return InetAddress.getByName(ip).isSiteLocalAddress();
        } catch (UnknownHostException e) {
            log.error("cannot parse: {}", ip, e);
            return false;
        }
    }
}
