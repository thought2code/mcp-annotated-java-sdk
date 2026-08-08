---
title: 快速开始
description: 使用纯 Java 构建第一个注解驱动的 MCP 服务器。
---

本指南将帮助你在 5 分钟内构建第一个 MCP 服务器。

如果你希望在不引入 Spring 运行时的情况下，使用纯 Java 构建注解驱动的 MCP 服务器，可以选择本 SDK。如果你正在开发 Spring Boot 应用，Spring AI MCP 通常是更合适的默认选择；本指南专注于面向 CLI 工具、嵌入式服务器、本地自动化和小型服务进程的轻量级 Java 方案。

## 环境要求

- **Java 17 或更高版本**（官方 MCP Java SDK 的要求）

## 安装

### Maven 依赖

```xml
<dependency>
    <groupId>io.github.thought2code</groupId>
    <artifactId>mcp-annotated-java-sdk</artifactId>
    <version>0.20.0</version>
</dependency>
```

### Gradle 依赖

```groovy
implementation 'io.github.thought2code:mcp-annotated-java-sdk:0.20.0'
```

## 5 分钟教程

### 第 1 步：创建配置文件

在 `src/main/resources` 中创建 `mcp-server.yml`：

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

### 第 2 步：创建 MCP 服务器主类

```java
@McpServerApplication
public class MyFirstMcpServer {
    public static void main(String[] args) {
        McpApplication.run(MyFirstMcpServer.class, args);
    }
}
```

### 第 3 步：定义 MCP 资源（可选）

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

### 第 4 步：定义 MCP 工具

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

### 第 5 步：定义 MCP 提示词（可选）

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

### 第 6 步：运行服务器

```bash
# 编译项目
./mvnw clean package
```

可以从 IDE 中运行 `MyFirstMcpServer`，也可以使用 `java -cp ...`，将编译后的类和依赖加入 classpath 后运行。如果希望通过单个文件执行 `java -jar`，请在自己的项目中配置可执行 JAR。

如需加载非默认名称的配置文件，请使用 `McpApplication.run(MyFirstMcpServer.class, args, "custom-mcp-server.yml")`。

### 打包可执行 Fat JAR

部署时，请将应用打包为可执行 Fat JAR，确保 MCP Java SDK、本 SDK 及全部传递依赖在运行时可用。请将 `mcp-server.yml` 保留在 `src/main/resources` 下，以便它被包含在运行时 classpath 中。

使用 Maven Shade 时，配置 JAR 清单中的主类：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.6.2</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <createDependencyReducedPom>false</createDependencyReducedPom>
                <transformers>
                    <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                        <mainClass>com.example.MyFirstMcpServer</mainClass>
                    </transformer>
                </transformers>
            </configuration>
        </execution>
    </executions>
</plugin>
```

然后运行：

```bash
java -jar target/your-app.jar
```

如果使用 Gradle Shadow，请在清单中配置主类：

```groovy
tasks.shadowJar {
    manifest {
        attributes 'Main-Class': 'com.example.MyFirstMcpServer'
    }
}
```

## 服务器模式

本 SDK 支持两种 MCP 服务器模式。通过 YAML 加载配置时，`mcp-server.yml` 中的 `mode` 字段是必填项，不存在隐式默认值（仅通过 `ServerConfiguration.builder()` 以编程方式构建配置时才会应用默认值）。

### 1. STDIO 模式

基于标准输入/输出通信，适用于 CLI 工具和本地开发。

```yaml
# mcp-server.yml
mode: STDIO
```

### 2. STREAMABLE 模式

面向 Web 应用的 HTTP 流式传输，推荐用于生产环境。

```yaml
# mcp-server.yml
mode: STREAMABLE
streamable:
  mcp-endpoint: /mcp/message
  disallow-delete: false
  keep-alive-interval: 20000
  port: 8080
