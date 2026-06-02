package com.github.thought2code.mcp.annotated.server;

import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import java.util.List;
import java.util.Objects;
import reactor.core.publisher.Mono;

/**
 * Defers {@link McpServerTransportProvider#setSessionFactory} until {@link #activate()} is called.
 *
 * <p>STDIO transport begins reading stdin as soon as the session factory is set. Delaying
 * activation until after MCP components are registered prevents clients from observing partially
 * registered servers during startup.
 */
final class DeferredActivationServerTransportProvider implements McpServerTransportProvider {

  private final McpServerTransportProvider delegate;

  private McpServerSession.Factory sessionFactory;

  private volatile boolean activated;

  /**
   * @param delegate underlying transport provider whose session factory is deferred
   */
  DeferredActivationServerTransportProvider(McpServerTransportProvider delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
  }

  /**
   * Starts the underlying transport using the session factory supplied during server construction.
   */
  void activate() {
    if (activated) {
      return;
    }
    if (sessionFactory == null) {
      throw new IllegalStateException("MCP session factory has not been configured");
    }
    activated = true;
    delegate.setSessionFactory(sessionFactory);
  }

  /**
   * Stores the session factory until {@link #activate()}; forwards immediately once activated.
   *
   * @param sessionFactory MCP server session factory from server construction
   */
  @Override
  public void setSessionFactory(McpServerSession.Factory sessionFactory) {
    if (activated) {
      delegate.setSessionFactory(sessionFactory);
      return;
    }
    this.sessionFactory = sessionFactory;
  }

  /** {@inheritDoc} */
  @Override
  public List<String> protocolVersions() {
    return delegate.protocolVersions();
  }

  /**
   * No-ops until {@link #activate()} so clients are not notified before registration completes.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public Mono<Void> notifyClients(String method, Object params) {
    if (!activated) {
      return Mono.empty();
    }
    return delegate.notifyClients(method, params);
  }

  /**
   * No-ops until {@link #activate()} so clients are not notified before registration completes.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public Mono<Void> notifyClient(String sessionId, String method, Object params) {
    if (!activated) {
      return Mono.empty();
    }
    return delegate.notifyClient(sessionId, method, params);
  }

  /** {@inheritDoc} */
  @Override
  public Mono<Void> closeGracefully() {
    return delegate.closeGracefully();
  }

  /** {@inheritDoc} */
  @Override
  public void close() {
    delegate.close();
  }
}
