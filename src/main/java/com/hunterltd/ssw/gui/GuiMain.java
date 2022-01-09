package com.hunterltd.ssw.gui;

/**
 * This class is used as the entry point for the Maven shade compiler. If the entry point extends Application, it thinks
 * that the JavaFX modules are missing; in reality, they're getting packed into the fat JAR
 */
public class GuiMain {
    public static void main(String[] args) {
        SimpleServerWrapperGui.main(args);
    }
}
