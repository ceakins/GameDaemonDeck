package io.github.ceakins.gamedaemondeck.plugins;

import java.io.IOException;
import java.nio.file.Path;

public interface GamePlugin {

    String getName();

    void startServer(Path serverPath) throws IOException;

    void stopServer() throws IOException;

    String getBotName();
}
