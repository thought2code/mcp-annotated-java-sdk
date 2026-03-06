---
hide:
    - navigation
---

# Getting Started Guide

This guide will help you build your first MCP server in 5 minutes.

## Requirements

- **Java 17 or later** (restricted by official MCP Java SDK)
- **Maven 3.6+** or **Gradle 7+**

## Installation

### Maven Dependency

```xml
<dependency>
    <groupId>io.github.thought2code</groupId>
    <artifactId>mcp-annotated-java-sdk</artifactId>
    <version>0.12.0</version>
</dependency>
```

### Gradle Dependency

```gradle
implementation 'io.github.thought2code:mcp-annotated-java-sdk:0.12.0'
```

## 5-Minutes Tutorial

### Step 1: Create MCP Server Main Class

```java
@McpServerApplication
public class MyFirstMcpServer {
    /**
     * Main method to start the MCP server.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        McpServerConfiguration.Builder configuration =
            McpServerConfiguration.builder().name("my-first-mcp-server").version("1.0.0");
        McpServers.run(MyFirstMcpServer.class, args).startStdioServer(configuration);
    }
}
```

### Step 2: Define MCP Resources (Optional)

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

### Step 3: Define MCP Tools

```java
public class MyTools {
    @McpTool(description = "Calculate the sum of two numbers")
    public int add(
        @McpToolParam(name = "a", description = "First number", required = true) int a,
        @McpToolParam(name = "b", description = "Second number", required = true) int b
    ) {
        return a + b;
    }
}
```

### Step 4: Define MCP Prompts (Optional)

```java
public class MyPrompts {
    @McpPrompt(description = "Generate code for a given task")
    public String generateCode(
        @McpPromptParam(name = "language", description = "Programming language", required = true) String language,
        @McpPromptParam(name = "task", description = "Task description", required = true) String task
    ) {
        return String.format("Write %s code to: %s", language, task);
    }
}
```

### Step 5: Run the Server

```bash
# Compile and run
mvn clean package
java -jar target/your-app.jar
```

## Server Modes

This SDK supports three MCP server modes:

### 1. STDIO Mode (Default)
Based on standard input/output communication, suitable for CLI tools.

```java
// Start STDIO server
McpServers.run(MyMcpServer.class, args).startStdioServer(configuration);
```

### 2. SSE (Server-Sent Events) Mode
HTTP-based real-time communication (deprecated).

```java
// Start SSE server
McpServers.run(MyMcpServer.class, args).startSseServer(configuration);
```

### 3. Streamable HTTP Mode
HTTP streaming for web applications.

```java
// Start Streamable HTTP server
McpServers.run(MyMcpServer.class, args).startStreamableServer(configuration);
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
│   │   │           ├── MyMcpServer.java          # Main entry point
│   │   │           ├── components/
│   │   │           │   ├── MyResources.java     # MCP Resources
│   │   │           │   ├── MyTools.java         # MCP Tools
│   │   │           │   └── MyPrompts.java       # MCP Prompts
│   │   │           └── service/
│   │   │               └── BusinessLogic.java    # Business logic
│   │   └── resources/
│   │       ├── mcp-server.yml                    # MCP configuration
│   │       └── messages.properties               # Internationalization messages
└── target/
    └── your-app.jar                              # Executable JAR
```

## Next Steps

- Want to learn more about MCP components? Check [Core Components](./components.md)
