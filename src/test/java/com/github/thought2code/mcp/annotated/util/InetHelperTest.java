package com.github.thought2code.mcp.annotated.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.Inet4Address;
import java.net.InetAddress;
import org.junit.jupiter.api.Test;

class InetHelperTest {

  @Test
  void findFirstNonLoopbackAddress_shouldReturnIpv4OrLoopbackFallback() {
    InetAddress address = InetHelper.findFirstNonLoopbackAddress();
    assertNotNull(address);
    // Either a real IPv4 address or loopback fallback — both are valid outcomes.
    assertNotNull(address.getHostAddress());
    if (address instanceof Inet4Address inet4Address) {
      assertNotNull(inet4Address.getHostAddress());
    }
  }
}
