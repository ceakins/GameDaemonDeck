package io.github.ceakins.daemondeck.util;

import io.github.ceakins.daemondeck.db.ConfigStore;
import io.github.ceakins.daemondeck.db.Configuration;

import java.io.IOException;
import java.nio.file.Path;

public class SteamManager {

    private final ConfigStore configStore;

    public SteamManager(ConfigStore configStore) {
        this.configStore = configStore;
    }

    public void installOrUpdateGame(String appId, String installDir) throws IOException, InterruptedException {
        Configuration config = configStore.getConfiguration()
                .orElseThrow(() -> new IllegalStateException("Application is not configured."));

        Path steamCmdPath = Path.of(config.getSteamCmdPath());
        ProcessBuilder processBuilder = new ProcessBuilder(
                steamCmdPath.toString(),
                "+login", "anonymous",
                "+app_update", appId,
                "+quit"
        );
        processBuilder.directory(Path.of(installDir).toFile());

        Process process = processBuilder.start();
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> {
                try {
                    new String(process.getInputStream().readAllBytes());
                } catch (IOException e) {
                    // Handle exception
                }
            });
            executor.submit(() -> {
                try {
                    new String(process.getErrorStream().readAllBytes());
                } catch (IOException e) {
                    // Handle exception
                }
            });
        }
        process.waitFor();
    }
}
