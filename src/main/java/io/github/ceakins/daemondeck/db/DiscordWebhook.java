package io.github.ceakins.daemondeck.db;

import java.io.Serializable;

public class DiscordWebhook implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String url;
    private String pluginName;

    public DiscordWebhook() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }
}
