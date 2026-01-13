package io.github.ceakins.gamedaemondeck.util;

import org.glavo.rcon.AuthenticationException;
import org.glavo.rcon.Rcon;

import java.io.IOException;

public class RCONClientManager {

    private Rcon rcon;
    private final String host;
    private final int port;
    private final String password;
    private final Rcon rconInstance; // For testing

    public RCONClientManager(String host, int port, String password) {
        this(host, port, password, null); // Default to null for non-test usage
    }

    // Constructor for testing purposes
    RCONClientManager(String host, int port, String password, Rcon rconInstance) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.rconInstance = rconInstance;
    }

    public void connect() throws IOException, AuthenticationException {
        if (rconInstance != null) {
            rcon = rconInstance;
        } else {
            rcon = new Rcon(host, port, password);
        }
    }

    public void disconnect() throws IOException {
        if (rcon != null) {
            rcon.close();
        }
    }

    public String sendCommand(String command) throws IOException {
        if (rcon == null) {
            throw new IOException("Not connected to server.");
        }
        return rcon.command(command);
    }
}
