---
title: 核心组件
description: 使用 Java 注解定义 MCP 资源、工具、提示词、补全和结构化内容。
---

MCP（模型上下文协议）定义了三种核心组件类型，本 SDK 通过注解简化了这些组件的创建过程。

## 资源

资源组件用于向 LLM 暴露数据，类似于 Web API 中的 GET 请求。

### 基本用法

```java
import com.github.thought2code.mcp.annotated.annotation.McpResource;

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

### 注解参数

| 参数 | 说明 | 是否必填 |
|---|---|---|
| `uri` | 资源的唯一标识符（URI 格式） | 是 |
| `description` | 帮助 LLM 理解资源的说明 | 否（默认依次使用 `name`、方法名） |
| `name` | 资源名称 | 否（默认为方法名） |
| `title` | 资源标题 | 否（默认为 `name`） |
| `mimeType` | 资源内容的 MIME 类型 | 否（默认为 `text/plain`） |
| `roles` | 允许访问资源的角色 | 否（默认为 `ASSISTANT`、`USER`） |
| `priority` | 资源优先级 | 否（默认为 `1.0`） |

## 工具

工具组件用于执行操作或计算，类似于 Web API 中的 POST 请求。

### 基本用法

```java
import com.github.thought2code.mcp.annotated.annotation.McpTool;
import com.github.thought2code.mcp.annotated.annotation.McpToolParam;

public class MyTools {
    @McpTool(description = "Calculate the sum of two numbers")
    public int add(
        @McpToolParam(name = "a", description = "First number") int a,
        @McpToolParam(name = "b", description = "Second number") int b
    ) {
        return a + b;
    }

    @McpTool(description = "Read complete file contents with UTF-8 encoding")
    public String readFile(
        @McpToolParam(name = "path", description = "File path") String path
    ) {
        try {
            return Files.readString(Path.of(path));
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }
}
```

### 注解参数

#### @McpTool

| 参数 | 说明 | 是否必填 |
|---|---|---|
| `description` | 帮助 LLM 理解工具的说明 | 否（默认依次使用工具 `name`、方法名） |
| `name` | 工具名称 | 否（默认为方法名） |
| `title` | 用于展示的工具标题 | 否 |

#### @McpToolParam

| 参数 | 说明 | 是否必填 |
|---|---|---|
| `name` | 参数名称 | 是 |
| `description` | 参数说明 | 否（默认为 `name`） |
| `required` | 参数是否必填 | 否（默认为 `true`） |

- `@McpTool`：将方法标记为 MCP 工具。
- `@McpToolParam`：将方法参数标记为工具参数。
  - `name`：参数名称。
  - `description`：参数说明。
  - `required`：参数是否必填（默认为 `true`）。

## 提示词

提示词组件用于定义可复用的提示词模板。

### 基本用法

```java
import com.github.thought2code.mcp.annotated.annotation.McpPrompt;
import com.github.thought2code.mcp.annotated.annotation.McpPromptParam;

public class MyPrompts {
    @McpPrompt(description = "Generate code for a given task")
    public String generateCode(
        @McpPromptParam(name = "language", description = "Programming language") String language,
        @McpPromptParam(name = "task", description = "Task description") String task
    ) {
        return String.format("Write %s code to: %s", language, task);
    }

