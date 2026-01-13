package io.github.ceakins.daemondeck.db;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.repository.ObjectRepository;

import java.util.Optional;

public class ConfigStore {

    private static ConfigStore instance;
    private final Nitrite db;
    private final ObjectRepository<Configuration> configRepository;

    private ConfigStore() {
        this.db = Nitrite.builder()
                .file("daemondeck.db")
                .openOrCreate();
        this.configRepository = db.getRepository(Configuration.class);
    }

    public static synchronized ConfigStore getInstance() {
        if (instance == null) {
            instance = new ConfigStore();
        }
        return instance;
    }

    public Optional<Configuration> getConfiguration() {
        return Optional.ofNullable(configRepository.find().firstOrDefault());
    }

    public void saveConfiguration(Configuration config) {
        if (configRepository.find().totalCount() > 0) {
            config.setId(configRepository.find().firstOrDefault().getId());
            configRepository.update(config);
        } else {
            configRepository.insert(config);
        }
    }

    public boolean isConfigured() {
        return configRepository.find().totalCount() > 0;
    }

    public void close() {
        if (db != null && !db.isClosed()) {
            db.close();
        }
    }
}
