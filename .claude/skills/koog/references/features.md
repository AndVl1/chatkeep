# Koog Agent Features Reference

## Table of Contents
- [Feature Installation](#feature-installation)
- [EventHandler](#eventhandler)
- [Memory](#memory)
- [Tracing](#tracing)
- [Persistence (Snapshots)](#persistence-snapshots)
- [Built-in Tools](#built-in-tools)

---

## Feature Installation

Features are installed in the AIAgent constructor's trailing lambda:

```kotlin
val agent = AIAgent(
    promptExecutor = executor,
    llmModel = model,
    strategy = chatAgentStrategy(),
    toolRegistry = toolRegistry,
    systemPrompt = "..."
) {
    // Install features here
    handleEvents { /* ... */ }
    install(AgentMemory) { /* ... */ }
    install(Tracing) { /* ... */ }
    install(Persistence) { /* ... */ }
}
```

---

## EventHandler

Package: `ai.koog.agents.features.eventHandler.feature`

Callback-based API for reacting to agent lifecycle events.

```kotlin
val agent = AIAgent(...) {
    handleEvents {
        // Agent lifecycle
        onAgentStarting { ctx -> println("Agent ${ctx.agent.id} starting") }
        onAgentCompleted { ctx -> println("Result: ${ctx.result}") }
        onAgentExecutionFailed { ctx -> println("Error: ${ctx.error}") }
        onAgentClosing { ctx -> /* cleanup */ }

        // Strategy lifecycle
        onStrategyStarting { ctx -> /* ... */ }
        onStrategyCompleted { ctx -> /* ... */ }

        // Node execution
        onNodeExecutionStarting { ctx -> /* ... */ }
        onNodeExecutionCompleted { ctx -> /* ... */ }
        onNodeExecutionFailed { ctx -> /* ... */ }

        // LLM calls
        onLLMCallStarting { ctx -> /* ... */ }
        onLLMCallCompleted { ctx -> /* ... */ }

        // Streaming
        onLLMStreamingStarting { ctx -> /* ... */ }
        onLLMStreamingFrameReceived { ctx -> /* ... */ }
        onLLMStreamingCompleted { ctx -> /* ... */ }
        onLLMStreamingFailed { ctx -> /* ... */ }

        // Tool execution
        onToolCallStarting { ctx -> /* ... */ }
        onToolCallCompleted { ctx -> /* ... */ }
        onToolCallFailed { ctx -> /* ... */ }
        onToolValidationFailed { ctx -> /* ... */ }
    }
}
```

---

## Memory

Package: `ai.koog.agents.memory.feature`

Enables agents to store, retrieve, and use information across conversations.

### Installation

```kotlin
import ai.koog.agents.memory.feature.AgentMemory

val agent = AIAgent(...) {
    install(AgentMemory) {
        memoryProvider = myMemoryProvider // LocalFileMemoryProvider or NoMemory
        featureName = "my-feature"
        organizationName = "my-org"
    }
}
```

### Concepts

- **Concept** — category of information (keyword + description + fact type)
- **Fact** — individual piece of information associated with a concept
- **MemoryScope** — context for facts: `Global`, `Agent`, `Subgraph`, `User`
- **MemorySubject** — what the memory is about

### Strategy DSL Nodes

```kotlin
import ai.koog.agents.memory.feature.nodes.*

val strategy = strategy<String, String>("with-memory") {
    // Save facts from conversation history
    val save by nodeSaveToMemory("save", scopes = listOf(MemoryScope.Agent))

    // Auto-detect and save facts
    val autoSave by nodeSaveToMemoryAutoDetectFacts("autoSave", scopes = listOf(...))

    // Load facts into agent prompt
    val load by nodeLoadFromMemory("load", scopeTypes = listOf(MemoryScopeType.Agent))

    // Load all facts
    val loadAll by nodeLoadAllFactsFromMemory("loadAll", scopeTypes = listOf(...))
}
```

### Providers

- `LocalFileMemoryProvider` — file-based storage
- `NoMemory` — no persistence (in-memory only)

---

## Tracing

Package: `ai.koog.agents.features.tracing.feature`

Captures trace events during agent execution for debugging and monitoring.

```kotlin
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.agents.features.tracing.writer.TraceFeatureMessageLogWriter
import ai.koog.agents.features.tracing.writer.TraceFeatureMessageFileWriter
import kotlinx.io.files.Path

val agent = AIAgent(...) {
    install(Tracing) {
        // Log traces to logger
        addMessageProcessor(TraceFeatureMessageLogWriter(logger))

        // Write traces to file
        addMessageProcessor(TraceFeatureMessageFileWriter(
            Path("/path/to/trace.log"),
            { path -> SystemFileSystem.sink(path).buffered() }
        ))
    }
}
```

Writers:
- `TraceFeatureMessageLogWriter` — write to kotlin-logging logger
- `TraceFeatureMessageFileWriter` — write to file
- `TraceFeatureMessageRemoteWriter` — send to remote endpoint

---

## Persistence (Snapshots)

Package: `ai.koog.agents.snapshot.feature`

Save and restore agent state checkpoints for fault tolerance.

```kotlin
import ai.koog.agents.snapshot.feature.Persistence
import ai.koog.agents.snapshot.feature.RollbackStrategy

val agent = AIAgent(...) {
    install(Persistence) {
        // Storage backend
        storage = InMemoryPersistenceStorageProvider()
        // or: FilePersistenceStorageProvider(path)

        // Auto-checkpoint after each node
        enableAutomaticPersistence = true

        // Rollback strategy
        rollbackStrategy = RollbackStrategy.Default          // full state machine
        // or: RollbackStrategy.MessageHistoryOnly           // messages only

        // Optional: rollback side-effects when restoring checkpoint
        rollbackToolRegistry = RollbackToolRegistry {
            registerRollback(::someTool, ::howToRollbackThisTool)
        }
    }
}
```

Providers:
- `InMemoryPersistenceStorageProvider` — in-memory (lost on restart)
- `FilePersistenceStorageProvider` — file-based checkpoints
- `NoPersistencyStorageProvider` — disabled

---

## Built-in Tools

Package: `ai.koog.agents.ext.tool`

### User Interaction

```kotlin
import ai.koog.agents.ext.tool.AskUser
import ai.koog.agents.ext.tool.SayToUser

val toolRegistry = ToolRegistry {
    tool(AskUser)    // LLM asks user a question, gets text response
    tool(SayToUser)  // LLM displays a message to user
    tools(MyTools())
}
```

- `AskUser` — takes `question: String`, returns user's text response
- `SayToUser` — takes `message: String`, displays to user

### File Tools

Package: `ai.koog.agents.ext.tool.file`

```kotlin
import ai.koog.agents.ext.tool.file.*

// Available tools:
ReadFileTool    // Read file contents (supports startLine/endLine)
WriteFileTool   // Write file (supports createDirectories)
EditFileTool    // Edit file with original/updated replacement
ListDirectoryTool // List directory (recursive, maxDepth, glob patterns)
```

### Shell Tools

Package: `ai.koog.agents.ext.tool.shell`

```kotlin
import ai.koog.agents.ext.tool.shell.*

// Execute shell commands
val shellTool = ExecuteShellCommandTool(
    confirmationHandler = BraveModeConfirmationHandler  // auto-approve
    // or: PrintShellCommandConfirmationHandler          // ask user
)

// Args: command, workingDirectory?, timeout?
// Result: exitCode, stdout, stderr
```

### Registering Built-in Tools

```kotlin
val toolRegistry = ToolRegistry {
    tool(AskUser)
    tool(SayToUser)
    tool(ReadFileTool())
    tool(WriteFileTool())
    tool(EditFileTool())
    tool(ListDirectoryTool())
    tool(ExecuteShellCommandTool(BraveModeConfirmationHandler))
    tools(MyCustomToolSet())
}
```

---

## SimpleTool (Class-Based Alternative to @Tool)

For tools that need custom serialization or complex args:

```kotlin
import ai.koog.agents.core.tools.SimpleTool
import kotlinx.serialization.Serializable

class WebSearchTool : SimpleTool<WebSearchTool.Args>(
    argsSerializer = Args.serializer(),
    name = "web_search",
    description = "Search the web"
) {
    @Serializable
    class Args(val query: String)

    override suspend fun execute(args: Args): String {
        return httpClient.get("https://api.search.com?q=${args.query}").bodyAsText()
    }
}

// Register: tool(WebSearchTool())
```

### Converting ToolSet to Tool List

```kotlin
// Use .asTools() to get List<Tool<*, *>> from annotation-based ToolSet
val tools = MyToolSet().asTools()

// Useful in subgraph definitions:
val sg by subgraphWithTask<String, String>(
    tools = MyToolSet().asTools() + AskUser,
    ...
) { /* ... */ }
```
