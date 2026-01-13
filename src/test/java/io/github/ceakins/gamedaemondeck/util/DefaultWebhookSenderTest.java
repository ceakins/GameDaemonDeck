package io.github.ceakins.gamedaemondeck.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultWebhookSenderTest {

    @Mock
    private OkHttpClient httpClient;

    @Mock
    private Call httpCall;

    @Mock
    private Response httpResponse;

    private ObjectMapper objectMapper = new ObjectMapper();

    private DefaultWebhookSender webhookSender;

    @BeforeMethod
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        webhookSender = new DefaultWebhookSender(httpClient, objectMapper);

        when(httpClient.newCall(any(Request.class))).thenReturn(httpCall);
        when(httpCall.execute()).thenReturn(httpResponse);
        when(httpResponse.isSuccessful()).thenReturn(true);
    }

    @Test
    public void testSendWebhookMessage() throws IOException {
        String url = "http://example.com/webhook";
        String content = "Test message";

        webhookSender.sendWebhookMessage(url, content);

        verify(httpClient).newCall(any(Request.class));
        verify(httpCall).execute();
    }
}
