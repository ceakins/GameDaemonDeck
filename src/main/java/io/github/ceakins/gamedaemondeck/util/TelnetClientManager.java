package io.github.ceakins.gamedaemondeck.util;

import org.apache.commons.net.telnet.TelnetClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TelnetClientManager {

    private final TelnetClient telnetClient;
    InputStream in;
    PrintStream out;

    public TelnetClientManager() {
        this(new TelnetClient());
    }

    public TelnetClientManager(TelnetClient telnetClient) {
        this.telnetClient = telnetClient;
    }

    public void connect(String server, int port) throws IOException {
        telnetClient.connect(server, port);
        in = telnetClient.getInputStream();
        out = new PrintStream(telnetClient.getOutputStream());
    }

    public void disconnect() throws IOException {
        if (telnetClient != null && telnetClient.isConnected()) {
            telnetClient.disconnect();
        }
    }

    public String sendCommand(String command) throws IOException {
        if (telnetClient == null || !telnetClient.isConnected()) {
            throw new IOException("Not connected to server.");
        }
        
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> future = executor.submit(() -> {
                write(command);
                return readUntil(">");
            });
            return future.get();
        } catch (Exception e) {
            throw new IOException("Failed to send command", e);
        }
    }

    private String readUntil(String pattern) throws IOException {
        char lastChar = pattern.charAt(pattern.length() - 1);
        StringBuilder sb = new StringBuilder();
        char ch;
        while ((ch = (char) in.read()) != -1) {
            sb.append(ch);
            if (ch == lastChar) {
                if (sb.toString().endsWith(pattern)) {
                    return sb.toString();
                }
            }
        }
        return sb.toString();
    }

    private String write(String value) {
        out.println(value);
        out.flush();
        return value; // Return value for testing purposes
    }
}
