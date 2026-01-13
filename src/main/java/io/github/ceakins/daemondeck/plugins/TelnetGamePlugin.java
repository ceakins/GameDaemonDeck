package io.github.ceakins.daemondeck.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.apache.commons.net.telnet.TelnetClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TelnetGamePlugin implements GamePlugin {

    private TelnetClient telnet;
    private InputStream in;
    private PrintStream out;
    private Process serverProcess;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();


    public TelnetGamePlugin() {
    }

    @Override
    public String getName() {
        return "Telnet-based Game";
    }

    @Override
    public void startServer(Path serverPath) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(serverPath.toString());
        serverProcess = pb.start();
        sendWebhookMessage(getName() + " server started.");
        // In a real scenario, you'd wait for the server to be ready
        connect("localhost", 27015);
    }

    @Override
    public void stopServer() throws IOException {
        disconnect();
        if (serverProcess != null) {
            serverProcess.destroy();
        }
        sendWebhookMessage(getName() + " server stopped.");
    }

    @Override
    public String sendCommand(String command) throws IOException {
        if (telnet == null || !telnet.isConnected()) {
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

    @Override
    public String getBotName() {
        return "TelnetGameBot";
    }

    private void connect(String server, int port) throws IOException {
        telnet = new TelnetClient();
        telnet.connect(server, port);
        in = telnet.getInputStream();
        out = new PrintStream(telnet.getOutputStream());
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

    private void write(String value) {
        out.println(value);
        out.flush();
    }

    private void disconnect() throws IOException {
        if (telnet != null && telnet.isConnected()) {
            telnet.disconnect();
        }
    }

    private void sendWebhookMessage(String content) {
        // In a real app, the webhook URL would be configurable
        String webhookUrl = "YOUR_WEBHOOK_URL";
        try {
            Map<String, String> payload = Map.of("content", content);
            RequestBody body = RequestBody.create(
                    objectMapper.writeValueAsString(payload),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(webhookUrl)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                // Log response if needed
            }
        } catch (IOException e) {
            // Log error
        }
    }
}
