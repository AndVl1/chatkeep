---
name: koog
description: JetBrains Koog AI Agent framework (Kotlin) - use for building AI agents with tool calling, LLM integration via OpenRouter/OpenAI/Anthropic/Google/DeepSeek, and AI-powered workflows. Use when implementing AI agents, LLM calls, tool-calling patterns, or integrating LLM providers in Kotlin projects.
---

# Koog AI Agent Framework

Kotlin Multiplatform framework for AI agents. Published on Maven Central under `ai.koog` group.

**Current version: `0.5.2`**

## Dependencies

`koog-agents` is the umbrella module — it transitively includes all sub-modules (agents-core, agents-ext, all provider clients, tools, prompt DSL, etc.).

```kotlin
// build.gradle.kts — minimal setup (JVM project)
repositories { mavenCentral() }

val koogVersion = "0.5.2"

dependencies {
    implementation("ai.koog:koog-agents:$koogVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
}
```

No need to add individual sub-modules like `prompt-executor-openrouter-client` — they come via `koog-agents`.

For Spring Boot, also add: `implementation("ai.koog:koog-ktor:$koogVersion")`

## Import Paths (verified from 0.5.2 JARs)

```
// Agent
ai.koog.agents.core.agent.AIAgent
ai.koog.agents.core.agent.config.AIAgentConfig

// Tools
ai.koog.agents.core.tools.ToolRegistry
ai.koog.agents.core.tools.annotations.Tool
ai.koog.agents.core.tools.annotations.LLMDescription
ai.koog.agents.core.tools.reflect.ToolSet       // interface for annotation-based tools
ai.koog.agents.core.tools.reflect.tools          // extension for ToolRegistry DSL

// Strategies (predefined)
ai.koog.agents.ext.agent.chatAgentStrategy       // chat agent with tool loop
ai.koog.agents.ext.agent.reActStrategy           // ReAct pattern
ai.koog.agents.core.agent.singleRunStrategy      // single LLM call + tools

// Strategy DSL (custom strategies)
ai.koog.agents.core.dsl.builder.strategy
ai.koog.agents.core.dsl.builder.forwardTo
ai.koog.agents.core.dsl.extension.nodeLLMRequest
ai.koog.agents.core.dsl.extension.nodeExecuteTool
ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
ai.koog.agents.core.dsl.extension.onAssistantMessage
ai.koog.agents.core.dsl.extension.onToolCall

// Prompt
ai.koog.prompt.dsl.Prompt
ai.koog.prompt.dsl.prompt

// Executor
ai.koog.prompt.executor.llms.SingleLLMPromptExecutor

// Providers — see references/providers.md for full list
ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
ai.koog.prompt.executor.clients.openrouter.OpenRouterParams
ai.koog.prompt.executor.clients.openai.OpenAILLMClient
ai.koog.prompt.executor.clients.openai.OpenAIModels
ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
```

## AIAgent Constructor

The simplest `String→String` overload:

```kotlin
AIAgent(
    promptExecutor: PromptExecutor,
    llmModel: LLModel,
    strategy: AIAgentGraphStrategy<String, String> = singleRunStrategy(),
    toolRegistry: ToolRegistry = ToolRegistry.EMPTY,
    id: String? = null,
    systemPrompt: String = "",
    temperature: Double = 0.0,
    numberOfChoices: Int = 1,
    maxIterations: Int = 50,
    installFeatures: GraphAIAgent.FeatureContext.() -> Unit = {}
): AIAgent<String, String>
```

AIAgentConfig-based overload:

```kotlin
AIAgent(
    promptExecutor: PromptExecutor,
    agentConfig: AIAgentConfig,
    strategy: AIAgentGraphStrategy<String, String> = singleRunStrategy(),
    toolRegistry: ToolRegistry = ToolRegistry.EMPTY,
    id: String? = null,
    installFeatures: GraphAIAgent.FeatureContext.() -> Unit = {}
): GraphAIAgent<String, String>
```

## Annotation-Based Tools

```kotlin
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet

@LLMDescription("Tools for file operations")
class FileTools : ToolSet {

    @Tool
    @LLMDescription("Read file contents")
    fun readFile(
        @LLMDescription("Path to file") path: String
    ): String {
        return java.io.File(path).readText()
    }

    @Tool
    @LLMDescription("List files in directory")
    fun listFiles(
        @LLMDescription("Directory path") dir: String
    ): String {
        return java.io.File(dir).listFiles()?.joinToString("\n") { it.name } ?: "empty"
    }
}
```

Register in ToolRegistry:

```kotlin
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools

val toolRegistry = ToolRegistry {
    tools(FileTools())         // register all @Tool methods from ToolSet
    tools(AnotherToolSet())    // can register multiple
}
```

## Predefined Strategies

| Strategy | Import | Use case |
|----------|--------|----------|
| `chatAgentStrategy()` | `ai.koog.agents.ext.agent` | Chat with tool calling loop (most common) |
| `reActStrategy(reasoningInterval, name)` | `ai.koog.agents.ext.agent` | ReAct: reason→act→observe loop |
| `singleRunStrategy(toolCalls)` | `ai.koog.agents.core.agent` | Single LLM request + optional tool execution |

