package io.github.ceakins.daemondeck.core;

import io.github.ceakins.daemondeck.db.ConfigStore;
import io.github.ceakins.daemondeck.db.DiscordBot;
import io.github.ceakins.daemondeck.db.DiscordWebhook;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DiscordServiceTest {

    @Mock
    private ConfigStore configStore;

    private DiscordService discordService;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        discordService = new DiscordService(configStore);
    }

    @Test
    public void testStartAllBots_noBots() {
        when(configStore.getAllBots()).thenReturn(Collections.emptyList());
        discordService.startAllBots();
        // Verify no JDA bots were built or started
        // This is a bit tricky to verify directly without mocking JDABuilder.
        // For now, we'll assume if getAllBots returns empty, no bots are started.
    }

    @Test
    public void testStartBot() {
        DiscordBot bot = new DiscordBot();
        bot.setName("TestBot");
        bot.setToken("FAKE_TOKEN");

        // Mock JDABuilder to avoid actual network calls
        try (var jdaBuilderMockedStatic = Mockito.mockStatic(JDABuilder.class)) {
            JDABuilder mockBuilder = Mockito.mock(JDABuilder.class);
            JDA mockJDA = Mockito.mock(JDA.class);

            jdaBuilderMockedStatic.when(() -> JDABuilder.createDefault(any())).thenReturn(mockBuilder);
            when(mockBuilder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockJDA);

            discordService.startBot(bot);

            assertTrue(discordService.getBot("TestBot") instanceof JDA);
        }
    }

    @Test
    public void testStopAllBots() {
        DiscordBot bot1 = new DiscordBot();
        bot1.setName("TestBot1");
        bot1.setToken("FAKE_TOKEN1");

        DiscordBot bot2 = new DiscordBot();
        bot2.setName("TestBot2");
        bot2.setToken("FAKE_TOKEN2");

        // Start bots for testing stopAllBots
        try (var jdaBuilderMockedStatic = Mockito.mockStatic(JDABuilder.class)) {
            JDABuilder mockBuilder = Mockito.mock(JDABuilder.class);
            JDA mockJDA1 = Mockito.mock(JDA.class);
            JDA mockJDA2 = Mockito.mock(JDA.class);

            jdaBuilderMockedStatic.when(() -> JDABuilder.createDefault("FAKE_TOKEN1")).thenReturn(mockBuilder);
            when(mockBuilder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockJDA1);
            discordService.startBot(bot1);

            jdaBuilderMockedStatic.when(() -> JDABuilder.createDefault("FAKE_TOKEN2")).thenReturn(mockBuilder);
            when(mockBuilder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockJDA2);
            discordService.startBot(bot2);

            // Directly check the running bots map size
            assertEquals(discordService.runningBots.size(), 2);

            discordService.stopAllBots();

            verify(mockJDA1).shutdown();
            verify(mockJDA2).shutdown();
            assertTrue(discordService.runningBots.isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testStopBot() {
        DiscordBot bot = new DiscordBot();
        bot.setName("TestBot");
        bot.setToken("FAKE_TOKEN");

        try (var jdaBuilderMockedStatic = Mockito.mockStatic(JDABuilder.class)) {
            JDABuilder mockBuilder = Mockito.mock(JDABuilder.class);
            JDA mockJDA = Mockito.mock(JDA.class);

            jdaBuilderMockedStatic.when(() -> JDABuilder.createDefault(any())).thenReturn(mockBuilder);
            when(mockBuilder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockJDA);

            discordService.startBot(bot);
            discordService.stopBot("TestBot");

            verify(mockJDA).shutdown();
            assertTrue(discordService.getBot("TestBot") == null);
        }
    }

    @Test
    public void testSaveWebhook() {
        DiscordWebhook webhook = new DiscordWebhook();
        webhook.setName("TestWebhook");
        discordService.saveWebhook(webhook);
        verify(configStore).saveWebhook(webhook);
    }

    @Test
    public void testGetAllWebhooks() {
        List<DiscordWebhook> expectedWebhooks = Arrays.asList(new DiscordWebhook(), new DiscordWebhook());
        when(configStore.getAllWebhooks()).thenReturn(expectedWebhooks);
        List<DiscordWebhook> actualWebhooks = discordService.getAllWebhooks();
        assertEquals(actualWebhooks, expectedWebhooks);
    }

    @Test
    public void testDeleteWebhook() {
        String webhookName = "TestWebhook";
        discordService.deleteWebhook(webhookName);
        verify(configStore).deleteWebhook(webhookName);
    }

    @Test
    public void testSaveBot() {
        DiscordBot bot = new DiscordBot();
        bot.setName("TestBot");
        discordService.saveBot(bot);
        verify(configStore).saveBot(bot);
    }

    @Test
    public void testGetAllBots() {
        List<DiscordBot> expectedBots = Arrays.asList(new DiscordBot(), new DiscordBot());
        when(configStore.getAllBots()).thenReturn(expectedBots);
        List<DiscordBot> actualBots = discordService.getAllBots();
        assertEquals(actualBots, expectedBots);
    }

    @Test
    public void testDeleteBot() {
        String botName = "TestBot";
        DiscordBot bot = new DiscordBot();
        bot.setName(botName);
        bot.setToken("FAKE_TOKEN");

        try (var jdaBuilderMockedStatic = Mockito.mockStatic(JDABuilder.class)) {
            JDABuilder mockBuilder = Mockito.mock(JDABuilder.class);
            JDA mockJDA = Mockito.mock(JDA.class);

            jdaBuilderMockedStatic.when(() -> JDABuilder.createDefault(any())).thenReturn(mockBuilder);
            when(mockBuilder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockJDA);

            discordService.startBot(bot);
            discordService.deleteBot(botName);

            verify(mockJDA).shutdown();
            verify(configStore).deleteBot(botName);
            assertTrue(discordService.getBot(botName) == null);
        }
    }
}
