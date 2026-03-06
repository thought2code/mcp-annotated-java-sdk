---
hide:
    - navigation
    - toc
---

# mcp-annotated-java-sdk

Annotation-driven [MCP (Model Context Protocol)](https://modelcontextprotocol.io) SDK for Java that simplifies MCP server development.

## 🎯 What is MCP?

Model Context Protocol (MCP) is a standardized protocol for building servers that expose data and functionality to LLM applications. Similar to Web API, but specifically designed for LLM interactions.

## ✨ Why this SDK?

MCP helps you build agents and complex workflows on top of LLMs. However, the official Java SDK is harder to use because its underlying implementation is more focused on the protocol's core layer. Creating your MCP server requires writing more repetitive low-level code unless you use the Spring AI Framework. But sometimes, we may simply need a lightweight development solution, that's why this project was born.

## Key Advantages

- **🚫 No Spring Framework Required** - Pure Java, lightweight and fast
- **⚡ Instant MCP Server** - Start server with just 1 line of code
- **🎉 Zero Boilerplate** - No need to write low-level MCP SDK code
- **👏 No JSON Schema** - No need to care about complex JSON definitions
- **🎯 Focus on Logic** - Concentrate on core business logic
- **🔌 Spring AI Compatible** - Configuration files compatible with Spring AI Framework
- **🌍 Multilingual Support** - Built-in internationalization support for MCP components
- **📦 Type-Safe** - Leverage Java's type system for compile-time safety checks

## Comparison with Official MCP Java SDK

| Feature              | Official MCP SDK | This SDK        |
|----------------------|------------------|-----------------|
| Code Required        | ~50-100 lines    | ~5-10 lines     |
| JSON Schema          | Hand-coded JSON  | No need to care |
| Type Safety          | Limited          | Full support    |
| Learning Curve       | Steep            | Gentle          |
| Multilingual Support | Unsupported      | Supported       |

## Use Cases

This SDK is especially suitable for the following scenarios:

1. **Rapid Prototyping** - Quickly validate MCP concepts and functionality
2. **Lightweight Applications** - Simple MCP services without Spring Framework
3. **Teaching Demonstrations** - Easy to understand and learn MCP protocol
4. **Microservice Components** - MCP functionality modules within systems
5. **Standalone Tools** - Running as independent MCP servers

## Supported Server Modes

| Mode                | Purpose                             | Use Cases                            |
|---------------------|-------------------------------------|--------------------------------------|
| **STDIO**           | Standard input/output communication | CLI tools, terminal applications     |
| **SSE**             | HTTP-based server-sent events       | Real-time communication (deprecated) |
| **Streamable HTTP** | HTTP streaming                      | Web applications                     |

## 📖 Getting Started

Want to get started quickly? Check out the [Getting Started Guide](./getting-started.md) to learn how to build your first MCP server in 5 minutes.
