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

Handlers must **return** `CompletionResult` and take **exactly one** parameter of type `McpSchema.CompleteRequest.CompleteArgument` (the argument being completed has `name()` and `value()` from the MCP request).

### Resource Completions

```java
import com.github.thought2code.mcp.annotated.annotation.McpResourceCompletion;
import com.github.thought2code.mcp.annotated.server.component.completion.CompletionResult;
import io.modelcontextprotocol.spec.McpSchema;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class MyCompletions {
    @McpResourceCompletion(uri = "file://")
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

### Prompt Completions

`@McpPromptCompletion.name` must match the **registered prompt name** (by default, the Java method name of the `@McpPrompt` method). Filter by `argument.name()` when one prompt has multiple parameters.

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

## Automatic Registration

After defining MCP components, they will be automatically registered to the server. You just need to ensure that the component classes are within the registration scope of the server application.

### One instance per component class

The SDK creates a single object per component class (via its no-arg constructor) and invokes all annotated methods on that class through the same instance. **Concurrent requests share one object**, so:

- Keep component classes stateless when possible.
- Any mutable instance fields must be thread-safe, or you must synchronize access.
- Do not treat instance fields as per-request storage.

### SYNC vs ASYNC server type

Setting `type: ASYNC` in `mcp-server.yml` uses the async MCP server API from the underlying SDK. Handlers are implemented with `Mono.fromCallable(...)` around your blocking method — **ASYNC mode is not Project Reactor**. Your `@McpTool`, `@McpPrompt`, and `@McpResource` methods remain ordinary synchronous Java code.

### Specify Package Path

If you need to specify a specific package path, you can use the following methods:

```java
@McpServerApplication(basePackageClass = MyMcpServer.class)
// or
@McpServerApplication(basePackage = "com.example.mcp.components")
```

If no package path is specified, the package of the class passed to `McpApplication.run()` is used as the default registration scope.

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
