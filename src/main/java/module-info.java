module com.hunterltd.ssw {
    requires com.google.gson;
    requires commons.lang;
    requires jakarta.ws.rs;
    requires java.desktop;
    requires javafx.fxml;
    requires javafx.controls;
    requires jersey.common;
    requires net.sourceforge.argparse4j;
    requires org.apache.commons.io;
    requires zip4j;
    requires java.management;
    requires jdk.management;

    opens com.hunterltd.ssw.gui to javafx.fxml;
    opens com.hunterltd.ssw.gui.controllers to javafx.fxml;
    opens com.hunterltd.ssw.minecraft to com.google.gson;
    opens com.hunterltd.ssw.curse to com.google.gson;
    opens com.hunterltd.ssw.curse.api to com.google.gson;
    opens com.hunterltd.ssw.util.events to com.google.gson;
    exports com.hunterltd.ssw.gui;
    exports com.hunterltd.ssw.gui.controllers;
    exports com.hunterltd.ssw.gui.components;
    exports com.hunterltd.ssw.gui.model;
    exports com.hunterltd.ssw.minecraft;
    exports com.hunterltd.ssw.curse.api;
    exports com.hunterltd.ssw.util;
    exports com.hunterltd.ssw.util.concurrency;
}
