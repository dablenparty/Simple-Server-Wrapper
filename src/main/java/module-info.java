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

    opens com.hunterltd.ssw.gui to javafx.fxml;
    opens com.hunterltd.ssw.gui.controllers to javafx.fxml;
    opens com.hunterltd.ssw.minecraft to com.google.gson;
    exports com.hunterltd.ssw.gui;
    exports com.hunterltd.ssw.gui.controllers;
}
