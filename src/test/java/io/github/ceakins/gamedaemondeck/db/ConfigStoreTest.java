package io.github.ceakins.gamedaemondeck.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ConfigStoreTest {

    @Mock
    private MVStore mvStore;

    @Mock
    private MVMap mvMap;

    private ObjectMapper objectMapper = new ObjectMapper();

    private ConfigStore configStore;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mvStore.openMap(any())).thenReturn(mvMap);
        configStore = new ConfigStore(mvStore, objectMapper);
    }

    @Test
    public void testGetConfiguration_whenNotConfigured() {
        when(mvMap.get(any())).thenReturn(null);
        Optional<Configuration> config = configStore.getConfiguration();
        assertTrue(config.isEmpty());
    }

    @Test
    public void testGetConfiguration_whenConfigured() throws IOException {
        Configuration expectedConfig = new Configuration();
        expectedConfig.setAdminUsername("admin");
        String configJson = objectMapper.writeValueAsString(expectedConfig);
        when(mvMap.get(any())).thenReturn(configJson);

        Optional<Configuration> actualConfig = configStore.getConfiguration();

        assertTrue(actualConfig.isPresent());
        assertEquals(actualConfig.get().getAdminUsername(), "admin");
    }

    @Test
    public void testSaveConfiguration() throws JsonProcessingException {
        Configuration configToSave = new Configuration();
        configToSave.setAdminUsername("new-admin");

        configStore.saveConfiguration(configToSave);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(mvMap).put(keyCaptor.capture(), valueCaptor.capture());

        assertEquals(keyCaptor.getValue(), "configuration");
        assertTrue(valueCaptor.getValue().contains("new-admin"));
    }

    @Test
    public void testSaveWebhook() throws JsonProcessingException {
        DiscordWebhook webhook = new DiscordWebhook();
        webhook.setName("test-webhook");

        configStore.saveWebhook(webhook);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(mvMap).put(keyCaptor.capture(), valueCaptor.capture());

        assertEquals(keyCaptor.getValue(), "test-webhook");
        assertTrue(valueCaptor.getValue().contains("test-webhook"));
    }

    @Test
    public void testGetWebhook() throws IOException {
        DiscordWebhook expectedWebhook = new DiscordWebhook();
        expectedWebhook.setName("test-webhook");
        String webhookJson = objectMapper.writeValueAsString(expectedWebhook);
        when(mvMap.get("test-webhook")).thenReturn(webhookJson);

        Optional<DiscordWebhook> actualWebhook = configStore.getWebhook("test-webhook");

        assertTrue(actualWebhook.isPresent());
        assertEquals(actualWebhook.get().getName(), "test-webhook");
    }

    @Test
    public void testSaveBot() throws JsonProcessingException {
        DiscordBot bot = new DiscordBot();
        bot.setName("test-bot");

        configStore.saveBot(bot);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(mvMap).put(keyCaptor.capture(), valueCaptor.capture());

        assertEquals(keyCaptor.getValue(), "test-bot");
        assertTrue(valueCaptor.getValue().contains("test-bot"));
    }

    @Test
    public void testGetBot() throws IOException {
        DiscordBot expectedBot = new DiscordBot();
        expectedBot.setName("test-bot");
        String botJson = objectMapper.writeValueAsString(expectedBot);
        when(mvMap.get("test-bot")).thenReturn(botJson);

        Optional<DiscordBot> actualBot = configStore.getBot("test-bot");

        assertTrue(actualBot.isPresent());
        assertEquals(actualBot.get().getName(), "test-bot");
    }
}
