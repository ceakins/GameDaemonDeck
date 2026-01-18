package io.github.ceakins.gamedaemondeck.plugins;

import io.github.ceakins.gamedaemondeck.db.GameServer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface GamePlugin {

    String getName();

    void startServer(Path serverPath) throws IOException;

    void stopServer() throws IOException;

    String getBotName();

    default List<ConfigField> getConfigFields() {
        return Collections.emptyList();
    }

    default List<LogHighlighter> getLogHighlighters() {
        return Collections.emptyList();
    }

    default List<ServerConfigField> getServerConfigFields() {
        return Collections.emptyList();
    }

    default void generateConfigFile(Map<String, String> values, Path serverDir, String fileName) throws IOException {
        // Default implementation does nothing
    }

    default Map<String, String> parseConfigFile(Path configFile) throws IOException {
        return Collections.emptyMap();
    }

    default Path getConfigFileFromParams(Map<String, String> params) {
        return null;
    }

    default void shutdownServer(GameServer server) throws IOException {
        stopServer(); // Default fallback
    }
}
