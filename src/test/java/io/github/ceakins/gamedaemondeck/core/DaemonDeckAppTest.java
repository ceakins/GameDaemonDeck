package io.github.ceakins.gamedaemondeck.core;

import io.github.ceakins.gamedaemondeck.db.ConfigStore;
import io.github.ceakins.gamedaemondeck.db.Configuration;
import io.javalin.Javalin;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DaemonDeckAppTest {

    @Mock
    private ConfigStore configStore;

    @Mock
    private DiscordService discordService;

    private DaemonDeckApp daemonDeckApp;
    private Javalin app;
    private final OkHttpClient client = new OkHttpClient.Builder().followRedirects(false).build();

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        daemonDeckApp = new DaemonDeckApp(configStore, discordService);
        app = daemonDeckApp.app;
        app.start(0); // Start on a random available port
    }

    @AfterMethod
    public void tearDown() {
        app.stop();
    }

    @Test
    public void testSetupPageServedIfNotConfigured() throws IOException {
        when(configStore.isConfigured()).thenReturn(false);

        Request request = new Request.Builder()
                .url("http://localhost:" + app.port() + "/setup")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string(); // Read once
            assertEquals(response.code(), 200);
            assertTrue(responseBody.contains("DaemonDeck First-Run Setup"));
            assertTrue(responseBody.contains("Session Timeout (seconds):</label>")); // New assertion
        }
    }

    @Test
    public void testRootRedirectsToSetupIfNotConfigured() throws IOException {
        when(configStore.isConfigured()).thenReturn(false);

        Request request = new Request.Builder()
                .url("http://localhost:" + app.port() + "/")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(response.code(), 302);
            assertTrue(response.header("Location").contains("/setup"));
        }
    }

    @Test
    public void testSetupPostConfiguresAppAndRedirects() throws IOException {
        when(configStore.isConfigured()).thenReturn(false);

        okhttp3.FormBody body = new okhttp3.FormBody.Builder()
                .add("adminUsername", "testuser")
                .add("adminPassword", "testpass")
                .add("steamCmdPath", "/path/to/steamcmd")
                .add("sessionTimeoutSeconds", "1800")
                .build();

        Request request = new Request.Builder()
                .url("http://localhost:" + app.port() + "/setup")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(response.code(), 302);
            assertTrue(response.header("Location").contains("/"));
            verify(configStore).saveConfiguration(any(Configuration.class));
        }
    }

    @Test
    public void testSetupPostRejectsInvalidSessionTimeout() throws IOException {
        when(configStore.isConfigured()).thenReturn(false);

        okhttp3.FormBody body = new okhttp3.FormBody.Builder()
                .add("adminUsername", "testuser")
                .add("adminPassword", "testpass")
                .add("steamCmdPath", "/path/to/steamcmd")
                .add("sessionTimeoutSeconds", "0") // Invalid value
                .build();

        Request request = new Request.Builder()
                .url("http://localhost:" + app.port() + "/setup")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            assertEquals(response.code(), 400); // Bad Request
            assertTrue(responseBody.contains("Session Timeout must be a positive integer."));
        }
    }

    @Test
    public void testRootRequiresAuthenticationWhenConfigured() throws IOException {
        Configuration config = new Configuration();
        config.setAdminUsername("admin");
        config.setAdminPasswordHash(BCrypt.hashpw("password", BCrypt.gensalt()));
        config.setSessionTimeoutSeconds(1800); // Set a valid timeout
        when(configStore.isConfigured()).thenReturn(true);
        when(configStore.getConfiguration()).thenReturn(Optional.of(config));

        Request request = new Request.Builder()
                .url("http://localhost:" + app.port() + "/")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(response.code(), 401);
            assertTrue(response.header("WWW-Authenticate").contains("Basic"));
        }
    }

    @Test
    public void testRootAllowsAuthenticatedAccessWhenConfigured() throws IOException {
        Configuration config = new Configuration();
        config.setAdminUsername("admin");
        config.setAdminPasswordHash(BCrypt.hashpw("password", BCrypt.gensalt()));
        config.setSessionTimeoutSeconds(1800); // Set a valid timeout
        when(configStore.isConfigured()).thenReturn(true);
        when(configStore.getConfiguration()).thenReturn(Optional.of(config));

        String credential = okhttp3.Credentials.basic("admin", "password");
        Request request = new Request.Builder()
                .url("http://localhost:" + app.port() + "/")
                .header("Authorization", credential)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(response.code(), 200);
            assertTrue(response.body().string().contains("Welcome to GameDaemonDeck!"));
        }
    }

    @Test
    public void testMainPageRendersSessionTimeout() throws IOException {
        Configuration config = new Configuration();
        config.setAdminUsername("admin");
        config.setAdminPasswordHash(BCrypt.hashpw("password", BCrypt.gensalt()));
        config.setSessionTimeoutSeconds(1800);
        when(configStore.isConfigured()).thenReturn(true);
        when(configStore.getConfiguration()).thenReturn(Optional.of(config));

        // Create a client that follows redirects and authenticates
        OkHttpClient authenticatedClient = new OkHttpClient.Builder().authenticator((route, response) -> {
            String credential = okhttp3.Credentials.basic("admin", "password");
            return response.request().newBuilder().header("Authorization", credential).build();
        }).build();

        Request request = new Request.Builder()
                .url("http://localhost:" + app.port() + "/")
                .build();

        try (Response response = authenticatedClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            assertEquals(response.code(), 200);
            assertTrue(responseBody.contains("Welcome to GameDaemonDeck!")); // Corrected assertion
            assertTrue(responseBody.contains("const sessionTimeoutSeconds =") && responseBody.contains("1800;")); // More generic assertion
        }
    }
}