package com.hunterltd.ServerWrapper;

import com.hunterltd.ServerWrapper.GUI.WrapperGUI;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello from main");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> {
            WrapperGUI wrapperGUI = new WrapperGUI();
            wrapperGUI.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            wrapperGUI.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    JFrame frame = (JFrame) e.getSource();
                    if (wrapperGUI.getServer() != null) {
                        int result = JOptionPane.showConfirmDialog(
                                frame,
                                "Are you sure you want to exit the server wrapper? This will stop the server if it's running.",
                                "Exit Server Wrapper",
                                JOptionPane.YES_NO_OPTION
                        );
                        if (result == JOptionPane.YES_OPTION) {
                            if (wrapperGUI.getServer().isRunning() && wrapperGUI.getServer().getServerProcess().isAlive()) {
                                try {
                                    wrapperGUI.getServer().sendCommand("stop");
                                } catch (IOException ioException) {
                                    ioException.printStackTrace();
                                }
                            }
                        }
                    }
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                }
            });
            wrapperGUI.pack();
            wrapperGUI.setVisible(true);
        });
    }
}
