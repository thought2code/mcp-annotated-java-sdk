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
    <version>0.13.0</version>
</dependency>
```

### Gradle Dependency

```gradle
implementation 'io.github.thought2code:mcp-annotated-java-sdk:0.13.0'
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
request-timeout: 20000
capabilities:
  resource: true
  prompt: true
  tool: true
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
# Compile and run
./mvnw clean package
java -jar target/your-app.jar
```

## Server Modes

This SDK supports three MCP server modes:

### 1. STDIO Mode (Default)

Based on standard input/output communication, suitable for CLI tools and local development.

```yaml
# mcp-server.yml
mode: STDIO
```

### 2. SSE (Server-Sent Events) Mode

!!! warning "Deprecated"
    SSE mode is deprecated. Use STREAMABLE mode for new projects.

HTTP-based real-time communication.

```yaml
# mcp-server.yml
mode: SSE
sse:
  port: 8080
  endpoint: /sse
  messageEndpoint: /message
```

### 3. STREAMABLE Mode

HTTP streaming for web applications, recommended for production.

```yaml
# mcp-server.yml
mode: STREAMABLE
streamable:
  mcp-endpoint: /mcp/message
  disallow-delete: true
  keep-alive-interval: 30000
  port: 8080
```

## Configuration Properties

| Property              | Description                               | Default      |
|-----------------------|-------------------------------------------|--------------|
| `enabled`             | Enable/disable MCP server                 | `true`       |
| `mode`                | Server mode: `STDIO`, `SSE`, `STREAMABLE` | `STREAMABLE` |
| `name`                | Server name                               | `mcp-server` |
| `version`             | Server version                            | `1.0.0`      |
| `type`                | Server type: `SYNC`, `ASYNC`              | `SYNC`       |
| `request-timeout`     | Request timeout in milliseconds           | `20000`      |
| `capabilities`        | Enable resources, prompts, tools          | all `true`   |
| `change-notification` | Enable change notifications               | all `true`   |

## Profile-based Configuration

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
│   │       ├── mcp-server.yml                   # MCP configuration
│   │       └── messages.properties              # Internationalization messages
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── McpServerTest.java       # Unit tests
└── target/
    └── your-app.jar                             # Executable JAR
```

## Next Steps

- Want to learn more about MCP components? Check [Core Components](./components.md)
- Need multilingual support? See [i18n Support](./components.md#multilingual-support) section
