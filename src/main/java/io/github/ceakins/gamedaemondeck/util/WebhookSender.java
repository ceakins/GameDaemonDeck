package io.github.ceakins.gamedaemondeck.util;

import java.io.IOException;

public interface WebhookSender {
    void sendWebhookMessage(String url, String content) throws IOException;
}
