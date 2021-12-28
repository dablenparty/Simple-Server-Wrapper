package com.hunterltd.ssw.gui.model;

import com.sun.management.OperatingSystemMXBean;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class SimpleServerWrapperModel {
    private final BooleanProperty serverRunning;
    private final BooleanProperty restart;
    private final BooleanProperty proxy;
    private final StringProperty outputtedText;
    private final StringProperty serverPath;
    private final StringProperty extraArgs;
    private final ObservableList<Double> serverMemoryOptions;
    private final IntegerProperty restartInterval;
    private final IntegerProperty proxyShutdownInterval;
    private final double maxMemory;

    public SimpleServerWrapperModel() {
        outputtedText = new SimpleStringProperty();
        double memGigabytes = calculateSystemMemoryGb();
        ArrayList<Double> memOpts = DoubleStream
                .iterate(0.5, i -> i < memGigabytes, i -> i + 0.5)
                .boxed()
                .collect(Collectors.toCollection(() -> new ArrayList<>((int) Math.round(memGigabytes) * 2)));
        serverMemoryOptions = FXCollections.observableArrayList(memOpts);
        maxMemory = memOpts.get(memOpts.size() - 1);
        restartInterval = new SimpleIntegerProperty();
        serverPath = new SimpleStringProperty();
        extraArgs = new SimpleStringProperty();
        serverRunning = new SimpleBooleanProperty(false);
        proxyShutdownInterval = new SimpleIntegerProperty();
        restart = new SimpleBooleanProperty();
        proxy = new SimpleBooleanProperty();
    }

    public boolean isRestart() {
        return restart.get();
    }

    public void setRestart(boolean restart) {
        this.restart.set(restart);
    }

    public BooleanProperty restartProperty() {
        return restart;
    }

    public boolean isProxy() {
        return proxy.get();
    }

    public void setProxy(boolean proxy) {
        this.proxy.set(proxy);
    }

    public BooleanProperty proxyProperty() {
        return proxy;
    }

    public int getProxyShutdownInterval() {
        return proxyShutdownInterval.get();
    }

    public void setProxyShutdownInterval(int proxyShutdownInterval) {
        this.proxyShutdownInterval.set(proxyShutdownInterval);
    }

    public IntegerProperty proxyShutdownIntervalProperty() {
        return proxyShutdownInterval;
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

    public String getExtraArgs() {
        return extraArgs.get();
    }

    public void setExtraArgs(String extraArgs) {
        this.extraArgs.set(extraArgs);
    }

    public StringProperty extraArgsProperty() {
        return extraArgs;
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
