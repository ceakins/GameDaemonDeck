package io.github.ceakins.daemondeck.core;

import io.github.ceakins.daemondeck.db.ConfigStore;
import io.github.ceakins.daemondeck.db.DiscordBot;
import io.github.ceakins.daemondeck.db.DiscordWebhook;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordService {

    private final ConfigStore configStore;
    final Map<String, JDA> runningBots = new ConcurrentHashMap<>();

    public DiscordService(ConfigStore configStore) {
        this.configStore = configStore;
    }

    public void startAllBots() {
        List<DiscordBot> bots = configStore.getAllBots();
        for (DiscordBot bot : bots) {
            startBot(bot);
        }
    }

    public void startBot(DiscordBot bot) {
        if (runningBots.containsKey(bot.getName())) {
            return; // Bot is already running
        }
        try {
            JDA jda = JDABuilder.createDefault(bot.getToken())
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .build();
            runningBots.put(bot.getName(), jda);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopAllBots() {
        runningBots.values().forEach(JDA::shutdown);
        runningBots.clear();
    }

    public void stopBot(String botName) {
        if (runningBots.containsKey(botName)) {
            runningBots.get(botName).shutdown();
            runningBots.remove(botName);
        }
    }

    public JDA getBot(String botName) {
        return runningBots.get(botName);
    }

    public void saveWebhook(DiscordWebhook webhook) {
        configStore.saveWebhook(webhook);
    }

    public List<DiscordWebhook> getAllWebhooks() {
        return configStore.getAllWebhooks();
    }

    public void deleteWebhook(String name) {
        configStore.deleteWebhook(name);
    }

    public void saveBot(DiscordBot bot) {
        configStore.saveBot(bot);
    }

    public List<DiscordBot> getAllBots() {
        return configStore.getAllBots();
    }

    public void deleteBot(String botName) {
        stopBot(botName);
        configStore.deleteBot(botName);
    }
}