    @McpPrompt(description = "Format text as specified style")
    public String formatText(
        @McpPromptParam(name = "text", description = "Text to format") String text,
        @McpPromptParam(name = "style", description = "Format style (e.g., formal, casual, technical)") String style
    ) {
        return String.format("Rewrite the following text in a %s style: %s", style, text);
    }
}
```

### 注解参数

#### @McpPrompt

| 参数 | 说明 | 是否必填 |
|---|---|---|
| `description` | 帮助 LLM 理解提示词的说明 | 否（默认依次使用提示词 `name`、方法名） |
| `name` | 提示词名称 | 否（默认为方法名） |
| `title` | 用于展示的提示词标题 | 否 |

#### @McpPromptParam

| 参数 | 说明 | 是否必填 |
|---|---|---|
| `name` | 参数名称 | 是 |
| `description` | 参数说明 | 否（默认为 `name`） |
| `required` | 参数是否必填 | 否（默认为 `true`） |

## 补全

补全功能为资源 URI 和提示词参数提供自动补全建议。

处理方法必须**返回** `CompletionResult`，并且**只能接收一个** `McpSchema.CompleteRequest.CompleteArgument` 类型的参数。该参数表示正在补全的参数，其 `name()` 和 `value()` 来自 MCP 请求。

### 资源补全

`@McpResourceCompletion.uri` 必须与 `@McpResource` 上的 **`uri` 完全一致**，包括 `file://{path}` 这样的 URI 模板。

补全处理方法应与使用相同 URI 模式的资源配对：

```java
import com.github.thought2code.mcp.annotated.annotation.McpResource;
import com.github.thought2code.mcp.annotated.annotation.McpResourceCompletion;
import com.github.thought2code.mcp.annotated.server.component.completion.CompletionResult;
import io.modelcontextprotocol.spec.McpSchema;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class MyFileResources {
    @McpResource(uri = "file://{path}", description = "Read a file by path")
    public String readFile() {
        return "file content";
    }

    @McpResourceCompletion(uri = "file://{path}")
    public CompletionResult completeFileUri(McpSchema.CompleteRequest.CompleteArgument argument) {
        String prefix = argument.value() != null ? argument.value() : "";
        try {
            List<String> paths =
                    Files.list(Paths.get(prefix.isEmpty() ? "." : prefix))
                            .map(Path::toString)
                            .limit(50)
                            .collect(Collectors.toList());
            return CompletionResult.builder()
                    .values(paths)
                    .total(paths.size())
                    .hasMore(false)
                    .build();
        } catch (Exception e) {
            return CompletionResult.empty();
        }
    }
}
```

### 提示词补全

`@McpPromptCompletion.name` 必须与**已注册的提示词名称**一致：如果设置了 `@McpPrompt.name` 属性则使用该属性，否则使用 `@McpPrompt` 方法名。当一个提示词有多个参数时，应通过 `argument.name()` 进行筛选；该名称必须与正在补全的 `@McpPromptParam.name` 一致。

```java
import com.github.thought2code.mcp.annotated.annotation.McpPromptCompletion;
import com.github.thought2code.mcp.annotated.server.component.completion.CompletionResult;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

public class MyPromptCompletions {
    @McpPromptCompletion(name = "generateCode")
    public CompletionResult completeGenerateCode(McpSchema.CompleteRequest.CompleteArgument argument) {
        if (!"language".equals(argument.name())) {
            return CompletionResult.empty();
        }
        return CompletionResult.builder()
                .values(List.of("Java", "Python", "JavaScript", "Go", "Rust"))
                .total(5)
                .hasMore(false)
                .build();
    }
}
```

## 自动注册

定义 MCP 组件后，它们会自动注册到服务器。只需确保组件类位于服务器应用的注册范围内。

### 每个组件类一个实例

SDK 会为每个组件类创建一个对象（通过其**公共无参构造方法**），并使用同一实例调用该类上的所有注解方法。**并发请求会共享一个对象**，因此：

- 组件类必须提供可访问的无参构造方法。
- 尽可能让组件类保持无状态。
- 所有可变实例字段都必须是线程安全的，或者由你同步访问。
- 不要将实例字段作为请求级存储。

### SYNC 与 ASYNC 服务器类型

在 `mcp-server.yml` 中设置 `type: ASYNC` 会使用底层 SDK 的异步 MCP 服务器 API。处理器通过 `Mono.fromCallable(...)` 包装阻塞方法——**ASYNC 模式不等于 Project Reactor 编程模型**。`@McpTool`、`@McpPrompt` 和 `@McpResource` 方法仍然是普通的同步 Java 代码。

### 指定包路径

如需指定特定的包路径，可以使用以下方式：

```java
@McpServerApplication(basePackageClass = MyMcpServer.class)
// 或者
@McpServerApplication(basePackage = "com.example.mcp.components")
```

当主类同时提供多个选项时，按以下顺序解析：

1. `basePackageClass`（不为 `Object.class` 时）——使用该类所在的包。
2. 非空白的 `basePackage`。
3. 传递给 `McpApplication.run()` 的类所在的包。

最终包下的子包也会包含在扫描范围内，其他包中的类会被忽略。

## 结构化内容

工具可以通过返回**实现了** `McpStructuredContent` 的类型来提供结构化内容，以生成更丰富的响应。该类型通常可以是一个 `record`，并在字段上使用 `@McpJsonSchemaProperty`。API 中不存在 `McpStructuredContent.of(...)` 辅助方法。

```java
import com.github.thought2code.mcp.annotated.annotation.McpJsonSchemaDefinition;
import com.github.thought2code.mcp.annotated.annotation.McpJsonSchemaProperty;
import com.github.thought2code.mcp.annotated.annotation.McpTool;
import com.github.thought2code.mcp.annotated.annotation.McpToolParam;
import com.github.thought2code.mcp.annotated.server.McpStructuredContent;

public class UserTools {

  @McpJsonSchemaDefinition
  public record User(
      @McpJsonSchemaProperty(description = "User id") String id,
      @McpJsonSchemaProperty(description = "Display name") String name)
      implements McpStructuredContent {

    @Override
    public String asTextContent() {
      return "User " + id + ": " + name;
    }
  }

  @McpTool(description = "Get user details")
  public User getUser(@McpToolParam(name = "id", description = "User ID") String id) {
    return new User(id, "Ada");
  }
}
```

## 错误处理

如果工具方法**抛出任何异常**，服务器会返回 `isError` 为 `true` 的 `CallToolResult` 和通用的方法调用错误信息。目前不会将异常消息转发给客户端。

对于校验失败等预期错误，请返回普通值（例如 `String`），这样工具调用仍会得到 `isError` 为 `false` 的成功结果：

```java
@McpTool(description = "Divide two numbers")
public String divide(
    @McpToolParam(name = "a", description = "Dividend") double a,
    @McpToolParam(name = "b", description = "Divisor") double b) {
  if (b == 0) {
    return "Cannot divide by zero.";
  }
  return Double.toString(a / b);
}
```
