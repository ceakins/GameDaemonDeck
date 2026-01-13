package io.github.ceakins.gamedaemondeck.util;

import io.github.ceakins.gamedaemondeck.db.ConfigStore;
import io.github.ceakins.gamedaemondeck.db.Configuration;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertThrows;

public class SteamManagerTest {

    @Mock
    private ConfigStore configStore;

    @Mock
    private ProcessBuilder processBuilder;

    @Mock
    private Process process;

    private SteamManager steamManager;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        steamManager = new SteamManager(configStore, processBuilder);
    }

    @Test
    public void testInstallOrUpdateGame_notConfigured() {
        when(configStore.getConfiguration()).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> steamManager.installOrUpdateGame("123", "/path/to/game"));
    }

    @Test
    public void testInstallOrUpdateGame_configured() throws IOException, InterruptedException {
        Configuration config = new Configuration();
        config.setSteamCmdPath("/path/to/steamcmd");
        when(configStore.getConfiguration()).thenReturn(Optional.of(config));

        // Mock ProcessBuilder to return our mock Process
        when(processBuilder.command(any(String[].class))).thenReturn(processBuilder);
        when(processBuilder.directory(any())).thenReturn(processBuilder);
        when(processBuilder.start()).thenReturn(process);

        // Mock Process to return a stream and exit successfully
        InputStream inputStream = new ByteArrayInputStream("output".getBytes());
        InputStream errorStream = new ByteArrayInputStream("".getBytes());
        when(process.getInputStream()).thenReturn(inputStream);
        when(process.getErrorStream()).thenReturn(errorStream);
        when(process.waitFor()).thenReturn(0); // Successful exit

        steamManager.installOrUpdateGame("123", "/path/to/game");

        // Use Path.of().toString() to get the platform-specific path for verification
        String expectedSteamCmdPath = Path.of("/path/to/steamcmd").toString();
        String expectedInstallDirPath = Path.of("/path/to/game").toFile().toString();

        verify(processBuilder).command(expectedSteamCmdPath, "+login", "anonymous", "+app_update", "123", "+quit");
        verify(processBuilder).directory(Path.of(expectedInstallDirPath).toFile());
        verify(process).waitFor();
    }
}
