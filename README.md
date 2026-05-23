<div align="center">

English · [简体中文](README.zh-CN.md)

# [MCP Annotated Java SDK](https://github.com/thought2code/mcp-annotated-java-sdk)

*Annotation-driven MCP dev — No Spring, Zero Boilerplate, Pure Java.*

**Build MCP servers in Java with annotations instead of boilerplate.**

[Quick Start](#-quick-start) · [Why This SDK](#-why-this-sdk) · [Documentation](#-documentation) · [License](#-license)

![Java](https://img.shields.io/badge/Java-17+-blue)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.thought2code/mcp-annotated-java-sdk?color=blue)](https://central.sonatype.com/artifact/io.github.thought2code/mcp-annotated-java-sdk)
[![Commit Activity](https://img.shields.io/github/commit-activity/w/thought2code/mcp-annotated-java-sdk)](https://github.com/thought2code/mcp-annotated-java-sdk/graphs/commit-activity)
[![Coverage](https://img.shields.io/codecov/c/github/thought2code/mcp-annotated-java-sdk?logo=codecov&color=brightgreen)](https://app.codecov.io/github/thought2code/mcp-annotated-java-sdk)
[![GitHub Action](https://github.com/thought2code/mcp-annotated-java-sdk/actions/workflows/maven-build.yml/badge.svg)](https://github.com/thought2code/mcp-annotated-java-sdk/actions/workflows/maven-build.yml)

</div>

---

## Overview

This SDK is a lightweight, annotation-based framework that simplifies MCP server development in Java. Define, develop, and integrate your MCP Resources / Prompts / Tools with minimal code — **no Spring Framework required**.

> **Workflow:** Add dependency → Configure `mcp-server.yml` → Annotate Resources / Tools / Prompts → Run with `McpApplication`

[📖 Documentation](https://thought2code.github.io/mcp-annotated-java-sdk-docs) · [💡 Examples](https://github.com/thought2code/mcp-java-sdk-examples/tree/main/mcp-server-filesystem/mcp-server-filesystem-annotated-sdk-implementation) · [🐛 Report Issues](https://github.com/thought2code/mcp-annotated-java-sdk/issues)

---

## ✨ Why This SDK?

### Key Advantages

- 🚫 **No Spring Framework Required** - Pure Java, lightweight and fast
- ⚡ **Instant MCP Server** - Get your server running with just 1 line of code
- 🎉 **Zero Boilerplate** - No need to write low-level MCP SDK code
- 👏 **No JSON Schema** - Forget about complex and lengthy JSON definitions
- 🎯 **Focus on Logic** - Concentrate on your core business logic
- 🔌 **Spring AI Compatible** - Configuration file compatible with Spring AI Framework
- 🌍 **Multilingual Support** - Built-in i18n support for MCP components
- 📦 **Type-Safe** - Leverage Java's type system for compile-time safety

### Comparison with [Official MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk)

| Feature        | Official MCP SDK | This SDK        |
|----------------|------------------|-----------------|
| Code Required  | ~50-100 lines    | ~5-10 lines     |
| JSON Schema    | Hand-coded JSON  | No need to care |
| Type Safety    | Limited          | Full            |
| Learning Curve | Steep            | Gentle          |
| Multilingual   | Unsupported      | Supported       |

## 🎯 Quick Start

### Prerequisites

- **Java 17 or later** (required by official MCP Java SDK)

### 5-Minutes Tutorial

#### Step 1: Add Dependency

**Maven:**
```xml
<dependency>
    <groupId>io.github.thought2code</groupId>
    <artifactId>mcp-annotated-java-sdk</artifactId>
    <version>0.15.0</version>
</dependency>
```

**Gradle:**
```gradle
implementation 'io.github.thought2code:mcp-annotated-java-sdk:0.15.0'
```

#### Step 2: Create Configuration File

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
  prompt: true
  tool: true
change-notification:
  resource: true
  prompt: true
  tool: true
```

#### Step 3: Create Your MCP Server

```java
@McpServerApplication
public class MyFirstMcpServer {
    public static void main(String[] args) {
        McpApplication.run(MyFirstMcpServer.class, args);
    }
}
```

#### Step 4: Define MCP Resources (if needed)

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

#### Step 5: Define MCP Tools

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

#### Step 6: Define MCP Prompts (if needed)

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

#### Step 7: Run Your Server

```bash
# Compile your project
./mvnw clean package
```

Run `MyFirstMcpServer` from your IDE, or use `java -cp ...` with your compiled classes and dependencies on the classpath. Your own project needs an executable JAR setup (for example Spring Boot or the Maven Shade plugin) if you want `java -jar` with a single file.

That's it! Your MCP server is now ready to serve resources, tools, and prompts!

## 📚 Core Concepts

### What is MCP?

The [Model Context Protocol (MCP)](https://modelcontextprotocol.io) is a standardized protocol for building servers that expose data and functionality to LLM applications. Think of it like a web API, but specifically designed for LLM interactions.

### MCP Components

| Component     | Purpose            | Analogy        |
|---------------|--------------------|----------------|
| **Resources** | Expose data to LLM | GET endpoints  |
| **Tools**     | Execute actions    | POST endpoints |
| **Prompts**   | Reusable templates | Form templates |

### Supported Server Modes

This SDK supports three MCP server modes:

| Mode           | Description                         | Use Case                                     |
|----------------|-------------------------------------|----------------------------------------------|
| **STDIO**      | Standard input/output communication | CLI tools, local development                 |
| **SSE**        | Server-Sent Events (HTTP-based)     | Real-time web applications (deprecated)      |
| **STREAMABLE** | HTTP streaming                      | Web applications, recommended for production |

## 🔧 Advanced Usage

### Configuration File

Create `mcp-server.yml` in your classpath:

```yaml
enabled: true
mode: STREAMABLE
name: my-mcp-server
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
streamable:
  mcp-endpoint: /mcp/message
  disallow-delete: false
  keep-alive-interval: 20000
  port: 8080
```

### Configuration Properties

| Property                          | Description                               | Default        |
|-----------------------------------|-------------------------------------------|----------------|
| `enabled`                         | Enable/disable MCP server                 | `true`         |
| `mode`                            | Server mode: `STDIO`, `SSE`, `STREAMABLE` | `STREAMABLE`   |
| `name`                            | Server name                               | `mcp-server`   |
| `version`                         | Server version                            | `1.0.0`        |
| `type`                            | Server type: `SYNC`, `ASYNC`              | `SYNC`         |
| `instructions`                    | Instructions for the LLM client           | (empty)        |
| `request-timeout`                 | Request timeout in milliseconds           | `20000`        |
| `capabilities.resource`           | Enable resource support                   | `true`         |
| `capabilities.subscribe-resource` | Enable resource subscription              | `true`         |
| `capabilities.prompt`             | Enable prompt support                     | `true`         |
| `capabilities.tool`               | Enable tool support                       | `true`         |
| `capabilities.completion`         | Enable completion support                 | `true`         |
| `change-notification.resource`    | Notify clients on resource change         | `true`         |
| `change-notification.prompt`      | Notify clients on prompt change           | `true`         |
| `change-notification.tool`        | Notify clients on tool change             | `true`         |
| `sse.message-endpoint`            | SSE POST message path                     | `/mcp/message` |
| `sse.endpoint`                    | SSE stream path                           | `/sse`         |
| `sse.base-url`                    | Public base URL for the SSE server        | *(empty)*      |
| `sse.port`                        | HTTP port for SSE mode                    | `8080`         |
| `streamable.mcp-endpoint`         | Streamable HTTP MCP path                  | `/mcp/message` |
| `streamable.disallow-delete`      | Reject HTTP DELETE on session             | `false`        |
| `streamable.keep-alive-interval`  | Keep-alive interval (ms)                  | `20000`        |
| `streamable.port`                 | HTTP port for STREAMABLE mode             | `8080`         |

### Profile-based Configuration

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

### Runtime model and stability

#### SYNC vs ASYNC (`type`)

The `type` setting selects which **MCP Java SDK server API** the framework uses. It does **not** turn your component methods into reactive code.

| `type`  | What happens                                                                                                                                                                            |
|---------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `SYNC`  | Handlers invoke your `@McpTool` / `@McpPrompt` / `@McpResource` methods on the request thread.                                                                                          |
| `ASYNC` | Handlers return Reactor `Mono` values for MCP SDK compatibility. The SDK wraps each call in `Mono.fromCallable(...)` — your method body is still a normal **blocking** Java invocation. |

**ASYNC is not a non-blocking or Project Reactor programming model.** You do not implement `Mono`/`Flux` in annotated methods. Long-running or CPU-heavy work still occupies a Reactor worker thread. Use **SYNC** unless your deployment specifically requires the async MCP server API. For high concurrency, keep handlers short and tune `request-timeout`.

#### Component instances and concurrency

The SDK creates **one instance per component class** (no-arg constructor) and reuses it for every MCP request to methods on that class. Concurrent calls share the same object.

- Prefer **stateless** component classes, or **thread-safe** mutable state only.
- Do not store per-request data in instance fields without proper synchronization.
- Delegate shared mutable state to thread-safe services when needed.

`McpApplicationContext.from(...)` currently uses this default singleton-per-class factory. There is no built-in Spring/CDI wiring in the public API today.

#### MCP Java SDK 2.x (milestone)

This project builds on the official [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) **2.0.0-M3**, a **pre-release milestone**. APIs may change before 2.0 GA — pin dependency versions and re-run tests when upgrading.

- **STREAMABLE** is the recommended HTTP transport for new projects.
- **SSE** is still supported for compatibility but is **deprecated** in MCP SDK 2.x; avoid new SSE deployments.

### Multilingual Support (i18n)

Enable i18n for your MCP components:

```java
@McpServerApplication
@McpI18nEnabled(resourceBundleBaseName = "messages")
public class I18nMcpServer {
    public static void main(String[] args) {
        McpApplication.run(I18nMcpServer.class, args);
    }
}
```

Create resource bundles:

```properties
# messages.properties
tool.calculate.description=Calculate the sum of two numbers
tool.calculate.param.a.description=First number
tool.calculate.param.b.description=Second number
```

```properties
# messages_zh_CN.properties
tool.calculate.description=计算两个数字的和
tool.calculate.param.a.description=第一个数字
tool.calculate.param.b.description=第二个数字
```

Use i18n keys in your MCP components:

```java
@McpTool(description = "tool.calculate.description")
public int add(
    @McpToolParam(name = "a", description = "tool.calculate.param.a.description") int a,
    @McpToolParam(name = "b", description = "tool.calculate.param.b.description") int b
) {
    return a + b;
}
```

## 🏗️ Project Structure

A typical project structure:

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
│   │   │               └── BusinessLogic.java   # Your business logic
│   │   └── resources/
│   │       ├── mcp-server.yml                   # MCP configuration
│   │       └── messages.properties              # i18n messages
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── McpServerTest.java       # Unit tests
```

## 🧪 Testing

Run the test suite:

```bash
./mvnw clean test
```

## ❓ FAQ

### Q: Do I need Spring Framework?

**A:** No! This SDK is completely independent of Spring Framework. However, the configuration file format is compatible with Spring AI if you want to migrate.

### Q: Can I use this in production?

**A:** The annotated layer is stable for development and testing, but it depends on the official MCP Java SDK **2.0.0-M3** (a milestone release). Pin versions, run your own integration tests, and expect possible SDK API changes before 2.0 GA before relying on it in production.

### Q: What does `type: ASYNC` mean?

**A:** It selects the async MCP server API from the underlying SDK. Your `@McpTool` / `@McpPrompt` / `@McpResource` methods remain ordinary blocking Java code; the framework wraps them in `Mono.fromCallable(...)`. Use SYNC unless you specifically need the async server API.

### Q: Are component classes singletons?

**A:** Yes. The SDK creates one instance per component class and reuses it for all requests. Keep components stateless or thread-safe; do not keep per-request mutable state on the instance without synchronization.

### Q: What Java version is required?

**A:** Java 17 or later is required, as this is a constraint of the underlying MCP Java SDK.

### Q: What Maven version is required?

**A:** Just use the provided Maven wrapper script `./mvnw` to build this project.

### Q: How do I debug my MCP server?

**A:** You can use the [MCP Inspector](https://github.com/modelcontextprotocol/inspector) and set Java breakpoints to debug your MCP server.

### Q: Which server mode should I use?

**A:** 
- **STDIO**: For CLI tools and local development
- **STREAMABLE**: For web applications and production deployments (recommended)
- **SSE**: Deprecated, use STREAMABLE instead

## 🤝 Contributing

We welcome and appreciate contributions! Please follow these steps to contribute:

1. **Fork the repository**
2. **Create a new branch** for your feature or bug fix
3. **Add tests** for your changes
4. **Update documentation** if necessary
5. **Ensure all tests pass**
6. **Submit a pull request** with a clear description of your changes

### Development Setup

```bash
# Clone the repository
git clone https://github.com/thought2code/mcp-annotated-java-sdk.git
cd mcp-annotated-java-sdk

# Build the project
./mvnw clean install

# Run tests
./mvnw clean test
```

## 📖 Documentation

- [Official Documentation](https://thought2code.github.io/mcp-annotated-java-sdk-docs)
- [Examples Repository](https://github.com/thought2code/mcp-java-sdk-examples/tree/main/mcp-server-filesystem/mcp-server-filesystem-annotated-sdk-implementation)
- [MCP Official Site](https://modelcontextprotocol.io)

## 📄 License

This project is licensed under the [MIT License](LICENSE).

## 🙏 Acknowledgments

- [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) - The underlying MCP implementation
- [Model Context Protocol](https://modelcontextprotocol.io) - The protocol specification

> [!NOTE]
> This project is under active development. We appreciate your feedback and contributions!
