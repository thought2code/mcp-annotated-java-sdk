---
hide:
    - navigation
---

# Getting Started Guide

This guide will help you build your first MCP server in 5 minutes.

## Requirements

- **Java 17 or later** (required by official MCP Java SDK)

## Installation

### Maven Dependency

```xml
<dependency>
    <groupId>io.github.thought2code</groupId>
    <artifactId>mcp-annotated-java-sdk</artifactId>
    <version>0.19.0</version>
</dependency>
```

### Gradle Dependency

```gradle
implementation 'io.github.thought2code:mcp-annotated-java-sdk:0.19.0'
```

## 5-Minutes Tutorial

### Step 1: Create Configuration File

Create `mcp-server.yml` in your `src/main/resources`:

```yaml
enabled: true
mode: STDIO
name: my-first-mcp-server
version: 1.0.0
type: SYNC
instructions: You are a helpful AI assistant
request-timeout: 20000
capabilities:
  resource: true
  subscribe-resource: true
  prompt: true
  tool: true
  completion: true
change-notification:
  resource: true
  prompt: true
  tool: true
```

### Step 2: Create MCP Server Main Class

```java
@McpServerApplication
public class MyFirstMcpServer {
    public static void main(String[] args) {
        McpApplication.run(MyFirstMcpServer.class, args);
    }
}
```

### Step 3: Define MCP Resources (Optional)

```java
public class MyResources {
    @McpResource(uri = "system://info", description = "System information")
    public Map<String, String> getSystemInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("os", System.getProperty("os.name"));
        info.put("java", System.getProperty("java.version"));
        info.put("cores", String.valueOf(Runtime.getRuntime().availableProcessors()));
        return info;
    }
}
```

### Step 4: Define MCP Tools

```java
public class MyTools {
    @McpTool(description = "Calculate the sum of two numbers")
    public int add(
        @McpToolParam(name = "a", description = "First number") int a,
        @McpToolParam(name = "b", description = "Second number") int b
    ) {
        return a + b;
    }
}
```

### Step 5: Define MCP Prompts (Optional)

```java
public class MyPrompts {
    @McpPrompt(description = "Generate code for a given task")
    public String generateCode(
        @McpPromptParam(name = "language", description = "Programming language") String language,
        @McpPromptParam(name = "task", description = "Task description") String task
    ) {
        return String.format("Write %s code to: %s", language, task);
    }
}
```

### Step 6: Run the Server

```bash
# Compile your project
./mvnw clean package
```

Run `MyFirstMcpServer` from your IDE, or use `java -cp ...` with your compiled classes and dependencies on the classpath. Use an executable JAR setup in your own project if you need `java -jar` with a single file.

To load a non-default configuration file name, use `McpApplication.run(MyFirstMcpServer.class, args, "custom-mcp-server.yml")`.

## Server Modes

This SDK supports three MCP server modes. If you omit `mode` in `mcp-server.yml`, the server defaults to **STREAMABLE** (see the configuration table below).

### 1. STDIO Mode

Based on standard input/output communication, suitable for CLI tools and local development.

```yaml
# mcp-server.yml
mode: STDIO
```

### 2. STREAMABLE Mode

HTTP streaming for web applications, recommended for production.

```yaml
# mcp-server.yml
mode: STREAMABLE
streamable:
  mcp-endpoint: /mcp/message
  disallow-delete: false
  keep-alive-interval: 20000
  port: 8080
```

## Configuration Properties

| Property                          | Description                                                     | Default                      |
|-----------------------------------|-----------------------------------------------------------------|------------------------------|
| `enabled`                         | Enable/disable MCP server                                       | `true`                       |
| `mode`                            | Server mode: `STDIO`, `STREAMABLE`                              | `STREAMABLE`                 |
| `name`                            | Server name                                                     | `mcp-server`                 |
| `version`                         | Server version                                                  | `1.0.0`                      |
| `type`                            | Server type: `SYNC`, `ASYNC`                                    | `SYNC`                       |
| `instructions`                    | Instructions for the LLM client                                 | Required (non-blank in YAML) |
| `request-timeout`                 | Request timeout in milliseconds                                 | `20000`                      |
| `capabilities.resource`           | Enable resource support                                         | `true`                       |
| `capabilities.subscribe-resource` | Enable resource subscription                                    | `true`                       |
| `capabilities.prompt`             | Enable prompt support                                           | `true`                       |
| `capabilities.tool`               | Enable tool support                                             | `true`                       |
| `capabilities.completion`         | Enable completion support                                       | `true`                       |
| `change-notification.resource`    | Notify clients on resource change                               | `true`                       |
| `change-notification.prompt`      | Notify clients on prompt change                                 | `true`                       |
| `change-notification.tool`        | Notify clients on tool change                                   | `true`                       |
| `streamable.mcp-endpoint`         | Streamable HTTP MCP path                                        | `/mcp/message`               |
| `streamable.disallow-delete`      | Reject HTTP DELETE on session                                   | `false`                      |
| `streamable.keep-alive-interval`  | Keep-alive interval (ms)                                        | `20000`                      |
| `streamable.port`                 | HTTP port for STREAMABLE mode                                   | `8080`                       |

## Runtime model and stability

### SYNC vs ASYNC (`type`)

The `type` property selects the MCP Java SDK server API (`SYNC` or `ASYNC`). It does **not** make your annotated methods reactive.

- **SYNC** — methods run on the request thread.
- **ASYNC** — the SDK exposes async handlers that wrap your method in `Mono.fromCallable(...)`. Your code is still **blocking** Java; you do not return `Mono` from `@McpTool` / `@McpPrompt` / `@McpResource` methods.

Use **SYNC** by default. Choose **ASYNC** only when your deployment requires the async MCP server API. Long work still blocks a Reactor worker thread under ASYNC.

### Component instances and concurrency

The SDK creates **one instance per component class** (via a **public no-arg constructor**) and reuses it for all requests. Concurrent MCP calls share that object. Keep components **stateless** or **thread-safe**; avoid unsynchronized per-request instance fields.

- **STREAMABLE** is the supported HTTP transport.

## Profile-based Configuration

Set `profile` in the base file to load `mcp-server-{profile}.yml` from the classpath. Profile values are merged into the base configuration with Jackson deep merge; nested objects such as `capabilities` and `streamable` are merged field-by-field. The `profile` name always comes from the base file. After merge, transport settings that do not match the resolved `mode` are cleared (for example, `streamable` is removed when `mode` is `STDIO`).

You can use profiles for different environments:

```yaml
# mcp-server.yml (base configuration)
enabled: true
mode: STREAMABLE
name: my-mcp-server
version: 1.0.0
profile: dev
```

```yaml
# mcp-server-dev.yml (profile-specific configuration)
streamable:
  port: 8080
```

## Project Structure

The typical project structure is as follows:

```
your-mcp-project/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           ├── MyMcpServer.java         # Main entry point
│   │   │           ├── components/
│   │   │           │   ├── MyResources.java     # MCP Resources
│   │   │           │   ├── MyTools.java         # MCP Tools
│   │   │           │   └── MyPrompts.java       # MCP Prompts
│   │   │           └── service/
│   │   │               └── BusinessLogic.java   # Business logic
│   │   └── resources/
│   │       └── mcp-server.yml                   # MCP configuration
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── McpServerTest.java       # Unit tests
└── target/
    └── *.jar                                    # Build output (name depends on your project)
```

## Next Steps

- Want to learn more about MCP components? Check [Core Components](./components.md)
