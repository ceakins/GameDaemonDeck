package io.github.ceakins.gamedaemondeck.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConfigStore {

    private static ConfigStore instance;
    private final MVStore store;
    private final ObjectMapper objectMapper;

    private static final String CONFIG_KEY = "configuration";
    private static final String WEBHOOKS_MAP = "webhooks";
    private static final String BOTS_MAP = "bots";
    private static final String DB_FILE_NAME = "gamedaemondeck.db";
    private static final String DATA_DIR = "data";

    // For testing
    ConfigStore(MVStore store, ObjectMapper objectMapper) {
        this.store = store;
        this.objectMapper = objectMapper;
    }

    private ConfigStore() {
        Path dataDirPath = Paths.get(DATA_DIR);
        try {
            if (!Files.exists(dataDirPath)) {
                Files.createDirectories(dataDirPath);
            }
        } catch (IOException e) {
            e.printStackTrace(); // Log error or handle appropriately
        }
        this.store = MVStore.open(dataDirPath.resolve(DB_FILE_NAME).toString());
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
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public void saveConfiguration(Configuration config) {
        MVMap<String, String> configMap = store.openMap("config");
        try {
            String configJson = objectMapper.writeValueAsString(config);
            configMap.put(CONFIG_KEY, configJson);
            store.commit();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public boolean isConfigured() {
        MVMap<String, String> configMap = store.openMap("config");
        return configMap.containsKey(CONFIG_KEY);
    }

    public void saveWebhook(DiscordWebhook webhook) {
        MVMap<String, String> webhooksMap = store.openMap(WEBHOOKS_MAP);
        try {
            String webhookJson = objectMapper.writeValueAsString(webhook);
            webhooksMap.put(webhook.getName(), webhookJson);
            store.commit();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public Optional<DiscordWebhook> getWebhook(String name) {
        MVMap<String, String> webhooksMap = store.openMap(WEBHOOKS_MAP);
        String webhookJson = webhooksMap.get(name);
        if (webhookJson == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(webhookJson, DiscordWebhook.class));
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public List<DiscordWebhook> getAllWebhooks() {
        MVMap<String, String> webhooksMap = store.openMap(WEBHOOKS_MAP);
        return webhooksMap.values().stream().map(webhookJson -> {
            try {
                return objectMapper.readValue(webhookJson, DiscordWebhook.class);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).collect(Collectors.toList());
    }

    public void deleteWebhook(String name) {
        MVMap<String, String> webhooksMap = store.openMap(WEBHOOKS_MAP);
        webhooksMap.remove(name);
        store.commit();
    }

    public void saveBot(DiscordBot bot) {
        MVMap<String, String> botsMap = store.openMap(BOTS_MAP);
        try {
            String botJson = objectMapper.writeValueAsString(bot);
            botsMap.put(bot.getName(), botJson);
            store.commit();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public Optional<DiscordBot> getBot(String name) {
        MVMap<String, String> botsMap = store.openMap(BOTS_MAP);
        String botJson = botsMap.get(name);
        if (botJson == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(botJson, DiscordBot.class));
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public List<DiscordBot> getAllBots() {
        MVMap<String, String> botsMap = store.openMap(BOTS_MAP);
        return botsMap.values().stream().map(botJson -> {
            try {
                return objectMapper.readValue(botJson, DiscordBot.class);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).collect(Collectors.toList());
    }

    public void deleteBot(String name) {
        MVMap<String, String> botsMap = store.openMap(BOTS_MAP);
        botsMap.remove(name);
        store.commit();
    }

    public void close() {
        if (store != null && !store.isClosed()) {
            store.close();
        }
    }
}