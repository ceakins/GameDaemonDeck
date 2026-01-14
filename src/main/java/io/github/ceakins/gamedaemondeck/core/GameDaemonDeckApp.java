package io.github.ceakins.gamedaemondeck.core;

import io.github.ceakins.gamedaemondeck.db.ConfigStore;
import io.github.ceakins.gamedaemondeck.db.Configuration;
import io.github.ceakins.gamedaemondeck.db.DiscordBot;
import io.github.ceakins.gamedaemondeck.db.DiscordWebhook;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.rendering.template.JavalinThymeleaf;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.StringTokenizer;

public class GameDaemonDeckApp {

    private final ConfigStore configStore;
    private final DiscordService discordService;
    public final io.javalin.Javalin app;

    public GameDaemonDeckApp() {
        this(ConfigStore.getInstance(), new DiscordService(ConfigStore.getInstance()));
    }

    public GameDaemonDeckApp(ConfigStore configStore, DiscordService discordService) {
        this.configStore = configStore;
        this.discordService = discordService;

        app = Javalin.create(config -> {
            config.fileRenderer(new JavalinThymeleaf());
            config.staticFiles.add(staticFileConfig -> {
                staticFileConfig.directory = "/static"; // Serves files from src/main/resources/static
                staticFileConfig.hostedPath = "/static"; // Hosted at /static
                staticFileConfig.precompress = false; // Optional: disable precompress
                staticFileConfig.headers = Map.of("Cache-Control", "max-age=600"); // Optional: add cache headers
            });
        });

        // Add error handlers directly on the app instance
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.render("templates/error.html", Map.of(
                "title", "Game Daemon Deck - Error",
                "header", "An Unexpected Error Occurred",
                "message", "Something went wrong: " + e.getMessage(),
                "returnUrl", "/",
                "returnText", "Dashboard"
            ));
        });

        app.error(HttpStatus.FORBIDDEN.getCode(), ctx -> { // Corrected: .getCode()
            ctx.render("templates/error.html", Map.of(
                "title", "Game Daemon Deck - Access Denied",
                "header", "Access Denied",
                "message", "You do not have permission to perform this action.",
                "returnUrl", "/",
                "returnText", "Dashboard"
            ));
        });

        app.error(HttpStatus.NOT_FOUND.getCode(), ctx -> { // Corrected: .getCode()
            ctx.render("templates/error.html", Map.of(
                "title", "Game Daemon Deck - Not Found",
                "header", "Page Not Found",
                "message", "The page you requested could not be found.",
                "returnUrl", "/",
                "returnText", "Dashboard"
            ));
        });

        // Start all bots
        discordService.startAllBots();

        // Add a shutdown hook to stop all bots and close the database
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            discordService.stopAllBots();
            configStore.close();
        }));


        // Before-filter to redirect to /setup if not configured
        app.before(ctx -> {
            if (!ctx.path().equals("/setup") && !configStore.isConfigured()) {
                ctx.redirect("/setup", HttpStatus.FOUND);
            }
        });

        // Session-based authentication before-filter
        app.before(ctx -> {
            // Exclude /setup, /login from session check
            if (ctx.path().equals("/setup") || ctx.path().equals("/login")) {
                return;
            }

            // If not configured, always redirect to setup
            if (!configStore.isConfigured()) {
                ctx.redirect("/setup", HttpStatus.FOUND);
                return;
            }

            // Check for session attribute
            String username = ctx.sessionAttribute("username");
            if (username == null) {
                // No active session, redirect to login with a message
                ctx.redirect("/login?message=Your session has expired or you need to log in.", HttpStatus.FOUND);
            }
        });

        // Setup routes
        app.get("/setup", ctx -> {
            if (configStore.isConfigured()) {
                ctx.redirect("/", HttpStatus.FOUND);
                return;
            }
            ctx.render("templates/Setup.html", Map.of("title", "GameDaemonDeck Setup"));
        });

        app.post("/setup", ctx -> {
            if (configStore.isConfigured()) {
                ctx.render("templates/error.html", Map.of(
                    "title", "Game Daemon Deck - Error",
                    "header", "Configuration Error",
                    "message", "The application is already configured. Please navigate to the dashboard.",
                    "returnUrl", "/",
                    "returnText", "Dashboard"
                ));
                return;
            }
            String adminUsername = ctx.formParam("adminUsername");
            String adminPassword = ctx.formParam("adminPassword");
            String steamCmdPath = ctx.formParam("steamCmdPath");
            String sessionTimeoutSecondsParam = ctx.formParam("sessionTimeoutSeconds");

            if (adminUsername == null || adminPassword == null || steamCmdPath == null || sessionTimeoutSecondsParam == null ||
                adminUsername.isBlank() || adminPassword.isBlank() || steamCmdPath.isBlank() || sessionTimeoutSecondsParam.isBlank()) {
                ctx.render("templates/error.html", Map.of(
                    "title", "Game Daemon Deck - Error",
                    "header", "Setup Error",
                    "message", "All fields are required for initial setup.",
                    "returnUrl", "/setup",
                    "returnText", "Setup Page"
                ));
                return;
            }

            int sessionTimeoutSeconds;
            try {
                sessionTimeoutSeconds = Integer.parseInt(sessionTimeoutSecondsParam);
                if (sessionTimeoutSeconds <= 0) {
                    ctx.render("templates/error.html", Map.of(
                        "title", "Game Daemon Deck - Error",
                        "header", "Setup Error",
                        "message", "Session Timeout must be a positive integer.",
                        "returnUrl", "/setup",
                        "returnText", "Setup Page"
                    ));
                    return;
                }
            } catch (NumberFormatException e) {
                ctx.render("templates/error.html", Map.of(
                    "title", "Game Daemon Deck - Error",
                    "header", "Setup Error",
                    "message", "Session Timeout must be a valid number.",
                    "returnUrl", "/setup",
                    "returnText", "Setup Page"
                ));
                return;
            }

            String hashedPassword = BCrypt.hashpw(adminPassword, BCrypt.gensalt());

            Configuration newConfig = new Configuration();
            newConfig.setAdminUsername(adminUsername);
            newConfig.setAdminPasswordHash(hashedPassword);
            newConfig.setSteamCmdPath(steamCmdPath);
            newConfig.setAllowedIps(Collections.emptyList());
            newConfig.setSessionTimeoutSeconds(sessionTimeoutSeconds);

            configStore.saveConfiguration(newConfig);

            ctx.redirect("/", HttpStatus.FOUND);
        });

        // Authenticated routes
        // Authenticated routes
        app.get("/", ctx -> {
            configStore.getConfiguration().ifPresent(config -> {
                ctx.render("templates/index.html", Map.of(
                    "title", "Game Daemon Deck - Dashboard"
                    // Other config values are not directly used in the main dashboard view, so remove them
                ));
            });
        });

        // Discord Webhook routes
        app.get("/api/discord/webhooks", ctx -> ctx.json(discordService.getAllWebhooks()));
        app.post("/api/discord/webhooks", ctx -> {
            DiscordWebhook webhook = ctx.bodyAsClass(DiscordWebhook.class);
            discordService.saveWebhook(webhook);
            ctx.status(HttpStatus.CREATED);
        });
        app.delete("/api/discord/webhooks/{name}", ctx -> {
            discordService.deleteWebhook(ctx.pathParam("name"));
            ctx.status(HttpStatus.NO_CONTENT);
        });

        // Discord Bot routes
        app.get("/api/discord/bots", ctx -> ctx.json(discordService.getAllBots()));
        app.post("/api/discord/bots", ctx -> {
            DiscordBot bot = ctx.bodyAsClass(DiscordBot.class);
            discordService.saveBot(bot);
            discordService.startBot(bot);
            ctx.status(HttpStatus.CREATED);
        });
        app.delete("/api/discord/bots/{name}", ctx -> {
            discordService.deleteBot(ctx.pathParam("name"));
            ctx.status(HttpStatus.NO_CONTENT);
        });

        // API for updating configuration
        app.post("/api/config", ctx -> {
            String steamCmdPath = ctx.formParam("steamCmdPath");
            String sessionTimeoutSecondsParam = ctx.formParam("sessionTimeoutSeconds");

            if (steamCmdPath == null || steamCmdPath.isBlank() ||
                sessionTimeoutSecondsParam == null || sessionTimeoutSecondsParam.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST).result("SteamCMD Path and Session Timeout are required.");
                return;
            }

            int sessionTimeoutSeconds;
            try {
                sessionTimeoutSeconds = Integer.parseInt(sessionTimeoutSecondsParam);
                if (sessionTimeoutSeconds <= 0) {
                    ctx.status(HttpStatus.BAD_REQUEST).result("Session Timeout must be a positive integer.");
                    return;
                }
            } catch (NumberFormatException e) {
                ctx.status(HttpStatus.BAD_REQUEST).result("Session Timeout must be a valid number.");
                return;
            }

            Optional<Configuration> configOptional = configStore.getConfiguration();
            if (configOptional.isEmpty()) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Configuration not found.");
                return;
            }

            Configuration config = configOptional.get();
            config.setSteamCmdPath(steamCmdPath);
            config.setSessionTimeoutSeconds(sessionTimeoutSeconds);
            configStore.saveConfiguration(config);

            ctx.status(HttpStatus.OK).result("Configuration updated successfully.");
        });

        // Settings page
        app.get("/settings", ctx -> {
            configStore.getConfiguration().ifPresent(config -> {
                ctx.render("templates/settings.html", Map.of(
                    "title", "Game Daemon Deck - Settings",
                    "sessionTimeoutSeconds", config.getSessionTimeoutSeconds(),
                    "steamCmdPath", config.getSteamCmdPath()
                ));
            });
        });

        app.get("/login", ctx -> {
            String message = ctx.queryParam("message");
            Map<String, Object> model = new HashMap<>();
            model.put("title", "Login");
            if (message != null) {
                model.put("message", message);
            }
            ctx.render("templates/login.html", model);
        });

        app.post("/login", ctx -> {
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");

            Optional<Configuration> configOptional = configStore.getConfiguration();

            if (configOptional.isEmpty()) {
                ctx.redirect("/setup", HttpStatus.FOUND); // Should not happen if setup before filter works
                return;
            }

            Configuration config = configOptional.get();

            if (username != null && password != null &&
                config.getAdminUsername().equals(username) &&
                BCrypt.checkpw(password, config.getAdminPasswordHash())) {
                // Successful login
                ctx.sessionAttribute("username", username);
                ctx.redirect("/", HttpStatus.FOUND);
            } else {
                // Failed login
                ctx.redirect("/login?message=Invalid username or password.", HttpStatus.FOUND);
            }
        });

        app.get("/logout", ctx -> {
            // Invalidate session
            ctx.sessionAttribute("username", null);
            ctx.redirect("/login?message=You have been successfully logged out.", HttpStatus.FOUND);
        });
    }

    public static void main(String[] args) {
        GameDaemonDeckApp gameDaemonDeckApp = new GameDaemonDeckApp();
        gameDaemonDeckApp.start(7070);
    }

    public void start(int port) {
        app.start(port);
    }

    public void stop() {
        app.stop();
    }
}
