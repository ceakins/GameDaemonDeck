package io.github.ceakins.gamedaemondeck.plugins;

import java.io.IOException;
import java.nio.file.Path;

public class SevenDaysToDiePlugin implements GamePlugin {

    @Override
    public String getName() {
        return "7 Days to Die";
    }

    @Override
    public void startServer(Path serverPath) throws IOException {
        // Placeholder for starting the 7 Days to Die server
    }

    @Override
    public void stopServer() throws IOException {
        // Placeholder for stopping the 7 Days to Die server
    }

    @Override
    public String getBotName() {
        return null; // Or a specific bot name if applicable
    }
}
