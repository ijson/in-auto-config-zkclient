package com.ijson.config.helper;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;

public class JmxHelper {

    private static MBeanServer findBeanServer(final ObjectName objectName) {
        final List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
        for (MBeanServer server : servers) {
            try {
                if (server.getObjectInstance(objectName) != null) {
                    return server;
                }
            } catch (InstanceNotFoundException e) {
                // woops, not registered here...
            }
        }

        // oh well, most likely it is here then...
        return getPlatformMBeanServer();
    }

    /**
     * Check that an mbean exists.
     *
     * @param objectName The bean to check for.
     * @return <code>true</code> if the bean exists, or <code>false</code>
     * otherwise.
     */
    static boolean mbeanExists(final String objectName) {
        try {
            final ObjectName name = new ObjectName(objectName);
            return findBeanServer(name).isRegistered(name);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Register a new mbean in the platform mbean server.
     *
     * @param mbean      The mbean to register.
     * @param objectName The object name to register it under.
     * @throws Exception When there was a problem registering MBeans.
     */
    public static void register(final Object mbean, final String objectName) throws Exception {
        unregister(objectName);

        getPlatformMBeanServer().registerMBean(mbean, new ObjectName(objectName));
    }

    /**
     * Query for a string, based on the object name as string. Convenience
     * method that does the casts.
     *
     * @param objectName The object name to query.
     * @param attribute  The attribute to query.
     * @return The value of the attribute, as string.
     * @throws Exception When there was a problem querying.
     */
    public static String queryString(final String objectName, final String attribute) throws Exception {
        final Object value = query(objectName, attribute);
        return value == null ? null : value.toString();
    }

    /**
     * Query for a string, based on the object name. Convenience method that
     * does the casts.
     *
     * @param objectName The object name to query.
     * @param attribute  The attribute to query.
     * @return The value of the attribute, as string.
     * @throws Exception When there was a problem querying.
     */
    static String queryString(final ObjectName objectName, final String attribute) throws Exception {
        final Object value = query(objectName, attribute);
        return value == null ? null : value.toString();
    }

    /**
     * Query for an integer, based on the object name. Convenience method that
     * does the casts.
     *
     * @param objectName The object name to query.
     * @param attribute  The attribute to query.
     * @return The value of the attribute, as string.
     * @throws Exception When there was a problem querying.
     */
    public static Integer queryInt(final String objectName, final String attribute) throws Exception {
        final Object value = query(objectName, attribute);
        return value == null ? null : parseInt(value.toString());
    }


    /**
     * Query for an integer, based on the object name. Convenience method that
     * does the casts.
     *
     * @param objectName The object name to query.
     * @param attribute  The attribute to query.
     * @return The value of the attribute, as string.
     * @throws Exception When there was a problem querying.
     */
    static Integer queryInt(final ObjectName objectName, final String attribute) throws Exception {
        final Object value = query(objectName, attribute);
        return value == null ? null : parseInt(value.toString());
    }

    /**
     * Query for a long, based on the object name. Convenience method that does
     * the casts.
     *
     * @param objectName The object name to query.
     * @param attribute  The attribute to query.
     * @return The value of the attribute, as string.
     * @throws Exception When there was a problem querying.
     */
    public static Long queryLong(final ObjectName objectName, final String attribute) throws Exception {
        final Object value = query(objectName, attribute);
        return value == null ? null : parseLong(value.toString());
    }

    /**
     * Query for a value, based on the object name as string. Convenience method
     * that does the casts.
     *
     * @param objectName The object name to query.
     * @param attribute  The attribute to query.
     * @return The value of the attribute, as string.
     * @throws Exception When there was a problem querying.
     */
    private static Object query(final String objectName, final String attribute) throws Exception {
        return query(new ObjectName(objectName), attribute);
    }

    /**
     * Query a JMX attribute.
     *
     * @param objectName The name of the mbean to query.
     * @param attribute  The attribute to query on that mbean.
     * @return The value of the attribute on the named object.
     * @throws Exception When there was a problem querying.
     */
    private static Object query(final ObjectName objectName, final String attribute) throws Exception {
        final int dot = attribute.indexOf('.');
        if (dot < 0) {
            return findBeanServer(objectName).getAttribute(objectName, attribute);
        }

        return resolveFields((CompositeData) findBeanServer(objectName).getAttribute(objectName, attribute.substring(0, dot)), attribute
                .substring(dot + 1));
    }

    private static Object resolveFields(final CompositeData attribute, final String field) {
        final int dot = field.indexOf('.');
        if (dot < 0) {
            return attribute.get(field);
        }

        return resolveFields((CompositeData) attribute.get(field.substring(0, dot)), field.substring(dot + 1));
    }

    /**
     * Find a list of object names.
     *
     * @param query The wildcarded object name to list.
     * @return A list of matching object names.
     * @throws MalformedObjectNameException When the query could not be parsed.
     */
    static Set<ObjectName> queryNames(final String query) throws MalformedObjectNameException {
        final ObjectName objectNameQuery = new ObjectName(query);
        Set<ObjectName> names = new HashSet<>();

        final List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
        for (int i = 0; names.isEmpty() && i < servers.size(); i++) {
            names = servers.get(i).queryNames(objectNameQuery, null);
        }

        if (names.isEmpty()) {
            names = getPlatformMBeanServer().queryNames(objectNameQuery, null);
        }

        return names;
    }

    /**
     * Unregister an MBean, suppressing all errors that may arise. Good for
     * making sure a bean really is not there. We assume that the registered
     * bean was registered in the platform mbean server.
     *
     * @param objectName The object name of the MBean to unregister.
     */
    private static void unregister(final String objectName) {
        try {
            getPlatformMBeanServer().unregisterMBean(new ObjectName(objectName));
        } catch (Exception e) {
            // ignore, this was just to clean up
        }
    }
}
