package com.hunterltd.ssw.util.os;

import java.nio.file.Path;

public class OsConstants {
    public static final String OS_NAME = System.getProperty("os.name");
    public static final Path USER_HOME = Path.of(System.getProperty("user.home"));
}
