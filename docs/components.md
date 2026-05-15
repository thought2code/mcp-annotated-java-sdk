---
hide:
    - navigation
    - toc
---

# Core Components

MCP (Model Context Protocol) defines three core component types, and this SDK simplifies the creation process of these components through annotations.

## Resources

Resource components are used to expose data to LLMs, similar to GET requests in Web APIs.

### Basic Usage

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

### Annotation Parameters

| Parameter     | Description                                    | Required                                  |
|---------------|------------------------------------------------|-------------------------------------------|
| `uri`         | Unique identifier of the resource (URI format) | Yes                                       |
| `description` | Resource description for LLM understanding     | No (defaults to `name`, then method name) |
| `name`        | Resource name (defaults to method name)        | No                                        |
| `title`       | Resource title (defaults to `name`)            | No                                        |
| `mimeType`    | MIME type of the resource content              | No (default `text/plain`)                 |

## Tools

Tool components are used to execute operations or calculations, similar to POST requests in Web APIs.

### Basic Usage

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

### Annotation Parameters

#### @McpTool

| Parameter     | Description                            | Required                                       |
|---------------|----------------------------------------|------------------------------------------------|
| `description` | Tool description for LLM understanding | No (defaults to tool `name`, then method name) |
| `name`        | Tool name (defaults to method name)    | No                                             |
| `title`       | Tool title for display purposes        | No                                             |

#### @McpToolParam

| Parameter     | Description                       | Required                |
|---------------|-----------------------------------|-------------------------|
| `name`        | Parameter name                    | Yes                     |
| `description` | Parameter description             | No (defaults to `name`) |
| `required`    | Whether the parameter is required | No (default `true`)     |

- `@McpTool`: Marks a method as an MCP tool
- `@McpToolParam`: Marks method parameters as tool parameters
  - `name`: Parameter name
  - `description`: Parameter description
  - `required`: Whether the parameter is required (default `true`)

## Prompts

Prompt components are used to define reusable prompt templates.

### Basic Usage

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

### Annotation Parameters

#### @McpPrompt

| Parameter     | Description                              | Required                                         |
|---------------|------------------------------------------|--------------------------------------------------|
| `description` | Prompt description for LLM understanding | No (defaults to prompt `name`, then method name) |
| `name`        | Prompt name (defaults to method name)    | No                                               |
| `title`       | Prompt title for display purposes        | No                                               |

#### @McpPromptParam

| Parameter     | Description                       | Required                |
|---------------|-----------------------------------|-------------------------|
| `name`        | Parameter name                    | Yes                     |
| `description` | Parameter description             | No (defaults to `name`) |
| `required`    | Whether the parameter is required | No (default `true`)     |

## Completions

Completions provide auto-complete suggestions for resource URIs and prompt arguments.

Handlers must **return** `McpCompleteCompletion` and take **exactly one** parameter of type `McpSchema.CompleteRequest.CompleteArgument` (the argument being completed has `name()` and `value()` from the MCP request).

### Resource Completions

```java
import com.github.thought2code.mcp.annotated.annotation.McpResourceCompletion;
import com.github.thought2code.mcp.annotated.server.component.McpCompleteCompletion;
import io.modelcontextprotocol.spec.McpSchema;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class MyCompletions {
  @McpResourceCompletion(uri = "file://")
  public McpCompleteCompletion completeFileUri(McpSchema.CompleteRequest.CompleteArgument argument) {
    String prefix = argument.value() != null ? argument.value() : "";
    try {
      List<String> paths =
          Files.list(Paths.get(prefix.isEmpty() ? "." : prefix))
              .map(Path::toString)
              .limit(50)
              .collect(Collectors.toList());
      return McpCompleteCompletion.builder()
          .values(paths)
          .total(paths.size())
          .hasMore(false)
          .build();
    } catch (Exception e) {
      return McpCompleteCompletion.empty();
    }
  }
}
```

### Prompt Completions

`@McpPromptCompletion.name` must match the **registered prompt name** (by default, the Java method name of the `@McpPrompt` method). Filter by `argument.name()` when one prompt has multiple parameters.

```java
import com.github.thought2code.mcp.annotated.annotation.McpPromptCompletion;
import com.github.thought2code.mcp.annotated.server.component.McpCompleteCompletion;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;

public class MyPromptCompletions {
  @McpPromptCompletion(name = "generateCode")
  public McpCompleteCompletion completeGenerateCode(McpSchema.CompleteRequest.CompleteArgument argument) {
    if (!"language".equals(argument.name())) {
      return McpCompleteCompletion.empty();
    }
    return McpCompleteCompletion.builder()
        .values(List.of("Java", "Python", "JavaScript", "Go", "Rust"))
        .total(5)
        .hasMore(false)
        .build();
  }
}
```

## Multilingual Support

This SDK has built-in multilingual support, which can be enabled through the `@McpI18nEnabled` annotation.

### Enable i18n

```java
@McpServerApplication
@McpI18nEnabled(resourceBundleBaseName = "messages")
public class I18nMcpServer {
    public static void main(String[] args) {
        McpApplication.run(I18nMcpServer.class, args);
    }
}
```

### Create Resource Bundles

Create `messages.properties` file:

```properties
# messages.properties
tool.add.description=Calculate the sum of two numbers
tool.add.param.a.description=First number
tool.add.param.b.description=Second number
resource.system.info.description=System information
prompt.generate.code.description=Generate code for a given task
prompt.generate.code.param.language.description=Programming language
prompt.generate.code.param.task.description=Task description
```

Create `messages_zh_CN.properties` file:

```properties
# messages_zh_CN.properties
tool.add.description=计算两个数字的和
tool.add.param.a.description=第一个数字
tool.add.param.b.description=第二个数字
resource.system.info.description=系统信息
prompt.generate.code.description=根据任务描述生成代码
prompt.generate.code.param.language.description=编程语言
prompt.generate.code.param.task.description=任务描述
```

### Use i18n in Components

```java
@McpTool(description = "tool.add.description")
public int add(
    @McpToolParam(name = "a", description = "tool.add.param.a.description") int a,
    @McpToolParam(name = "b", description = "tool.add.param.b.description") int b
) {
    return a + b;
}
```

## Automatic Registration

After defining MCP components, they will be automatically registered to the server. You just need to ensure that the component classes are in the package scanning path of the server application.

### Specify Package Path

If you need to specify a specific package path, you can use the following methods:

```java
@McpServerApplication(basePackageClass = MyMcpServer.class)
// or
@McpServerApplication(basePackage = "com.example.mcp.components")
```

If no package path is specified, the package containing the main method will be scanned.

## Structured Content

Tools can return structured content for rich responses by returning a type that **implements** `McpStructuredContent` (often a `record` with `@McpJsonSchemaProperty` on fields). There is no `McpStructuredContent.of(...)` helper in the API.

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

## Error Handling

If a tool method **throws any exception**, the server returns a `CallToolResult` with `isError` set to `true` and a generic method-invocation error message (the exception message is not forwarded to the client today).

For expected failures such as validation, return a normal value (for example a `String`) so the tool call remains a successful result with `isError` false:

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
