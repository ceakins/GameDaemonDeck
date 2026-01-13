package io.github.ceakins.gamedaemondeck.db;

import java.io.Serializable;

public class DiscordBot implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String token;
    private String pluginName;

    public DiscordBot() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }
}
