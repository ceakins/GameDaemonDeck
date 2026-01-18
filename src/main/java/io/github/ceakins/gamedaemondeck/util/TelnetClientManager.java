package io.github.ceakins.gamedaemondeck.util;

import org.apache.commons.net.telnet.TelnetClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TelnetClientManager {

    private static final Logger logger = LoggerFactory.getLogger(TelnetClientManager.class);
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
        logger.debug("Connecting to Telnet server at {}:{}", server, port);
        telnetClient.connect(server, port);
        in = telnetClient.getInputStream();
        out = new PrintStream(telnetClient.getOutputStream());
    }

    public void disconnect() throws IOException {
        if (telnetClient != null && telnetClient.isConnected()) {
            logger.debug("Disconnecting from Telnet server");
            telnetClient.disconnect();
        }
    }

    public String sendCommand(String command) throws IOException {
        return sendCommand(command, ">", false);
    }

    public String sendCommand(String command, String expectedPrompt) throws IOException {
        return sendCommand(command, expectedPrompt, false);
    }

    public String sendCommand(String command, String expectedPrompt, boolean maskLog) throws IOException {
        if (telnetClient == null || !telnetClient.isConnected()) {
            throw new IOException("Not connected to server.");
        }
        
        if (maskLog) {
            logger.debug("Sending Telnet command: ******");
        } else {
            logger.debug("Sending Telnet command: {}", command);
        }

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> future = executor.submit(() -> {
                write(command);
                if (expectedPrompt == null) {
                    return ""; // Don't wait for response
                }
                return readUntil(expectedPrompt);
            });
            String response = future.get();
            logger.debug("Telnet response: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("Failed to send command", e);
            throw new IOException("Failed to send command", e);
        }
    }

    public String readUntil(String pattern) throws IOException {
        char lastChar = pattern.charAt(pattern.length() - 1);
        StringBuilder sb = new StringBuilder();
        int i;
        while ((i = in.read()) != -1) {
            char ch = (char) i;
            sb.append(ch);
            
            // Log lines as they are read
            if (ch == '\n') {
                logger.debug("Telnet RX: {}", sb.toString().trim());
            }
            
            if (ch == lastChar) {
                if (sb.toString().endsWith(pattern)) {
                    String result = sb.toString();
                    logger.debug("Read until '{}': {}", pattern, result);
                    return result;
                }
            }
        }
        String result = sb.toString();
        logger.debug("Read stream ended. Content: {}", result);
        return result;
    }

    public String read(long timeoutMillis) throws IOException {
        StringBuilder sb = new StringBuilder();
        long startTime = System.currentTimeMillis();
        byte[] buffer = new byte[1024];
        
        while ((System.currentTimeMillis() - startTime) < timeoutMillis) {
            if (in.available() > 0) {
                int len = in.read(buffer);
                if (len > 0) {
                    String chunk = new String(buffer, 0, len);
                    sb.append(chunk);
                    logger.debug("Telnet RX: {}", chunk.trim());
                }
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return sb.toString();
    }

    public void write(String value) {
        out.print(value + "\r\n"); // Explicitly send CRLF
        out.flush();
    }
}
