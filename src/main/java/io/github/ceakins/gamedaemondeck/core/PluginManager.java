package io.github.ceakins.gamedaemondeck.core;

import io.github.ceakins.gamedaemondeck.plugins.GamePlugin;
import io.github.ceakins.gamedaemondeck.plugins.SevenDaysToDiePlugin;

import java.util.ArrayList;
import java.util.List;

public class PluginManager {

    private final List<GamePlugin> plugins;

    public PluginManager() {
        this.plugins = new ArrayList<>();
        // For now, we'll manually register the plugins.
        // In the future, this could be done via reflection or a service loader.
        this.plugins.add(new SevenDaysToDiePlugin());
    }

    public List<GamePlugin> getPlugins() {
        return plugins;
    }

    public GamePlugin getPlugin(String name) {
        return plugins.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
