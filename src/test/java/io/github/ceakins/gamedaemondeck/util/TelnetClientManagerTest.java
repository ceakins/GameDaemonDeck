package io.github.ceakins.gamedaemondeck.util;

import org.apache.commons.net.telnet.TelnetClient;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

public class TelnetClientManagerTest {

    @Mock
    private TelnetClient telnetClient;

    private TelnetClientManager telnetClientManager;

    private ByteArrayOutputStream outputStream;
    private InputStream inputStream;

    @BeforeMethod
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        telnetClientManager = new TelnetClientManager(telnetClient);

        outputStream = new ByteArrayOutputStream();
        inputStream = new ByteArrayInputStream("response>".getBytes());

        when(telnetClient.getInputStream()).thenReturn(inputStream);
        when(telnetClient.getOutputStream()).thenReturn(new PrintStream(outputStream));
        when(telnetClient.isConnected()).thenReturn(true);
        doNothing().when(telnetClient).connect(anyString(), anyInt());
        doNothing().when(telnetClient).disconnect();

        // Set the in and out fields directly for testing
        telnetClientManager.in = telnetClient.getInputStream();
        telnetClientManager.out = new PrintStream(outputStream);
    }

    @Test
    public void testConnect() throws IOException {
        telnetClientManager.connect("localhost", 23);
        verify(telnetClient).connect("localhost", 23);
    }

    @Test
    public void testDisconnect() throws IOException {
        telnetClientManager.disconnect();
        verify(telnetClient).disconnect();
    }

    @Test
    public void testSendCommand_notConnected() {
        when(telnetClient.isConnected()).thenReturn(false);
        assertThrows(IOException.class, () -> telnetClientManager.sendCommand("status"));
    }

    @Test
    public void testSendCommand_connected() throws IOException {
        String command = "status";
        String response = telnetClientManager.sendCommand(command);
        assertTrue(outputStream.toString().contains("status"));
        assertEquals(response, "response>");
    }
}
