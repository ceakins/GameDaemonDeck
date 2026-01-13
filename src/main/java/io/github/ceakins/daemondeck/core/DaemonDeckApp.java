package io.github.ceakins.daemondeck.core;

import io.github.ceakins.daemondeck.db.ConfigStore;
import io.github.ceakins.daemondeck.db.Configuration;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.Base64;
import java.util.Collections;
import java.util.Optional;
import java.util.StringTokenizer;

public class DaemonDeckApp {

    private static final ConfigStore configStore = ConfigStore.getInstance();

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            // configuration here, if needed
        }).start(7070);

        // Before-filter to redirect to /setup if not configured
        app.before(ctx -> {
            if (!ctx.path().equals("/setup") && !configStore.isConfigured()) {
                ctx.redirect("/setup", HttpStatus.FOUND);
            }
        });

        // Before-filter for Basic Authentication on all routes except /setup
        app.before(ctx -> {
            if (ctx.path().equals("/setup") || !configStore.isConfigured()) {
                return;
            }

            Optional<String> authHeader = Optional.ofNullable(ctx.header("Authorization"));
            if (authHeader.isEmpty() || !authHeader.get().startsWith("Basic ")) {
                ctx.header("WWW-Authenticate", "Basic").status(HttpStatus.UNAUTHORIZED);
                return;
            }

            String base64Credentials = authHeader.get().substring("Basic ".length());
            String credentials = new String(Base64.getDecoder().decode(base64Credentials));
            final StringTokenizer tokenizer = new StringTokenizer(credentials, ":");
            if (tokenizer.countTokens() < 2) {
                ctx.header("WWW-Authenticate", "Basic").status(HttpStatus.UNAUTHORIZED);
                return;
            }
            final String username = tokenizer.nextToken();
            final String password = tokenizer.nextToken();

            Configuration config = configStore.getConfiguration().orElseThrow();
            if (!config.getAdminUsername().equals(username) || !BCrypt.checkpw(password, config.getAdminPasswordHash())) {
                ctx.header("WWW-Authenticate", "Basic").status(HttpStatus.UNAUTHORIZED);
            }
        });


        // Setup routes
        app.get("/setup", ctx -> {
            if (configStore.isConfigured()) {
                ctx.redirect("/", HttpStatus.FOUND);
                return;
            }
            // In a real app, you'd use a template engine. For simplicity, we'll inline it.
            ctx.html(new String(DaemonDeckApp.class.getResourceAsStream("/Setup.html").readAllBytes()));
        });

        app.post("/setup", ctx -> {
            if (configStore.isConfigured()) {
                ctx.status(HttpStatus.FORBIDDEN).result("Already configured.");
                return;
            }
            String adminUsername = ctx.formParam("adminUsername");
            String adminPassword = ctx.formParam("adminPassword");
            String steamCmdPath = ctx.formParam("steamCmdPath");

            if (adminUsername == null || adminPassword == null || steamCmdPath == null ||
                adminUsername.isBlank() || adminPassword.isBlank() || steamCmdPath.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST).result("All fields are required.");
                return;
            }

            String hashedPassword = BCrypt.hashpw(adminPassword, BCrypt.gensalt());

            Configuration newConfig = new Configuration();
            newConfig.setAdminUsername(adminUsername);
            newConfig.setAdminPasswordHash(hashedPassword);
            newConfig.setSteamCmdPath(steamCmdPath);
            newConfig.setAllowedIps(Collections.emptySet());

            configStore.saveConfiguration(newConfig);

            ctx.redirect("/", HttpStatus.FOUND);
        });

        // Authenticated routes
        app.get("/", ctx -> ctx.html("<h1>Welcome to DaemonDeck!</h1>"));

        // Add a shutdown hook to close the database
        Runtime.getRuntime().addShutdownHook(new Thread(configStore::close));
    }
}