```

**STREAMABLE** 是本 SDK 支持的 HTTP 传输方式。

## 配置属性

通过 YAML 加载配置时，下表中的核心字段及适用的嵌套设置均为**必填项**；缺少必填字段会使服务器启动失败，并抛出 `Missing config key '...'`。条件字段仅在对应功能或传输方式启用时才是必填项。“构建器默认值”仅表示通过 `ServerConfiguration.builder()` 以编程方式构建配置时采用的值。

| 属性 | 说明 | 构建器默认值 |
|---|---|---|
| `enabled` | 启用或禁用 MCP 服务器 | `true` |
| `mode` | 服务器模式：`STDIO`、`STREAMABLE` | `STREAMABLE` |
| `name` | 服务器名称 | `mcp-server` |
| `version` | 服务器版本 | `1.0.0` |
| `type` | 服务器类型：`SYNC`、`ASYNC` | `SYNC` |
| `instructions` | 提供给 LLM 客户端的指令 | *（空字符串）* |
| `request-timeout` | 请求超时时间（毫秒） | `20000` |
| `capabilities.resource` | 启用资源支持 | `true` |
| `capabilities.subscribe-resource` | 启用资源订阅 | `true` |
| `capabilities.prompt` | 启用提示词支持 | `true` |
| `capabilities.tool` | 启用工具支持 | `true` |
| `capabilities.completion` | 启用补全支持 | `true` |
| `change-notification.resource` | 资源变化时通知客户端 | `true` |
| `change-notification.prompt` | 提示词变化时通知客户端 | `true` |
| `change-notification.tool` | 工具变化时通知客户端 | `true` |
| `streamable.mcp-endpoint` | Streamable HTTP 的 MCP 路径 | `/mcp/message` |
| `streamable.disallow-delete` | 拒绝针对会话的 HTTP DELETE 请求 | `false` |
| `streamable.keep-alive-interval` | 保活间隔（毫秒） | `20000` |
| `streamable.port` | STREAMABLE 模式使用的 HTTP 端口 | `8080` |

条件要求：

- 仅当 `capabilities.resource` 为 `true` 时，`capabilities.subscribe-resource` 才是必填项。
- 仅当 `mode` 为 `STREAMABLE` 时，`streamable.*` 字段才是必填项。`mode` 为 `STDIO` 时会忽略 `streamable` 部分，因此可以省略。

## 运行时模型与稳定性

### SYNC 与 ASYNC（`type`）

`type` 属性用于选择 MCP Java SDK 的服务器 API（`SYNC` 或 `ASYNC`），它**不会**让注解方法变为响应式方法。

- **SYNC** — 方法在请求线程上运行。
- **ASYNC** — SDK 提供异步处理器，并通过 `Mono.fromCallable(...)` 包装你的方法。代码仍然是**阻塞式** Java；`@McpTool`、`@McpPrompt` 和 `@McpResource` 方法不能返回 `Mono`。

默认应使用 **SYNC**。仅当部署环境需要 MCP 异步服务器 API 时才选择 **ASYNC**。在 ASYNC 模式下，耗时任务仍会阻塞 Reactor 工作线程。

### 组件实例与并发

SDK 会为每个组件类创建**一个实例**（通过**公共无参构造方法**），并在所有请求之间复用。并发 MCP 调用会共享该对象，因此组件应保持**无状态**或保证**线程安全**，并避免使用未同步的实例字段保存请求级数据。

## 基于 Profile 的配置

在基础文件中设置 `profile`，即可从 classpath 加载 `mcp-server-{profile}.yml`。Profile 配置会通过 Jackson 深度合并到基础配置中，`capabilities` 和 `streamable` 等嵌套对象会逐字段合并。`profile` 名称始终取自基础文件。合并后，与最终 `mode` 不匹配的传输设置会被清除（例如 `mode` 为 `STDIO` 时会移除 `streamable`）。

可以针对不同环境使用 Profile：

```yaml
# mcp-server.yml（基础配置）
enabled: true
mode: STREAMABLE
name: my-mcp-server
version: 1.0.0
profile: dev
```

```yaml
# mcp-server-dev.yml（Profile 专用配置）
streamable:
  port: 8080
```

## 项目结构

典型的项目结构如下：

```
your-mcp-project/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           ├── MyMcpServer.java         # 主入口
│   │   │           ├── components/
│   │   │           │   ├── MyResources.java     # MCP 资源
│   │   │           │   ├── MyTools.java         # MCP 工具
│   │   │           │   └── MyPrompts.java       # MCP 提示词
│   │   │           └── service/
│   │   │               └── BusinessLogic.java   # 业务逻辑
│   │   └── resources/
│   │       └── mcp-server.yml                   # MCP 配置
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── McpServerTest.java       # 单元测试
└── target/
    └── *.jar                                    # 构建产物（名称取决于项目）
```

## 后续步骤

- 想进一步了解 MCP 组件？请参阅[核心组件](/mcp-annotated-java-sdk/zh-cn/reference/components/)
