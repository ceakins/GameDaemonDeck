package io.github.ceakins.gamedaemondeck.plugins;

import io.github.ceakins.gamedaemondeck.db.GameServer;
import io.github.ceakins.gamedaemondeck.util.TelnetClientManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SevenDaysToDiePlugin implements GamePlugin {

    @Override
    public String getName() {
        return "7 Days to Die";
    }

    @Override
    public void startServer(Path serverPath) throws IOException {
        // Placeholder for starting the 7 Days to Die server
    }

    @Override
    public void stopServer() throws IOException {
        // Placeholder for stopping the 7 Days to Die server
    }

    @Override
    public String getBotName() {
        return null; // Or a specific bot name if applicable
    }

    @Override
    public List<ConfigField> getConfigFields() {
        return Arrays.asList(
                new ConfigField("serverPath", "Server Path", "text", ""),
                new ConfigField("commandLine", "Command Line Arguments", "text", "-batchmode -nographics -dedicated -configfile=<locationnotset>")
        );
    }

    @Override
    public List<LogHighlighter> getLogHighlighters() {
        return Arrays.asList(
            new LogHighlighter(".*INF.*", "#28a745"), // Green for Info
            new LogHighlighter(".*WRN.*", "#ffc107"), // Yellow for Warning (short)
            new LogHighlighter(".*Warning.*", "#ffc107"), // Yellow for Warning (long)
            new LogHighlighter(".*ERR.*", "#dc3545"),  // Red for Error
            new LogHighlighter(".*Error.*", "#dc3545")   // Red for Error (long)
        );
    }

    @Override
    public List<ServerConfigField> getServerConfigFields() {
        return Arrays.asList(
            // General Server Settings
            new ServerConfigField("ServerName", "Server Name", "text", "My Game Host", "Whatever you want the name of the server to be."),
            new ServerConfigField("ServerDescription", "Server Description", "text", "A 7 Days to Die server", "Shown in the server browser."),
            new ServerConfigField("ServerWebsiteURL", "Website URL", "text", "", "Website URL for the server"),
            new ServerConfigField("ServerPassword", "Server Password", "password", "", "Password to gain entry to the server"),
            new ServerConfigField("ServerLoginConfirmationText", "Login Confirmation Text", "text", "", "Message seen during joining"),
            new ServerConfigField("Region", "Region", "select", "NorthAmericaEast", "The region this server is in.", Arrays.asList("NorthAmericaEast", "NorthAmericaWest", "CentralAmerica", "SouthAmerica", "Europe", "Russia", "Asia", "MiddleEast", "Africa", "Oceania")),
            new ServerConfigField("Language", "Language", "text", "English", "Primary language for players"),

            // Networking
            new ServerConfigField("ServerPort", "Server Port", "number", "26900", "Port you want the server to listen on."),
            new ServerConfigField("ServerVisibility", "Visibility", "select", "2", "2 = public, 1 = friends only, 0 = not listed", Arrays.asList("0", "1", "2")),
            new ServerConfigField("ServerDisabledNetworkProtocols", "Disabled Protocols", "text", "SteamNetworking", "Networking protocols that should not be used"),
            new ServerConfigField("ServerMaxWorldTransferSpeedKiBs", "Max World Transfer Speed", "number", "512", "Maximum speed in kiB/s"),

            // Slots
            new ServerConfigField("ServerMaxPlayerCount", "Max Players", "number", "8", "Maximum Concurrent Players"),
            new ServerConfigField("ServerReservedSlots", "Reserved Slots", "number", "0", "Slots reserved for specific permission level"),
            new ServerConfigField("ServerReservedSlotsPermission", "Reserved Slots Permission", "number", "100", "Required permission level to use reserved slots"),
            new ServerConfigField("ServerAdminSlots", "Admin Slots", "number", "0", "Slots reserved for admins"),
            new ServerConfigField("ServerAdminSlotsPermission", "Admin Slots Permission", "number", "0", "Required permission level to use admin slots"),

            // Admin Interfaces
            new ServerConfigField("WebDashboardEnabled", "Enable Web Dashboard", "boolean", "false", "Enable/disable the web dashboard"),
            new ServerConfigField("WebDashboardPort", "Web Dashboard Port", "number", "8080", "Port of the web dashboard"),
            new ServerConfigField("WebDashboardUrl", "Web Dashboard URL", "text", "", "External URL to the web dashboard"),
            new ServerConfigField("EnableMapRendering", "Enable Map Rendering", "boolean", "false", "Enable/disable rendering of the map to tile images"),
            new ServerConfigField("TelnetEnabled", "Enable Telnet", "boolean", "true", "Enable/Disable the telnet"),
            new ServerConfigField("TelnetPort", "Telnet Port", "number", "8081", "Port of the telnet server"),
            new ServerConfigField("TelnetPassword", "Telnet Password", "password", "", "Password to gain entry to telnet interface"),
            new ServerConfigField("TelnetFailedLoginLimit", "Telnet Failed Login Limit", "number", "10", "Block after this many wrong passwords"),
            new ServerConfigField("TelnetFailedLoginsBlocktime", "Telnet Block Time", "number", "10", "How long will the block persist (in seconds)"),
            new ServerConfigField("TerminalWindowEnabled", "Enable Terminal Window", "boolean", "true", "Show a terminal window for log output (Windows only)"),

            // Folder and File Locations
            new ServerConfigField("AdminFileName", "Admin File Name", "text", "serveradmin.xml", "Server admin file name"),
            // UserDataFolder commented out in example, skipping or adding as optional text

            // Other Technical Settings
            new ServerConfigField("ServerAllowCrossplay", "Allow Crossplay", "boolean", "false", "Enables/Disables crossplay"),
            new ServerConfigField("EACEnabled", "Enable EAC", "boolean", "true", "Enables/Disables EasyAntiCheat"),
            new ServerConfigField("IgnoreEOSSanctions", "Ignore EOS Sanctions", "boolean", "false", "Ignore EOS sanctions when allowing players to join"),
            new ServerConfigField("HideCommandExecutionLog", "Hide Command Log", "select", "0", "0=show all, 1=hide telnet, 2=hide remote, 3=hide all", Arrays.asList("0", "1", "2", "3")),
            new ServerConfigField("MaxUncoveredMapChunksPerPlayer", "Max Uncovered Map Chunks", "number", "131072", "Override how many chunks can be uncovered"),
            new ServerConfigField("PersistentPlayerProfiles", "Persistent Profiles", "boolean", "false", "If true they will join with the last profile they joined with"),
            new ServerConfigField("MaxChunkAge", "Max Chunk Age", "number", "-1", "Days before chunk reset"),
            new ServerConfigField("SaveDataLimit", "Save Data Limit", "number", "-1", "Max disk space for save game (MB)"),

            // Gameplay - World
            new ServerConfigField("GameWorld", "Game World", "select", "Navezgane", "World to load", Arrays.asList("Navezgane", "RWG", "Pregen06k01", "Pregen06k02", "Pregen08k01", "Pregen08k02")),
            new ServerConfigField("WorldGenSeed", "World Gen Seed", "text", "MyGame", "Seed for RWG"),
            new ServerConfigField("WorldGenSize", "World Gen Size", "select", "6144", "Width/Height of RWG world", Arrays.asList("6144", "8192", "10240")),
            new ServerConfigField("GameName", "Game Name", "text", "MyGame", "Name of the save game"),
            new ServerConfigField("GameMode", "Game Mode", "select", "GameModeSurvival", "Game Mode", Arrays.asList("GameModeSurvival", "GameModeCreative")),

            // Gameplay - Difficulty
            new ServerConfigField("GameDifficulty", "Difficulty", "select", "1", "0=easiest, 5=hardest", Arrays.asList("0", "1", "2", "3", "4", "5")),
            new ServerConfigField("BlockDamagePlayer", "Block Damage Player", "number", "100", "Percentage"),
            new ServerConfigField("BlockDamageAI", "Block Damage AI", "number", "100", "Percentage"),
            new ServerConfigField("BlockDamageAIBM", "Block Damage AI BM", "number", "100", "Percentage during Blood Moon"),
            new ServerConfigField("XPMultiplier", "XP Multiplier", "number", "100", "Percentage"),
            new ServerConfigField("PlayerSafeZoneLevel", "Player Safe Zone Level", "number", "5", "Level limit for safe zone"),
            new ServerConfigField("PlayerSafeZoneHours", "Player Safe Zone Hours", "number", "5", "Hours safe zone exists"),

            // Gameplay - Game Rules
            new ServerConfigField("BuildCreate", "Creative Mode", "boolean", "false", "Cheat mode on/off"),
            new ServerConfigField("DayNightLength", "Day Night Length", "number", "60", "Real time minutes per in game day"),
            new ServerConfigField("DayLightLength", "Day Light Length", "number", "18", "In game hours the sun shines"),
            new ServerConfigField("BiomeProgression", "Biome Progression", "boolean", "true", "Enables biome hazards and loot stage caps"),
            new ServerConfigField("StormFreq", "Storm Frequency", "number", "100", "0-500%"),
            new ServerConfigField("DeathPenalty", "Death Penalty", "select", "1", "0=Nothing, 1=XP, 2=Injured, 3=Permadeath", Arrays.asList("0", "1", "2", "3")),
            new ServerConfigField("DropOnDeath", "Drop On Death", "select", "1", "0=nothing, 1=all, 2=toolbelt, 3=backpack, 4=delete all", Arrays.asList("0", "1", "2", "3", "4")),
            new ServerConfigField("DropOnQuit", "Drop On Quit", "select", "0", "0=nothing, 1=all, 2=toolbelt, 3=backpack", Arrays.asList("0", "1", "2", "3")),
            new ServerConfigField("BedrollDeadZoneSize", "Bedroll Dead Zone", "number", "15", "Size of bedroll dead zone"),
            new ServerConfigField("BedrollExpiryTime", "Bedroll Expiry", "number", "45", "Days bedroll stays active"),
            new ServerConfigField("AllowSpawnNearFriend", "Spawn Near Friend", "select", "2", "0=Disabled, 1=Always, 2=Forest Only", Arrays.asList("0", "1", "2")),
            new ServerConfigField("CameraRestrictionMode", "Camera Restriction", "select", "0", "0=Free, 1=First Person, 2=Third Person", Arrays.asList("0", "1", "2")),
            new ServerConfigField("JarRefund", "Jar Refund", "number", "0", "Percentage"),

            // Gameplay - Performance
            new ServerConfigField("MaxSpawnedZombies", "Max Spawned Zombies", "number", "64", "Global limit"),
            new ServerConfigField("MaxSpawnedAnimals", "Max Spawned Animals", "number", "50", "Global limit"),
            new ServerConfigField("ServerMaxAllowedViewDistance", "Max View Distance", "number", "12", "6-12"),
            new ServerConfigField("MaxQueuedMeshLayers", "Max Queued Mesh Layers", "number", "1000", "Chunk mesh generation limit"),

            // Gameplay - Zombie Settings
            new ServerConfigField("EnemySpawnMode", "Enemy Spawn Mode", "boolean", "true", "Enable/Disable enemy spawning"),
            new ServerConfigField("EnemyDifficulty", "Enemy Difficulty", "select", "0", "0=Normal, 1=Feral", Arrays.asList("0", "1")),
            new ServerConfigField("ZombieFeralSense", "Feral Sense", "select", "0", "0=Off, 1=Day, 2=Night, 3=All", Arrays.asList("0", "1", "2", "3")),
            new ServerConfigField("ZombieMove", "Zombie Move", "select", "0", "0=Walk, 1=Jog, 2=Run, 3=Sprint, 4=Nightmare", Arrays.asList("0", "1", "2", "3", "4")),
            new ServerConfigField("ZombieMoveNight", "Zombie Move Night", "select", "3", "0=Walk, 1=Jog, 2=Run, 3=Sprint, 4=Nightmare", Arrays.asList("0", "1", "2", "3", "4")),
            new ServerConfigField("ZombieFeralMove", "Zombie Feral Move", "select", "3", "0=Walk, 1=Jog, 2=Run, 3=Sprint, 4=Nightmare", Arrays.asList("0", "1", "2", "3", "4")),
            new ServerConfigField("ZombieBMMove", "Zombie BM Move", "select", "3", "0=Walk, 1=Jog, 2=Run, 3=Sprint, 4=Nightmare", Arrays.asList("0", "1", "2", "3", "4")),
            new ServerConfigField("AISmellMode", "AI Smell Mode", "select", "3", "0-5", Arrays.asList("0", "1", "2", "3", "4", "5")),
            new ServerConfigField("BloodMoonFrequency", "Blood Moon Frequency", "number", "7", "Days"),
            new ServerConfigField("BloodMoonRange", "Blood Moon Range", "number", "0", "Deviation days"),
            new ServerConfigField("BloodMoonWarning", "Blood Moon Warning", "number", "8", "Hour number"),
            new ServerConfigField("BloodMoonEnemyCount", "Blood Moon Enemy Count", "number", "8", "Zombies per player"),

            // Gameplay - Loot
            new ServerConfigField("LootAbundance", "Loot Abundance", "number", "100", "Percentage"),
            new ServerConfigField("LootRespawnDays", "Loot Respawn Days", "number", "7", "Days"),
            new ServerConfigField("AirDropFrequency", "Air Drop Frequency", "number", "72", "Hours"),
            new ServerConfigField("AirDropMarker", "Air Drop Marker", "boolean", "true", "Show marker"),

            // Gameplay - Multiplayer
            new ServerConfigField("PartySharedKillRange", "Party Shared Kill Range", "number", "100", "Distance"),
            new ServerConfigField("PlayerKillingMode", "Player Killing Mode", "select", "3", "0=No Killing, 1=Allies Only, 2=Strangers Only, 3=Kill Everyone", Arrays.asList("0", "1", "2", "3")),

            // Gameplay - Land Claim
            new ServerConfigField("LandClaimCount", "Land Claim Count", "number", "5", "Max claims per player"),
            new ServerConfigField("LandClaimSize", "Land Claim Size", "number", "41", "Size in blocks"),
            new ServerConfigField("LandClaimDeadZone", "Land Claim Dead Zone", "number", "30", "Distance between keystones"),
            new ServerConfigField("LandClaimExpiryTime", "Land Claim Expiry", "number", "7", "Days offline"),
            new ServerConfigField("LandClaimDecayMode", "Land Claim Decay", "select", "0", "0=Slow, 1=Fast, 2=None", Arrays.asList("0", "1", "2")),
            new ServerConfigField("LandClaimOnlineDurabilityModifier", "Land Claim Online Durability", "number", "4", "Hardness multiplier"),
            new ServerConfigField("LandClaimOfflineDurabilityModifier", "Land Claim Offline Durability", "number", "4", "Hardness multiplier"),
            new ServerConfigField("LandClaimOfflineDelay", "Land Claim Offline Delay", "number", "0", "Minutes"),

            // Gameplay - Dynamic Mesh
            new ServerConfigField("DynamicMeshEnabled", "Dynamic Mesh Enabled", "boolean", "true", "Is Dynamic Mesh system enabled"),
            new ServerConfigField("DynamicMeshLandClaimOnly", "Dynamic Mesh Land Claim Only", "boolean", "true", "Only active in player LCB areas"),
            new ServerConfigField("DynamicMeshLandClaimBuffer", "Dynamic Mesh Land Claim Buffer", "number", "3", "Chunk radius"),
            new ServerConfigField("DynamicMeshMaxItemCache", "Dynamic Mesh Max Item Cache", "number", "3", "Items processed concurrently"),

            // Gameplay - Twitch
            new ServerConfigField("TwitchServerPermission", "Twitch Server Permission", "number", "90", "Required permission level"),
            new ServerConfigField("TwitchBloodMoonAllowed", "Twitch Blood Moon Allowed", "boolean", "false", "Allow actions during blood moon"),

            // Gameplay - Quest
            new ServerConfigField("QuestProgressionDailyLimit", "Quest Progression Daily Limit", "number", "4", "Limit quests per day")
        );
    }

    @Override
    public void generateConfigFile(Map<String, String> values, Path serverDir, String fileName) throws IOException {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\"?>\n");
        xml.append("<ServerSettings>\n");
        
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey().equals("fileName")) continue; // Skip filename field itself
            xml.append("\t<property name=\"").append(entry.getKey()).append("\" value=\"").append(entry.getValue()).append("\"/>\n");
        }
        
        xml.append("</ServerSettings>");
        
        if (!fileName.endsWith(".xml")) {
            fileName += ".xml";
        }
        
        Files.writeString(serverDir.resolve(fileName), xml.toString());
    }

    @Override
    public Map<String, String> parseConfigFile(Path configFile) throws IOException {
        Map<String, String> values = new HashMap<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(configFile.toFile());
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("property");

            for (int i = 0; i < nList.getLength(); i++) {
                Node nNode = nList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String name = eElement.getAttribute("name");
                    String value = eElement.getAttribute("value");
                    values.put(name, value);
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse config file", e);
        }
        return values;
    }

    @Override
    public Path getConfigFileFromParams(Map<String, String> params) {
        String commandLine = params.get("commandLine");
        if (commandLine != null) {
            // Look for -configfile=...
            // Regex to handle quoted or unquoted paths
            // -configfile="C:\Path With Spaces\config.xml" or -configfile=config.xml
            Pattern pattern = Pattern.compile("-configfile=(?:\"([^\"]+)\"|([^\\s]+))");
            Matcher matcher = pattern.matcher(commandLine);
            if (matcher.find()) {
                String pathStr = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                return Path.of(pathStr);
            }
        }
        return null;
    }

    @Override
    public void shutdownServer(GameServer server) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("commandLine", server.getCommandLine());
        Path configPath = getConfigFileFromParams(params);

        if (configPath != null) {
            // Resolve relative path if needed
            if (!configPath.isAbsolute()) {
                String serverPath = server.getServerPath();
                if (serverPath.startsWith("\"") && serverPath.endsWith("\"")) {
                    serverPath = serverPath.substring(1, serverPath.length() - 1);
                }
                Path serverDir = Paths.get(serverPath).getParent();
                configPath = serverDir.resolve(configPath);
            }

            if (Files.exists(configPath)) {
                Map<String, String> config = parseConfigFile(configPath);
                String telnetEnabled = config.get("TelnetEnabled");
                String telnetPortStr = config.get("TelnetPort");
                String telnetPassword = config.get("TelnetPassword");

                if ("true".equalsIgnoreCase(telnetEnabled) && telnetPortStr != null) {
                    int telnetPort = Integer.parseInt(telnetPortStr);
                    TelnetClientManager telnetManager = new TelnetClientManager();
                    try {
                        telnetManager.connect("localhost", telnetPort);
                        
                        // Wait for password prompt
                        String response = telnetManager.readUntil("Please enter password:");
                        if (response.contains("Please enter password:")) {
                            telnetManager.sendCommand(telnetPassword, null, true); // Mask password in logs
                        }

                        // Read login response (wait up to 2 seconds)
                        telnetManager.read(2000);

                        // Send shutdown command
                        telnetManager.sendCommand("shutdown", null);
                        
                        // Read shutdown response (wait up to 2 seconds)
                        telnetManager.read(2000);

                    } catch (Exception e) {
                        throw new IOException("Telnet shutdown failed", e);
                    } finally {
                        try {
                            telnetManager.disconnect();
                        } catch (Exception e) {
                            // Ignore disconnect errors
                        }
                    }
                    return; // Success
                }
            }
        }
        
        // Fallback if config not found or telnet disabled
        throw new IOException("Telnet not configured or disabled");
    }
}
