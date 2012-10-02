package org.jlab.elog.util;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * System Utilities.
 * 
 * @author ryans
 */
public final class SystemUtil {

    private SystemUtil() {
        // Can't instantiate publicly
    }

    /**
     * Attempt to obtain the process ID of the running JVM. Java has no official
     * way to do this and this method will return null if unable to obtain the
     * id.
     *
     * The approach used is to check the RuntimeMXBean name field, which often
     * is in the form <verbatim>pid@hostname</verbatim>, but isn't required to
     * be (the value is JVM implementation dependent).
     *
     * @return The pid of the running JVM, or null if unable to obtain it.
     */
    public static Integer getJVMProcessId() {
        Integer id = null;

        String name = ManagementFactory.getRuntimeMXBean().getName();

        String[] tokens = name.split("@");

        if (tokens.length > 0) {
            try {
                id = Integer.parseInt(tokens[0]);
            } catch (NumberFormatException e) {
                // Oh well, we tried
            }
        }

        return id;
    }

    /**
     * Attempt to obtain the hostname of machine this JVM is running on.  This
     * method will return null if unable to obtain the hostname; possibly due to
     * misconfiguration on the local system.  If the system has multiple
     * network interfaces the hostname retrieved is arbitrarily chosen.
     * 
     * @return The hostname, or null if unable to obtain it.
     */
    public static String getHostname() {
        String hostname = null;

        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            // Oh well, we tried;
        }

        return hostname;
    }
}
