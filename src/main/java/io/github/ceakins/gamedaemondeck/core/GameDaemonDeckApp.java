package io.github.ceakins.gamedaemondeck.core;

import io.github.ceakins.gamedaemondeck.db.ConfigStore;
import io.github.ceakins.gamedaemondeck.db.Configuration;
import io.github.ceakins.gamedaemondeck.db.DiscordBot;
import io.github.ceakins.gamedaemondeck.db.DiscordWebhook;
import io.github.ceakins.gamedaemondeck.db.GameServer;
import io.github.ceakins.gamedaemondeck.plugins.GamePlugin;
import io.github.ceakins.gamedaemondeck.plugins.LogHighlighter;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.rendering.template.JavalinThymeleaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GameDaemonDeckApp {

    private final ConfigStore configStore;
    private final DiscordService discordService;
    private final PluginManager pluginManager;
    public final io.javalin.Javalin app;
    private static final Logger logger = LoggerFactory.getLogger(GameDaemonDeckApp.class);
    private final Map<String, Process> runningServerProcesses = new ConcurrentHashMap<>();
    // Store logs for each server: Map<ServerName, Queue<LogLine>>
    private final Map<String, Queue<String>> serverLogs = new ConcurrentHashMap<>();
    private static final int MAX_LOG_LINES = 1000;

    public GameDaemonDeckApp() {
        this(ConfigStore.getInstance(), new DiscordService(ConfigStore.getInstance()), new PluginManager());
    }

    public GameDaemonDeckApp(ConfigStore configStore, DiscordService discordService, PluginManager pluginManager) {
        this.configStore = configStore;
        this.discordService = discordService;
        this.pluginManager = pluginManager;

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
            logger.error("{}", e);
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
                // If API request, return 401
                if (ctx.path().startsWith("/api/")) {
                    ctx.status(HttpStatus.UNAUTHORIZED);
                } else {
                    // No active session, redirect to login with a message
                    ctx.redirect("/login?message=Your session has expired or you need to log in.", HttpStatus.FOUND);
                }
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
                    ctx.status(HttpStatus.BAD_REQUEST).render("templates/error.html", Map.of(
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
                    "title", "Game Daemon Deck - Dashboard",
                    "servers", configStore.getServers(),
                    "plugins", pluginManager.getPlugins()
                ));
            });
        });

        app.post("/servers", ctx -> {
            String serverName = ctx.formParam("serverName");
            if (configStore.getServers().stream().anyMatch(s -> s.getName().equals(serverName))) {
                ctx.redirect("/?error=duplicate_server&serverName=" + serverName + "&appId=" + ctx.formParam("appId") + "&pluginName=" + ctx.formParam("pluginName") + "&headerColor=" + ctx.formParam("headerColor") + "&fontColor=" + ctx.formParam("fontColor"));
                return;
            }

            GameServer newServer = new GameServer();
            newServer.setName(serverName);
            newServer.setAppId(ctx.formParam("appId"));
            newServer.setPluginName(ctx.formParam("pluginName"));
            newServer.setHeaderColor(ctx.formParam("headerColor"));
            newServer.setFontColor(ctx.formParam("fontColor"));
            configStore.saveServer(newServer);
            ctx.redirect("/");
        });

        app.post("/servers/config", ctx -> {
            String serverName = ctx.formParam("serverName");
            String serverPath = ctx.formParam("serverPath");
            String commandLine = ctx.formParam("commandLine");

            logger.info("Saving server config for {} with path {} and command line {}",
                    serverName,
                    serverPath,
                    commandLine);

            configStore.getServers().stream()
                .filter(s -> s.getName().equals(serverName))
                .findFirst()
                .ifPresent(server -> {
                    server.setServerPath(serverPath);
                    server.setCommandLine(commandLine);
                    configStore.saveServer(server);
                });

            ctx.redirect("/");
        });

        app.post("/servers/{name}/start", ctx -> {
            String serverName = ctx.pathParam("name");
            Optional<GameServer> serverOpt = configStore.getServers().stream()
                .filter(s -> s.getName().equals(serverName))
                .findFirst();

            if (serverOpt.isPresent()) {
                GameServer server = serverOpt.get();
                if (server.isRunning()) {
                    ctx.status(HttpStatus.BAD_REQUEST).result("Server is already running");
                    return;
                }

                if (server.getServerPath() == null || server.getServerPath().isBlank()) {
                    ctx.status(HttpStatus.BAD_REQUEST).result("Server path not configured");
                    return;
                }

                try {
                    List<String> command = new ArrayList<>();
                    
                    // Clean the server path (remove quotes if present)
                    String serverPath = server.getServerPath();
                    if (serverPath.startsWith("\"") && serverPath.endsWith("\"")) {
                        serverPath = serverPath.substring(1, serverPath.length() - 1);
                    }
                    command.add(serverPath);

                    if (server.getCommandLine() != null && !server.getCommandLine().isBlank()) {
                        // Use regex to split arguments respecting quotes
                        Matcher matcher = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(server.getCommandLine());
                        while (matcher.find()) {
                            String arg = matcher.group(1);
                            // Remove surrounding quotes if present
                            if (arg.startsWith("\"") && arg.endsWith("\"")) {
                                arg = arg.substring(1, arg.length() - 1);
                            }
                            command.add(arg);
                        }
                    }

                    logger.info("Starting server {} with command: {}", serverName, command);

                    ProcessBuilder pb = new ProcessBuilder(command);
                    // Set working directory to the server executable's directory
                    File serverFile = new File(serverPath);
                    if (serverFile.getParentFile() != null) {
                        pb.directory(serverFile.getParentFile());
                    }
                    
                    // Redirect error stream to output stream so we can read both
                    pb.redirectErrorStream(true);
                    
                    Process process = pb.start();
                    long pid = process.pid();
                    
                    server.setRunning(true);
                    server.setPid(pid);
                    configStore.saveServer(server);
                    runningServerProcesses.put(server.getName(), process);
                    
                    // Initialize log queue for this server
                    serverLogs.put(server.getName(), new ConcurrentLinkedQueue<>());

                    // Start a thread to read and log the server's output
                    new Thread(() -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                // Store log line in memory
                                Queue<String> logs = serverLogs.get(serverName);
                                if (logs != null) {
                                    logs.add(line);
                                    if (logs.size() > MAX_LOG_LINES) {
                                        logs.poll(); // Remove oldest
                                    }
                                }
                                // Removed: logger.info("[{}] {}", serverName, line); // Don't log to main app log anymore
                            }
                        } catch (IOException e) {
                            logger.error("Error reading output from server {}", serverName, e);
                        }
                    }).start();

                    // Monitor process exit in a separate thread
                    new Thread(() -> {
                        try {
                            int exitCode = process.waitFor();
                            logger.info("Server {} exited with code {}", server.getName(), exitCode);
                            server.setRunning(false);
                            server.setPid(null);
                            configStore.saveServer(server);
                            runningServerProcesses.remove(server.getName());
                            // Keep logs for a while or clear them? For now, keep them so user can see why it crashed.
                        } catch (InterruptedException e) {
                            logger.error("Error waiting for server process", e);
                        }
                    }).start();

                    ctx.status(HttpStatus.OK).result("Server started with PID " + pid);
                } catch (IOException e) {
                    logger.error("Failed to start server {}", serverName, e);
                    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to start server: " + e.getMessage());
                }
            } else {
                ctx.status(HttpStatus.NOT_FOUND).result("Server not found");
            }
        });

        app.post("/servers/{name}/stop", ctx -> {
            String serverName = ctx.pathParam("name");
            Optional<GameServer> serverOpt = configStore.getServers().stream()
                .filter(s -> s.getName().equals(serverName))
                .findFirst();

            if (serverOpt.isPresent()) {
                GameServer server = serverOpt.get();
                if (!server.isRunning()) {
                    ctx.status(HttpStatus.BAD_REQUEST).result("Server is not running");
                    return;
                }

                // Placeholder for graceful shutdown logic (Telnet/RCON)
                // For now, we'll just kill the process if we have the handle, or use system kill command
                
                Process process = runningServerProcesses.get(serverName);
                if (process != null) {
                    process.destroy(); // Try graceful termination first
                } else if (server.getPid() != null) {
                    // If we don't have the Process object (e.g. after restart), try to kill by PID
                    try {
                        String os = System.getProperty("os.name").toLowerCase();
                        if (os.contains("win")) {
                            Runtime.getRuntime().exec("taskkill /F /PID " + server.getPid());
                        } else {
                            Runtime.getRuntime().exec("kill -9 " + server.getPid());
                        }
                    } catch (IOException e) {
                        logger.error("Failed to kill process by PID", e);
                    }
                }

                // Update state immediately for UI feedback, though the process watcher thread should also handle it
                server.setRunning(false);
                server.setPid(null);
                configStore.saveServer(server);
                runningServerProcesses.remove(serverName);

                ctx.status(HttpStatus.OK).result("Server stopped");
            } else {
                ctx.status(HttpStatus.NOT_FOUND).result("Server not found");
            }
        });

        app.get("/api/servers/status", ctx -> {
            List<Map<String, Object>> statuses = configStore.getServers().stream()
                .map(server -> {
                    Map<String, Object> status = new HashMap<>();
                    status.put("name", server.getName());
                    status.put("running", server.isRunning());
                    status.put("pid", server.getPid());
                    // Check if configured
                    boolean configured = server.getServerPath() != null && !server.getServerPath().isBlank();
                    status.put("configured", configured);
                    return status;
                })
                .collect(Collectors.toList());
            ctx.json(statuses);
        });
        
        app.get("/api/servers/{name}/logs", ctx -> {
            String serverName = ctx.pathParam("name");
            Queue<String> logs = serverLogs.get(serverName);
            if (logs != null) {
                ctx.json(logs);
            } else {
                ctx.json(Collections.emptyList());
            }
        });

        app.get("/api/servers/{name}/log-highlighters", ctx -> {
            String serverName = ctx.pathParam("name");
            Optional<GameServer> serverOpt = configStore.getServers().stream()
                .filter(s -> s.getName().equals(serverName))
                .findFirst();

            if (serverOpt.isPresent()) {
                GameServer server = serverOpt.get();
                GamePlugin plugin = pluginManager.getPlugin(server.getPluginName());
                if (plugin != null) {
                    ctx.json(plugin.getLogHighlighters());
                } else {
                    ctx.json(Collections.emptyList());
                }
            } else {
                ctx.status(HttpStatus.NOT_FOUND).result("Server not found");
            }
        });

        app.get("/api/plugins/{name}/server-config-fields", ctx -> {
            String pluginName = ctx.pathParam("name");
            GamePlugin plugin = pluginManager.getPlugin(pluginName);
            if (plugin != null) {
                ctx.json(plugin.getServerConfigFields());
            } else {
                ctx.status(HttpStatus.NOT_FOUND).result("Plugin not found");
            }
        });

        app.post("/api/servers/{name}/create-config", ctx -> {
            String serverName = ctx.pathParam("name");
            Optional<GameServer> serverOpt = configStore.getServers().stream()
                .filter(s -> s.getName().equals(serverName))
                .findFirst();

            if (serverOpt.isPresent()) {
                GameServer server = serverOpt.get();
                GamePlugin plugin = pluginManager.getPlugin(server.getPluginName());
                if (plugin != null) {
                    Map<String, String> values = ctx.bodyAsClass(Map.class);
                    String fileName = values.get("fileName");
                    if (fileName == null || fileName.isBlank()) {
                        ctx.status(HttpStatus.BAD_REQUEST).result("Filename is required");
                        return;
                    }

                    // Use serverPath from request body if available, otherwise fallback to stored
                    String serverPath = values.get("serverPath");
                    if (serverPath == null || serverPath.isBlank()) {
                        serverPath = server.getServerPath();
                    }

                    if (serverPath == null || serverPath.isBlank()) {
                        ctx.status(HttpStatus.BAD_REQUEST).result("Server path must be set first");
                        return;
                    }

                    if (serverPath.startsWith("\"") && serverPath.endsWith("\"")) {
                        serverPath = serverPath.substring(1, serverPath.length() - 1);
                    }
                    Path serverDir = Paths.get(serverPath).getParent();

                    try {
                        plugin.generateConfigFile(values, serverDir, fileName);
                        
                        // Return the full path to the created file
                        String fullPath = serverDir.resolve(fileName.endsWith(".xml") ? fileName : fileName + ".xml").toAbsolutePath().toString();
                        ctx.json(Map.of("path", fullPath));
                    } catch (IOException e) {
                        logger.error("Failed to create config file", e);
                        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to create config file: " + e.getMessage());
                    }
                } else {
                    ctx.status(HttpStatus.NOT_FOUND).result("Plugin not found");
                }
            } else {
                ctx.status(HttpStatus.NOT_FOUND).result("Server not found");
            }
        });

        app.post("/api/servers/{name}/parse-config", ctx -> {
            String serverName = ctx.pathParam("name");
            Optional<GameServer> serverOpt = configStore.getServers().stream()
                .filter(s -> s.getName().equals(serverName))
                .findFirst();

            if (serverOpt.isPresent()) {
                GameServer server = serverOpt.get();
                GamePlugin plugin = pluginManager.getPlugin(server.getPluginName());
                if (plugin != null) {
                    // Read parameters from request body if available, otherwise use stored server config
                    Map<String, String> requestBody = ctx.bodyAsClass(Map.class);
                    Map<String, String> params = new HashMap<>();
                    
                    if (requestBody != null && requestBody.containsKey("commandLine")) {
                        params.put("commandLine", requestBody.get("commandLine"));
                    } else {
                        params.put("commandLine", server.getCommandLine());
                    }
                    
                    Path configPath = plugin.getConfigFileFromParams(params);
                    if (configPath != null) {
                        // If path is relative, resolve against server directory
                        if (!configPath.isAbsolute()) {
                            String serverPath = server.getServerPath();
                            if (serverPath.startsWith("\"") && serverPath.endsWith("\"")) {
                                serverPath = serverPath.substring(1, serverPath.length() - 1);
                            }
                            Path serverDir = Paths.get(serverPath).getParent();
                            configPath = serverDir.resolve(configPath);
                        }

                        if (Files.exists(configPath)) {
                            try {
                                Map<String, String> values = plugin.parseConfigFile(configPath);
                                // Add filename to values so frontend can populate it
                                String fileName = configPath.getFileName().toString();
                                if (fileName.endsWith(".xml")) {
                                    fileName = fileName.substring(0, fileName.length() - 4);
                                }
                                values.put("fileName", fileName);
                                ctx.json(values);
                            } catch (IOException e) {
                                logger.error("Failed to parse config file", e);
                                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to parse config file: " + e.getMessage());
                            }
                        } else {
                            ctx.status(HttpStatus.NOT_FOUND).result("Config file not found at: " + configPath);
                        }
                    } else {
                        ctx.status(HttpStatus.BAD_REQUEST).result("No config file found in server parameters");
                    }
                } else {
                    ctx.status(HttpStatus.NOT_FOUND).result("Plugin not found");
                }
            } else {
                ctx.status(HttpStatus.NOT_FOUND).result("Server not found");
            }
        });

        app.get("/servers/config-fields", ctx -> {
            String pluginName = ctx.queryParam("pluginName");
            if (pluginName != null) {
                pluginManager.getPlugins().stream()
                    .filter(p -> p.getName().equals(pluginName))
                    .findFirst()
                    .ifPresent(plugin -> ctx.json(plugin.getConfigFields()));
            } else {
                ctx.json(Collections.emptyList());
            }
        });

        app.get("/api/files", ctx -> {
            String pathParam = ctx.queryParam("path");
            Path path;
            if (pathParam == null || pathParam.isBlank() || pathParam.equals(".")) {
                path = Paths.get(".").toAbsolutePath().normalize();
            } else {
                path = Paths.get(pathParam).toAbsolutePath().normalize();
            }

            if (!Files.exists(path) || !Files.isDirectory(path)) {
                // Fallback to current directory if invalid
                path = Paths.get(".").toAbsolutePath().normalize();
            }

            List<Map<String, Object>> files = new ArrayList<>();
            // Add parent directory entry if not root
            if (path.getParent() != null) {
                files.add(Map.of(
                    "name", "..",
                    "path", path.getParent().toString(),
                    "isDirectory", true
                ));
            }

            try (Stream<Path> stream = Files.list(path)) {
                List<Map<String, Object>> entries = stream
                    .map(p -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("name", p.getFileName().toString());
                        map.put("path", p.toAbsolutePath().toString());
                        map.put("isDirectory", Files.isDirectory(p));
                        return map;
                    })
                    .collect(Collectors.toList());
                files.addAll(entries);
            } catch (Exception e) {
                logger.error("Error listing files", e);
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                return;
            }

            ctx.json(Map.of(
                "currentPath", path.toString(),
                "files", files
            ));
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
            // Check if user is already logged in (has an active session)
            if (ctx.sessionAttribute("username") != null) {
                ctx.redirect("/", HttpStatus.FOUND); // Redirect to dashboard if logged in
                return;
            }

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
