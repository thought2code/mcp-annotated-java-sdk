package com.github.thought2code.mcp.annotated.server;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.thought2code.mcp.annotated.enums.McpServerError;
import jakarta.servlet.http.HttpServlet;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;

class JettyHttpServerTest {

  @Test
  void start_whenPortIsAlreadyInUse_shouldThrowException() throws IOException {
    try (ServerSocket blocker = new ServerSocket(0)) {
      final int occupiedPort = blocker.getLocalPort();
      JettyHttpServer server = new JettyHttpServer();

      IllegalStateException exception =
          assertThrows(
              IllegalStateException.class,
              () -> server.withTransportProvider(new HttpServlet() {}).bind(occupiedPort).start());

      assertTrue(
          exception.getMessage().contains(McpServerError.JETTY_SERVER_START_ERROR.getCode()));
    }
  }
}
