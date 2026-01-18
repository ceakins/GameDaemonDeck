package io.github.ceakins.gamedaemondeck.core;

import io.github.ceakins.gamedaemondeck.db.ConfigStore;
import io.github.ceakins.gamedaemondeck.db.Configuration;
import io.javalin.Javalin;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.CookieManager;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class GameDaemonDeckAppTest {

    private static final Logger logger = LoggerFactory.getLogger(GameDaemonDeckAppTest.class);

    @Mock
    private ConfigStore configStore;

    @Mock
    private DiscordService discordService;

    @Mock
    private PluginManager pluginManager;

    private GameDaemonDeckApp gameDaemonDeckApp;
    private Javalin app;
    private final CookieJar cookieJar = new CookieJar() {
        private final java.util.HashMap<String, java.util.List<Cookie>> cookieStore = new java.util.HashMap<>();

        @Override
        public void saveFromResponse(HttpUrl url, java.util.List<Cookie> cookies) {
            cookieStore.put(url.host(), cookies);
        }

        @Override
        public java.util.List<Cookie> loadForRequest(HttpUrl url) {
            java.util.List<Cookie> cookies = cookieStore.get(url.host());
            return cookies != null ? cookies : new java.util.ArrayList<>();
        }
    };
    private final OkHttpClient client = new OkHttpClient.Builder().followRedirects(false).cookieJar(cookieJar).build();

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        gameDaemonDeckApp = new GameDaemonDeckApp(configStore, discordService, pluginManager);
        app = gameDaemonDeckApp.app;
        app.start(0); // Start on a random available port
    }

    @AfterMethod
    public void tearDown() {
        app.stop();
    }

    private void performLogin(String username, String password) throws IOException {
        okhttp3.FormBody loginBody = new okhttp3.FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();

        Request loginRequest = new Request.Builder()
                .url("http://localhost:" + app.port() + "/login")
                .post(loginBody)
                .build();

        // Execute the login request. The cookieJar will automatically save the session cookie.
        try (Response response = client.newCall(loginRequest).execute()) {
            assertEquals(response.code(), 302); // Expect redirect after successful login
            assertTrue(response.header("Location").contains("/"));
        }
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
            assertTrue(responseBody.contains("DaemonDeck Setup"));
            assertTrue(responseBody.contains("Session Timeout (seconds):"));
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
            assertEquals(response.code(), 302); // Expect redirect to login
            assertTrue(response.header("Location").contains("/login")); // Verify redirect location
        }
    }

    @Test
    public void testApiEndpointReturns401WhenNotLoggedIn() throws IOException {
        Configuration config = new Configuration();
        config.setAdminUsername("admin");
        config.setAdminPasswordHash(BCrypt.hashpw("password", BCrypt.gensalt()));
        config.setSessionTimeoutSeconds(1800);
        when(configStore.isConfigured()).thenReturn(true);
        when(configStore.getConfiguration()).thenReturn(Optional.of(config));

        Request request = new Request.Builder()
                .url("http://localhost:" + app.port() + "/api/servers/status")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(response.code(), 401, "API endpoint should return 401 Unauthorized when not logged in");
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

        performLogin("admin", "password"); // Perform login to establish session

        Request request = new Request.Builder()
                .url("http://localhost:" + app.port() + "/")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string(); // Read once
            assertEquals(response.code(), 200);
            assertTrue(responseBody.contains("Welcome to Game Daemon Deck!"));
        }
    }

    @Test
    public void testSettingsPageRendersSessionTimeout() throws IOException {
        Configuration config = new Configuration();
        config.setAdminUsername("admin");
        config.setAdminPasswordHash(BCrypt.hashpw("password", BCrypt.gensalt()));
        config.setSessionTimeoutSeconds(1800);
        config.setSteamCmdPath("/path/to/steamcmd");
        when(configStore.isConfigured()).thenReturn(true);
        when(configStore.getConfiguration()).thenReturn(Optional.of(config));

        performLogin("admin", "password"); // Perform login to establish session

        Request request = new Request.Builder()
                .url("http://localhost:" + app.port() + "/settings")
                .build();

        try (Response response = client.newCall(request).execute()) { // Use the client with cookieJar
            String responseBody = response.body().string();
            assertEquals(response.code(), 200);
            assertTrue(responseBody.contains("const sessionTimeoutSeconds =") && responseBody.contains("1800;"));
        }
    }
}