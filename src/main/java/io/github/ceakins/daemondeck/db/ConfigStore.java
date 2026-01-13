package io.github.ceakins.daemondeck.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

import java.io.IOException;
import java.util.Optional;

public class ConfigStore {

    private static ConfigStore instance;
    private final MVStore store;
    private final ObjectMapper objectMapper;

    private static final String CONFIG_KEY = "configuration";

    private ConfigStore() {
        this.store = MVStore.open("daemondeck.db");
        this.objectMapper = new ObjectMapper();
    }

    public static synchronized ConfigStore getInstance() {
        if (instance == null) {
            instance = new ConfigStore();
        }
        return instance;
    }

    public Optional<Configuration> getConfiguration() {
        MVMap<String, String> configMap = store.openMap("config");
        String configJson = configMap.get(CONFIG_KEY);
        if (configJson == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(configJson, Configuration.class));
        } catch (IOException e) {
            // Log the error
            return Optional.empty();
        }
    }

    public void saveConfiguration(Configuration config) {
        MVMap<String, String> configMap = store.openMap("config");
        try {
            String configJson = objectMapper.writeValueAsString(config);
            configMap.put(CONFIG_KEY, configJson);
            store.commit();
        } catch (IOException e) {
            // Log the error
        }
    }



    public boolean isConfigured() {
        MVMap<String, String> configMap = store.openMap("config");
        return configMap.containsKey(CONFIG_KEY);
    }

    public void close() {
        if (store != null && !store.isClosed()) {
            store.close();
        }
    }
}