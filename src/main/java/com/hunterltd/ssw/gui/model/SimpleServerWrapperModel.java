package com.hunterltd.ssw.gui.model;

import com.sun.management.OperatingSystemMXBean;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class SimpleServerWrapperModel {
    private final BooleanProperty autoScroll;
    private final BooleanProperty serverRunning;
    private final StringProperty outputtedText;
    private final StringProperty serverPath;
    private final StringProperty commandText;
    private final ObservableList<Double> serverMemoryOptions;
    private final double maxMemory;

    public SimpleServerWrapperModel() {
        outputtedText = new SimpleStringProperty("");
        double memGigabytes = calculateSystemMemoryGb();
        ArrayList<Double> memOpts = DoubleStream
                .iterate(0.5, i -> i < memGigabytes, i -> i + 0.5)
                .boxed()
                .collect(Collectors.toCollection(() -> new ArrayList<>((int) Math.round(memGigabytes) * 2)));
        serverMemoryOptions = FXCollections.observableArrayList(memOpts);
        maxMemory = memOpts.get(memOpts.size() - 1);
        serverPath = new SimpleStringProperty();
        serverRunning = new SimpleBooleanProperty(false);
        commandText = new SimpleStringProperty("");
        autoScroll = new SimpleBooleanProperty(true);
    }

    public boolean isAutoScroll() {
        return autoScroll.get();
    }

    public void setAutoScroll(boolean autoScroll) {
        this.autoScroll.set(autoScroll);
    }

    public BooleanProperty autoScrollProperty() {
        return autoScroll;
    }

    public String getCommandText() {
        return commandText.get();
    }

    public void setCommandText(String commandText) {
        this.commandText.set(commandText);
    }

    public StringProperty commandTextProperty() {
        return commandText;
    }

    public boolean isServerRunning() {
        return serverRunning.get();
    }

    public void setServerRunning(boolean serverRunning) {
        this.serverRunning.set(serverRunning);
    }

    public BooleanProperty serverRunningProperty() {
        return serverRunning;
    }

    public String getServerPath() {
        return serverPath.get();
    }

    public void setServerPath(String serverPath) {
        this.serverPath.set(serverPath);
    }

    public StringProperty serverPathProperty() {
        return serverPath;
    }

    public double getMaxMemory() {
        return maxMemory;
    }

    private double calculateSystemMemoryGb() {
        long memBytes = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalMemorySize();
        return ((double) memBytes) / Math.pow(1024, 3);
    }

    public ObservableList<Double> getServerMemoryOptions() {
        return serverMemoryOptions;
    }

    public String getOutputtedText() {
        return outputtedText.get();
    }

    public void setOutputtedText(String outputtedText) {
        this.outputtedText.set(outputtedText);
    }

    public StringProperty outputtedTextProperty() {
        return outputtedText;
    }

    public void appendToOutputtedText(String text) {
        this.outputtedText.set(getOutputtedText() + text);
    }
}
