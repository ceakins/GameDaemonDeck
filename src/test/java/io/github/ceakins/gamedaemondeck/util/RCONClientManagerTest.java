package io.github.ceakins.gamedaemondeck.util;

import org.glavo.rcon.AuthenticationException;
import org.glavo.rcon.Rcon;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

public class RCONClientManagerTest {

    @Mock
    private Rcon mockRcon;

    private RCONClientManager rconClientManager;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Inject the mockRcon instance into the RCONClientManager for testing
        rconClientManager = new RCONClientManager("localhost", 27015, "password", mockRcon);
    }

    @Test
    public void testConnect() throws IOException, AuthenticationException {
        rconClientManager.connect();
        // Since mockRcon is injected, connect() just assigns it. No actual Rcon.open() call.
        // The implicit check is that no exception is thrown during connection.
    }

    @Test
    public void testDisconnect() throws IOException, AuthenticationException {
        rconClientManager.connect(); // Ensure 'rcon' field is set to mockRcon
        rconClientManager.disconnect();
        verify(mockRcon).close();
    }

    @Test
    public void testSendCommand_notConnected() {
        // Here, the 'rcon' field is null because connect() was not called
        rconClientManager = new RCONClientManager("localhost", 27015, "password", null); // Ensure rcon is null
        assertThrows(IOException.class, () -> rconClientManager.sendCommand("status"));
    }

    @Test
    public void testSendCommand_connected() throws IOException, AuthenticationException {
        rconClientManager.connect(); // Set the internal 'rcon' field to mockRcon
        String expectedResponse = "players: 5";
        when(mockRcon.command("status")).thenReturn(expectedResponse);

        String actualResponse = rconClientManager.sendCommand("status");

        assertEquals(actualResponse, expectedResponse);
        verify(mockRcon).command("status");
    }
}
