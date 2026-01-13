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
import java.util.Optional;
import java.util.StringTokenizer;

public class DaemonDeckApp {

    private final ConfigStore configStore;
    private final DiscordService discordService;
    public final io.javalin.Javalin app;

    public DaemonDeckApp() {
        this(ConfigStore.getInstance(), new DiscordService(ConfigStore.getInstance()));
    }

    public DaemonDeckApp(ConfigStore configStore, DiscordService discordService) {
        this.configStore = configStore;
        this.discordService = discordService;

        app = Javalin.create(config -> {
            config.fileRenderer(new JavalinThymeleaf());
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
                ctx.header("WWW-authenticate", "Basic").status(HttpStatus.UNAUTHORIZED);
            }
        });


        // Setup routes
        app.get("/setup", ctx -> {
            if (configStore.isConfigured()) {
                ctx.redirect("/", HttpStatus.FOUND);
                return;
            }
            ctx.render("templates/Setup.html", Map.of("title", "DaemonDeck Setup"));
        });

        app.post("/setup", ctx -> {
            if (configStore.isConfigured()) {
                ctx.status(HttpStatus.FORBIDDEN).result("Already configured.");
                return;
            }
            String adminUsername = ctx.formParam("adminUsername");
            String adminPassword = ctx.formParam("adminPassword");
            String steamCmdPath = ctx.formParam("steamCmdPath");
            String sessionTimeoutSecondsParam = ctx.formParam("sessionTimeoutSeconds");

            if (adminUsername == null || adminPassword == null || steamCmdPath == null || sessionTimeoutSecondsParam == null ||
                adminUsername.isBlank() || adminPassword.isBlank() || steamCmdPath.isBlank() || sessionTimeoutSecondsParam.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST).result("All fields are required.");
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
        app.get("/", ctx -> {
            configStore.getConfiguration().ifPresent(config -> {
                ctx.render("templates/index.html", Map.of(
                    "title", "Welcome to GameDaemonDeck",
                    "sessionTimeoutSeconds", config.getSessionTimeoutSeconds()
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
    }

    public static void main(String[] args) {
        DaemonDeckApp daemonDeckApp = new DaemonDeckApp();
        daemonDeckApp.start(7070);
    }

    public void start(int port) {
        app.start(port);
    }

    public void stop() {
        app.stop();
    }
}
