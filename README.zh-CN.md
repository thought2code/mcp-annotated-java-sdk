<div align="center">

[English](README.md) · 简体中文

# [MCP Annotated Java SDK](https://github.com/thought2code/mcp-annotated-java-sdk)

*注解驱动 MCP 开发 — 无需 Spring、零样板代码、纯 Java。*

**用注解代替样板代码，在 Java 中构建 MCP 服务端。**

[快速开始](#-快速开始) · [为什么选择本 SDK](#-为什么选择本-sdk) · [文档](#-相关链接) · [许可证](#-许可证)

![Java](https://img.shields.io/badge/Java-17+-blue)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.thought2code/mcp-annotated-java-sdk?color=blue)](https://central.sonatype.com/artifact/io.github.thought2code/mcp-annotated-java-sdk)
[![Coverage](https://img.shields.io/codecov/c/github/thought2code/mcp-annotated-java-sdk?logo=codecov&color=brightgreen)](https://app.codecov.io/github/thought2code/mcp-annotated-java-sdk)
[![GitHub Action](https://github.com/thought2code/mcp-annotated-java-sdk/actions/workflows/maven-build.yml/badge.svg)](https://github.com/thought2code/mcp-annotated-java-sdk/actions/workflows/maven-build.yml)

</div>

---

## 概述

本 SDK 是一个轻量级的注解框架，用于简化 Java 中的 MCP 服务端开发。你可以用极少的代码定义、开发并集成 MCP 资源（Resources）、提示词（Prompts）与工具（Tools），且**无需 Spring 框架**。

> **工作流：** 添加依赖 → 配置 `mcp-server.yml` → 注解 Resources / Tools / Prompts / Completions → 通过 `McpApplication` 启动

[📖 文档](https://thought2code.github.io/mcp-annotated-java-sdk-docs) · [💡 示例](https://github.com/thought2code/mcp-java-sdk-examples/tree/main/mcp-server-filesystem/mcp-server-filesystem-annotated-sdk-implementation) · [🐛 提交 Issue](https://github.com/thought2code/mcp-annotated-java-sdk/issues)

---

## ✨ 为什么选择本 SDK？

### 核心优势

- 🚫 **无需 Spring 框架** — 纯 Java 实现，轻量、快速
- ⚡ **一行启动 MCP 服务** — 最少只需一行代码即可运行服务端
- 🎉 **零样板代码** — 无需编写底层 MCP SDK 胶水代码
- 👏 **无需手写 JSON Schema** — 告别复杂冗长的 JSON 定义
- 🎯 **专注业务逻辑** — 把精力放在核心功能上
- 🔌 **兼容 Spring AI 配置** — 配置文件格式与 Spring AI 框架兼容
- 📦 **类型安全** — 充分利用 Java 类型系统在编译期保障正确性

### 与[官方 MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk)对比

| 特性       | 官方 MCP SDK | 本 SDK      |
|------------|--------------|-------------|
| 所需代码量 | 约 50–100 行 | 约 5–10 行  |
| JSON Schema | 手写 JSON   | 无需关心    |
| 类型安全   | 有限         | 完整        |
| 学习曲线   | 较陡         | 平缓        |

## 🎯 快速开始

### 环境要求

- **Java 17 或更高版本**（官方 MCP Java SDK 的硬性要求）

### 5 分钟上手

#### 第 1 步：添加依赖

**Maven：**

```xml
<dependency>
    <groupId>io.github.thought2code</groupId>
    <artifactId>mcp-annotated-java-sdk</artifactId>
    <version>latest-release-version</version>
</dependency>
```

**Gradle：**

```gradle
implementation 'io.github.thought2code:mcp-annotated-java-sdk:latest-release-version'
```

常规使用请填写 Maven Central 上最新的非 SNAPSHOT 版本。当前开发分支版本是 `0.19.0-SNAPSHOT`；仅在你已自行发布或安装快照，或从源码构建时使用该版本。

#### 第 2 步：创建配置文件

在 `src/main/resources` 下创建 `mcp-server.yml`：

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

#### 第 3 步：创建 MCP 服务端入口

```java
@McpServerApplication
public class MyFirstMcpServer {
    public static void main(String[] args) {
        McpApplication.run(MyFirstMcpServer.class, args);
    }
}
```

#### 第 4 步：定义 MCP 资源（可选）

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

#### 第 5 步：定义 MCP 工具

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

#### 第 6 步：定义 MCP 提示词（可选）

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

#### 第 7 步：运行服务端

```bash
# 编译项目
./mvnw clean package
```

在 IDE 中运行 `MyFirstMcpServer`，或使用 `java -cp ...` 指定编译产物与依赖 classpath。若希望用 `java -jar` 单文件启动，需在你的项目中自行配置可执行 JAR（例如 Spring Boot 或 Maven Shade 插件）。

完成！你的 MCP 服务端已可提供资源、工具与提示词。

## 📚 核心概念

### 什么是 MCP？

[Model Context Protocol（MCP）](https://modelcontextprotocol.io) 是一套标准化协议，用于构建向 LLM 应用暴露数据与能力的服务端。可以把它理解为面向 LLM 交互场景设计的「专用 API」。

### MCP 组件

| 组件            | 作用         | 类比      |
|---------------|------------|---------|
| **Resources** | 向 LLM 暴露数据 | GET 接口  |
| **Tools**     | 执行操作       | POST 接口 |
| **Prompts**   | 可复用模板      | 表单模板    |

### 支持的服务端模式

本 SDK 支持两种 MCP 服务端传输模式：

| 模式             | 说明             | 适用场景                                       |
|----------------|----------------|--------------------------------------------|
| **STDIO**      | 标准输入/输出通信      | CLI 工具、本地开发                                |
| **STREAMABLE** | HTTP 流式传输      | Web 应用、生产环境（推荐）                            |

## 🔧 进阶用法

### 配置文件

在 classpath 中创建 `mcp-server.yml`：

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

### 配置项说明

| 配置项                               | 说明                                    | 默认值             |
|-----------------------------------|---------------------------------------|-----------------|
| `enabled`                         | 是否启用 MCP 服务端                          | `true`          |
| `mode`                            | 服务端模式：`STDIO`、`STREAMABLE`            | `STREAMABLE`    |
| `name`                            | 服务端名称                                 | `mcp-server`    |
| `version`                         | 服务端版本                                 | `1.0.0`         |
| `type`                            | 服务端类型：`SYNC`、`ASYNC`                  | `SYNC`          |
| `instructions`                    | 给 LLM 客户端的说明                          | 必填（YAML 中不可为空）  |
| `request-timeout`                 | 请求超时（毫秒）                              | `20000`         |
| `capabilities.resource`           | 启用资源能力                                | `true`          |
| `capabilities.subscribe-resource` | 启用资源订阅                                | `true`          |
| `capabilities.prompt`             | 启用提示词能力                               | `true`          |
| `capabilities.tool`               | 启用工具能力                                | `true`          |
| `capabilities.completion`         | 启用补全能力                                | `true`          |
| `change-notification.resource`    | 资源变更时通知客户端                            | `true`          |
| `change-notification.prompt`      | 提示词变更时通知客户端                           | `true`          |
| `change-notification.tool`        | 工具变更时通知客户端                            | `true`          |
| `streamable.mcp-endpoint`         | Streamable HTTP MCP 路径                | `/mcp/message`  |
| `streamable.disallow-delete`      | 是否拒绝会话 HTTP DELETE                    | `false`         |
| `streamable.keep-alive-interval`  | 保活间隔（毫秒）                              | `20000`         |
| `streamable.port`                 | STREAMABLE 模式 HTTP 端口                 | `8080`          |

### 基于 Profile 的配置

在基础配置中设置 `profile` 后，会从 classpath 加载 `mcp-server-{profile}.yml`，并通过 Jackson deep merge 与基础配置合并；`capabilities`、`streamable` 等嵌套对象按字段递归覆盖。`profile` 名称始终取自基础配置文件。合并完成后，会清除与当前 `mode` 不匹配的传输配置（例如 `mode: STDIO` 时会移除 `streamable`）。

可通过 Profile 区分不同环境：

```yaml
# mcp-server.yml（基础配置）
enabled: true
mode: STREAMABLE
name: my-mcp-server
version: 1.0.0
profile: dev
```

```yaml
# mcp-server-dev.yml（Profile 覆盖配置）
streamable:
  port: 8080
```

### 运行时模型与稳定性

#### SYNC 与 ASYNC（`type`）

`type` 配置项决定框架使用哪一种 **MCP Java SDK 服务端 API**，**不会**把你的组件方法变成响应式代码。

| `type`  | 行为                                                                                                            |
|---------|---------------------------------------------------------------------------------------------------------------|
| `SYNC`  | 在请求线程上直接调用 `@McpTool` / `@McpPrompt` / `@McpResource` 方法。                                                     |
| `ASYNC` | 为兼容 MCP SDK 的异步 API，handler 返回 Reactor `Mono`。SDK 用 `Mono.fromCallable(...)` 包装调用 — 方法体仍是普通的 **阻塞式** Java 调用。 |

**ASYNC 不是非阻塞或 Project Reactor 编程模型。** 注解方法中无需也不应返回 `Mono`/`Flux`。耗时或 CPU 密集的逻辑仍会占用 Reactor 工作线程。除非部署环境明确要求 async MCP 服务端 API，否则优先使用 **SYNC**。高并发场景请保持 handler 简短，并合理设置 `request-timeout`。

#### 组件实例与并发

SDK 为每个组件类创建 **唯一实例**（无参构造），该类上所有 MCP 请求共享该对象。

- 组件类应 **无状态**，或仅持有 **线程安全** 的可变状态。
- 不要在实例字段中存放未同步的 per-request 数据。
- 需要共享可变状态时，请委托给线程安全的服务层。

当前 `McpApplicationContext.from(...)` 使用默认的单例-per-class 工厂；组件类需提供 **public 无参构造器**。公开 API 尚未内置 Spring/CDI 集成。

`McpApplication.run(mainClass, args)` 默认加载 `mcp-server.yml`；可通过第三个参数指定 classpath 中的其他配置文件名。

#### MCP Java SDK 2.x（候选版本）

本项目基于官方 [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) **2.0.0-RC1**，属于 **预发布候选版本**。2.0 正式版发布前 API 仍可能变更 — 请锁定依赖版本，升级后重新跑集成测试。

- 当前支持的 HTTP 传输为 **STREAMABLE**。

## 🏗️ 推荐项目结构

```
your-mcp-project/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           ├── MyMcpServer.java         # 入口类
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
```

## 🧪 测试

运行单元测试（请使用 Maven Wrapper 构建，勿使用系统安装的 `mvn`）：

```bash
./mvnw clean test
```

Windows：

```bash
mvnw.cmd clean test
```

## ❓ 常见问题

### 问：必须使用 Spring 框架吗？

**答：** 不需要。本 SDK 完全独立于 Spring。若你后续希望迁移到 Spring AI，配置文件格式是兼容的。

### 问：可以用于生产环境吗？

**答：** 注解层用于开发与测试已较稳定，但依赖官方 MCP Java SDK **2.0.0-RC1**（候选版本）。生产使用前请锁定依赖版本、跑完自己的集成测试，并关注 2.0 正式版发布前可能的 SDK API 变更。

### 问：`type: ASYNC` 是什么意思？

**答：** 表示使用底层 SDK 的 async MCP 服务端 API。你的 `@McpTool` / `@McpPrompt` / `@McpResource` 方法仍是普通阻塞 Java 代码，框架会用 `Mono.fromCallable(...)` 包装。无特殊需求时请优先使用 SYNC。

### 问：组件类是单例吗？

**答：** 是。SDK 为每个组件类创建一个实例并在所有请求间复用。请保持组件无状态或线程安全，不要在实例字段中存放未同步的 per-request 可变状态。

### 问：需要什么 Java 版本？

**答：** 需要 Java 17 或更高版本，这是底层 MCP Java SDK 的要求。

### 问：需要什么 Maven 版本？

**答：** 直接使用项目自带的 Maven Wrapper（`./mvnw` 或 `mvnw.cmd`）即可构建。

### 问：如何调试 MCP 服务端？

**答：** 可使用 [MCP Inspector](https://github.com/modelcontextprotocol/inspector)，并在 Java 代码中设置断点进行调试。

### 问：应该选择哪种服务端模式？

**答：**

- **STDIO**：CLI 工具与本地开发
- **STREAMABLE**：Web 应用与生产部署（推荐）

## 🤝 参与贡献

欢迎贡献代码与文档！建议流程：

1. **Fork 本仓库**
2. **创建功能或修复分支**
3. **为改动补充测试**
4. **必要时更新文档**
5. **确保测试通过**
6. **提交 Pull Request**，并清晰描述变更内容

### 本地开发

```bash
# 克隆仓库
git clone https://github.com/thought2code/mcp-annotated-java-sdk.git
cd mcp-annotated-java-sdk

# 构建项目（validate 阶段会执行 Spotless 格式检查）
./mvnw clean install

# 运行测试
./mvnw clean test
```

## 📖 相关链接

- [官方文档](https://thought2code.github.io/mcp-annotated-java-sdk-docs)
- [示例仓库](https://github.com/thought2code/mcp-java-sdk-examples/tree/main/mcp-server-filesystem/mcp-server-filesystem-annotated-sdk-implementation)
- [MCP 官网](https://modelcontextprotocol.io)

## 📄 许可证

本项目采用 [MIT License](LICENSE)。

## 🙏 致谢

- [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) — 底层 MCP 实现
- [Model Context Protocol](https://modelcontextprotocol.io) — 协议规范

> [!NOTE]
> 本项目仍在积极开发中，欢迎反馈与贡献！
