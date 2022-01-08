package com.hunterltd.ssw.minecraft;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

class Log4JPatcher {
    private static final MinecraftVersion ONE_EIGHTEEN_ONE = MinecraftVersion.of("1.18.1");
    private static final MinecraftVersion ONE_SEVENTEEN = MinecraftVersion.of("1.17");
    private static final MinecraftVersion ONE_TWELVE = MinecraftVersion.of("1.12");
    private static final MinecraftVersion ONE_SEVEN = MinecraftVersion.of("1.7");
    private static final String[] POSSIBLE_ARGS = new String[]{
            "-Dlog4j2.formatMsgNoLookups=true",
            "-Dlog4j.configurationFile=log4j2_112-116.xml",
            "-Dlog4j.configurationFile=log4j2_17-111.xml"
    };
    private final URL patchFileUrl;
    private final String jvmArgs;
    private final MinecraftServer minecraftServer;

    Log4JPatcher(MinecraftServer server) {
        minecraftServer = server;
        MinecraftVersion serverVersion = server.getServerSettings().getVersion();

        String args = "";
        URL url = null;
        // versions 1.18.1 and later are patched, versions 1.7 and earlier are not affected
        if (versionInRange(serverVersion, ONE_SEVENTEEN, ONE_EIGHTEEN_ONE)) {
            args = POSSIBLE_ARGS[0];
        } else if (versionInRange(serverVersion, ONE_TWELVE, ONE_SEVENTEEN)) {
            try {
                url = new URL("https://launcher.mojang.com/v1/objects/02937d122c86ce73319ef9975b58896fc1b491d1/log4j2_112-116.xml");
                args = POSSIBLE_ARGS[1];
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        } else if (versionInRange(serverVersion, ONE_SEVEN, ONE_TWELVE)) {
            try {
                url = new URL("https://launcher.mojang.com/v1/objects/4bb89a97a66f350bc9f73b3ca8509632682aea2e/log4j2_17-111.xml");
                args = POSSIBLE_ARGS[2];
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        jvmArgs = args;
        patchFileUrl = url;
    }

    public URL getPatchFileUrl() {
        return patchFileUrl;
    }

    public String getJvmArgs() {
        return jvmArgs;
    }

    /**
     * Downloads the patch file (if necessary) and adds the JVM arguments (if necessary)
     *
     * @throws IOException if an I/O error occurs downloading the patch file
     */
    public void patch() throws IOException {
        if (patchFileUrl != null) {
            String file = patchFileUrl.getFile();
            int sepIndex = file.lastIndexOf('/');
            String fileName = file.substring(sepIndex + 1);
            File destination = new File(minecraftServer.getServerPath().getParent().toString(), fileName);
            if (!destination.exists()) {
                FileUtils.copyURLToFile(patchFileUrl, destination);
            }
        }
        if (!jvmArgs.isEmpty()) {
            MinecraftServer.ServerSettings serverSettings = minecraftServer.getServerSettings();
            List<String> extraArgs = serverSettings.getExtraArgs();
            if (!extraArgs.contains(jvmArgs)) {
                // clean list
                for (String arg : POSSIBLE_ARGS)
                    extraArgs.remove(arg);

                extraArgs.add(jvmArgs);
                serverSettings.setExtraArgs(extraArgs);
                serverSettings.writeData();
                minecraftServer.updateExtraArgs();
            }
        }
    }

    /**
     * Checks whether a {@code MinecraftVersion} was released between two other versions.
     *
     * @param version version to compare
     * @param start   start of the range (inclusive)
     * @param end     end of the range (exclusive)
     * @return {@code true} if {@code version} is within the range. {@code false} otherwise
     */
    private boolean versionInRange(MinecraftVersion version, MinecraftVersion start, MinecraftVersion end) {
        return version.compareTo(start) >= 0 && version.compareTo(end) < 0;
    }
}
