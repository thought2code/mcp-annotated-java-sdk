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

| Parameter      | Description                                    | Required |
|----------------|------------------------------------------------|----------|
| `uri`          | Unique identifier of the resource (URI format) | Yes      |
| `description`  | Resource description for LLM understanding     | Yes      |
| `name`         | Resource name (defaults to method name)        | No       |
| `mimeType`     | MIME type of the resource content              | No       |

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

| Parameter     | Description                              | Required |
|---------------|------------------------------------------|----------|
| `description` | Tool description for LLM understanding   | Yes      |
| `name`        | Tool name (defaults to method name)      | No       |
| `title`       | Tool title for display purposes          | No       |

#### @McpToolParam

| Parameter     | Description                              | Required |
|---------------|------------------------------------------|----------|
| `name`        | Parameter name                           | Yes      |
| `description` | Parameter description                    | Yes      |
| `required`    | Whether the parameter is required        | No       |

- `@McpTool`: Marks a method as an MCP tool
- `@McpToolParam`: Marks method parameters as tool parameters
  - `name`: Parameter name
  - `description`: Parameter description
  - `required`: Whether the parameter is required

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

| Parameter     | Description                              | Required |
|---------------|------------------------------------------|----------|
| `description` | Prompt description for LLM understanding | Yes      |
| `name`        | Prompt name (defaults to method name)    | No       |
| `title`       | Prompt title for display purposes        | No       |

#### @McpPromptParam

| Parameter     | Description                              | Required |
|---------------|------------------------------------------|----------|
| `name`        | Parameter name                           | Yes      |
| `description` | Parameter description                    | Yes      |
| `required`    | Whether the parameter is required        | No       |

## Completions

Completions provide auto-complete suggestions for resource URIs and prompt arguments.

### Resource Completions

```java
import com.github.thought2code.mcp.annotated.annotation.McpResourceCompletion;

public class MyCompletions {
    @McpResourceCompletion(uri = "file://")
    public List<String> completeFilePath(String uri, String cursorValue) {
        // Return matching file paths
        return Files.list(Paths.get(cursorValue))
            .map(Path::toString)
            .collect(Collectors.toList());
    }
}
```

### Prompt Completions

```java
import com.github.thought2code.mcp.annotated.annotation.McpPromptCompletion;

public class MyCompletions {
    @McpPromptCompletion(promptName = "generateCode", argumentName = "language")
    public List<String> completeLanguage(String value) {
        return Arrays.asList("Java", "Python", "JavaScript", "Go", "Rust");
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

Tools can return structured content for rich responses:

```java
@McpTool(description = "Get user details")
public McpStructuredContent<User> getUser(
    @McpToolParam(name = "id", description = "User ID") String id
) {
    User user = userService.findById(id);
    return McpStructuredContent.of(user);
}
```

## Error Handling

When a tool encounters an error, you can return an error result:

```java
@McpTool(description = "Divide two numbers")
public Number divide(
    @McpToolParam(name = "a", description = "Dividend") double a,
    @McpToolParam(name = "b", description = "Divisor") double b
) {
    if (b == 0) {
        return McpStructuredContent.error("Division by zero is not allowed");
    }
    return a / b;
}
```