## Complete Example: Agent with OpenRouter

```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.ext.agent.chatAgentStrategy
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import kotlinx.coroutines.runBlocking

@LLMDescription("Math tools")
class MathTools : ToolSet {
    @Tool
    @LLMDescription("Add two numbers")
    fun add(@LLMDescription("First number") a: Int, @LLMDescription("Second number") b: Int): String {
        return "Result: ${a + b}"
    }
}

fun main() = runBlocking {
    val client = OpenRouterLLMClient(apiKey = System.getenv("OPENROUTER_API_KEY"))
    val executor = SingleLLMPromptExecutor(client)

    val agent = AIAgent(
        promptExecutor = executor,
        llmModel = OpenRouterModels.DeepSeekV30324,
        strategy = chatAgentStrategy(),
        toolRegistry = ToolRegistry { tools(MathTools()) },
        systemPrompt = "You are a helpful assistant. Use tools when needed.",
        temperature = 0.7,
        maxIterations = 10
    )

    val result = agent.run("What is 42 + 58?")
    println(result)
}
```

## Prompt DSL (without agent)

```kotlin
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor

val prompt = prompt("my-prompt") {
    system("You are a helpful assistant")
    user("Explain coroutines")
}

// Direct execution without agent
val response = executor.execute(prompt, model)
```

## Provider Quick Reference

For detailed provider configuration, see [references/providers.md](references/providers.md).

| Provider | Client class | Models object | Key env var |
|----------|-------------|---------------|-------------|
| OpenRouter | `OpenRouterLLMClient` | `OpenRouterModels` | `OPENROUTER_API_KEY` |
| OpenAI | `OpenAILLMClient` | `OpenAIModels.Chat` | `OPENAI_API_KEY` |
| Anthropic | `AnthropicLLMClient` | `AnthropicModels` | `ANTHROPIC_API_KEY` |
| Google | `GoogleLLMClient` | `GoogleModels` | `GOOGLE_API_KEY` |
| DeepSeek | `DeepSeekLLMClient` | `DeepSeekModels` | `DEEPSEEK_API_KEY` |
| Ollama | `OllamaLLMClient` | — | — |

## Custom Strategy DSL

For when predefined strategies aren't enough. Full reference: [references/strategies.md](references/strategies.md).

```kotlin
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*

val myStrategy = strategy<String, String>("my-agent") {
    val nodeLLM by nodeLLMRequest()
    val nodeExec by nodeExecuteTool()
    val nodeSend by nodeLLMSendToolResult()

    edge(nodeStart forwardTo nodeLLM)
    edge(nodeLLM forwardTo nodeFinish onAssistantMessage { true })
    edge(nodeLLM forwardTo nodeExec onToolCall { true })
    edge(nodeExec forwardTo nodeSend)
    edge(nodeSend forwardTo nodeFinish onAssistantMessage { true })
    edge(nodeSend forwardTo nodeExec onToolCall { true })
}
```

Key concepts (details in strategies.md):
- **Nodes**: `nodeLLMRequest`, `nodeExecuteTool`, `nodeLLMSendToolResult`, custom `node<In, Out>`
- **Edges**: `forwardTo` + conditions (`onAssistantMessage`, `onToolCall`, `onCondition`) + `transformed`
- **Subgraphs**: isolated sections with own tools/model — `subgraph`, `subgraphWithTask`, `subgraphWithVerification`
- **Parallel**: `parallel(nodeA, nodeB, nodeC) { selectByMax { it } }`
- **Sequential**: `nodeStart then subgraphA then subgraphB then nodeFinish`
- **Structured output**: `nodeLLMRequestStructured<MyDataClass>(examples = [...])`

## Agent Features & Built-in Tools

Full reference: [references/features.md](references/features.md).

- **EventHandler** — lifecycle callbacks (`onAgentStarting`, `onToolCallCompleted`, `onLLMCallCompleted`, etc.)
- **Memory** — store/retrieve facts across conversations (`AgentMemory` feature, scopes, concepts)
- **Tracing** — trace events to log/file/remote (`Tracing` feature)
- **Persistence** — checkpoint/restore agent state (`Persistence` feature, rollback strategies)
- **Built-in tools**: `AskUser`, `SayToUser`, `ReadFileTool`, `WriteFileTool`, `EditFileTool`, `ListDirectoryTool`, `ExecuteShellCommandTool`

```kotlin
// Feature installation example
val agent = AIAgent(...) {
    handleEvents {
        onAgentStarting { ctx -> println("Starting: ${ctx.agent.id}") }
        onToolCallCompleted { ctx -> println("Tool done") }
    }
    install(Tracing) { addMessageProcessor(TraceFeatureMessageLogWriter(logger)) }
}

// Built-in tools
val registry = ToolRegistry {
    tool(AskUser)       // ai.koog.agents.ext.tool.AskUser
    tool(SayToUser)     // ai.koog.agents.ext.tool.SayToUser
    tools(MyToolSet())
}
```
