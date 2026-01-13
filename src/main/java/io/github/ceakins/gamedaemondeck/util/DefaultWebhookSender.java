package io.github.ceakins.gamedaemondeck.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;

public class DefaultWebhookSender implements WebhookSender {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DefaultWebhookSender(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendWebhookMessage(String url, String content) throws IOException {
        Map<String, String> payload = Map.of("content", content);
        RequestBody body = RequestBody.create(
                objectMapper.writeValueAsString(payload),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            // Log response if needed
        }
    }
}
