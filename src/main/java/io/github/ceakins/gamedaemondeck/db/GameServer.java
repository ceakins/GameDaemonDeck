package io.github.ceakins.gamedaemondeck.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameServer implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String appId;
    private String pluginName;
    private boolean running;
    private String headerColor;
    private String fontColor;
    private String serverPath;
    private String commandLine;
    private Long pid;
    private List<String> restartTimes = new ArrayList<>();

    public GameServer() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public String getHeaderColor() {
        return headerColor;
    }

    public void setHeaderColor(String headerColor) {
        this.headerColor = headerColor;
    }

    public String getFontColor() {
        return fontColor;
    }

    public void setFontColor(String fontColor) {
        this.fontColor = fontColor;
    }

    public String getServerPath() {
        return serverPath;
    }

    public void setServerPath(String serverPath) {
        this.serverPath = serverPath;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(String commandLine) {
        this.commandLine = commandLine;
    }

    public Long getPid() {
        return pid;
    }

    public void setPid(Long pid) {
        this.pid = pid;
    }

    public List<String> getRestartTimes() {
        return restartTimes;
    }

    public void setRestartTimes(List<String> restartTimes) {
        this.restartTimes = restartTimes;
    }
}
