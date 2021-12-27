package com.hunterltd.ssw.gui.model;

import com.sun.management.OperatingSystemMXBean;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class SimpleServerWrapperModel {
    private final StringProperty outputtedText;
    private final StringProperty serverPath;
    private final ObservableList<Double> serverMemory;
    private final IntegerProperty restartInterval;
    private final double maxMemory;

    public SimpleServerWrapperModel() {
        outputtedText = new SimpleStringProperty();
        double memGigabytes = calculateSystemMemoryGb();
        ArrayList<Double> memOpts = DoubleStream
                .iterate(0.5, i -> i < memGigabytes, i -> i + 0.5)
                .boxed()
                .collect(Collectors.toCollection(() -> new ArrayList<>((int) Math.round(memGigabytes) * 2)));
        serverMemory = FXCollections.observableArrayList(memOpts);
        maxMemory = memOpts.get(memOpts.size() - 1);
        restartInterval = new SimpleIntegerProperty();
        serverPath = new SimpleStringProperty();
    }

    public String getServerPath() {
        return serverPath.get();
    }

    public StringProperty serverPathProperty() {
        return serverPath;
    }

    public void setServerPath(String serverPath) {
        this.serverPath.set(serverPath);
    }

    public int getRestartInterval() {
        return restartInterval.get();
    }

    public void setRestartInterval(int restartInterval) {
        this.restartInterval.set(restartInterval);
    }

    public IntegerProperty restartIntervalProperty() {
        return restartInterval;
    }

    public double getMaxMemory() {
        return maxMemory;
    }

    private double calculateSystemMemoryGb() {
        long memBytes = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalMemorySize();
        return ((double) memBytes) / Math.pow(1024, 3);
    }

    public ObservableList<Double> getServerMemory() {
        return serverMemory;
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
