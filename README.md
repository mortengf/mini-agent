# mini-agent
A Java implementation of a mini **agent loop** using the Anthropic Claude API with tool use. It demonstrates the core pattern of agentic AI: a loop where Claude reasons about a task, requests tool executions, receives results, and decides when it has enough information to produce a final answer.

Tools are discovered and executed dynamically via an external **MCP server** (Model Context Protocol), connected over stdio using the [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk).

Claude has no memory between API calls. The `messages` list grows with every iteration and is sent in full each time — this is how Claude maintains context throughout the conversation.

## Project structure

```
src/main/java/mortengf/ai/agent/
├── Main.java                      # Entry point — defines tasks and runs the agent
├── ClaudeClient.java              # HTTP communication with the Anthropic API
├── AgentLoop.java                 # The agent loop (send → tool call → result → repeat)
├── McpClientService.java          # MCP client — discovers and executes tools via stdio
└── tools/
    └── CalculatorToolOld.java     # Original hardcoded calculator — kept as a before/after reference
```

## How the agent loop works

```
[Your code sends full message history]
              ↓
[Claude reasons: do I have enough information?]
              ↓ no
[Claude returns stop_reason: "tool_use"]
              ↓
[Your code executes the tool via MCP server]
              ↓
[Tool result appended to message history]
              ↑_________________|

[Claude reasons: do I have enough information?]
              ↓ yes
[Claude returns stop_reason: "end_turn"]
              ↓
[Loop ends — final text response returned]
```

`stop_reason: "tool_use"` means Claude is saying: *"I need you to run a tool for me before I can continue."*
`stop_reason: "end_turn"` means Claude is saying: *"I have everything I need — here is your answer."*

Claude cannot execute code itself. Your Java code is the arm Claude does not have.

## How MCP fits in

`McpClientService` starts the MCP server as a subprocess and connects to it over stdio:

1. **`listTools()`** — called once at startup to discover available tools and build the tool definitions sent to Claude on every request.
2. **`callTool(name, arguments)`** — called each time Claude requests a tool, forwarding the call to the MCP server and returning the result.

This means tools are defined entirely in the MCP server. The agent code has no knowledge of what tools exist or how they work — it just relays calls.

## CalculatorToolOld

`CalculatorToolOld` is the original calculator implementation from before the MCP refactor. It is kept intentionally as a reference to illustrate the difference between the two approaches:

- **Before (CalculatorToolOld):** the tool's JSON schema and its execution logic live together in Java. Adding a tool means touching `AgentLoop`, `ClaudeClient`, and writing a new Java class.
- **After (McpClientService + MCP server):** the agent has no hardcoded knowledge of any tool. Tools are discovered at runtime via `listTools()` and executed via `callTool()`. Adding a tool only requires changes to the MCP server.

## Prerequisites

- Java 17+
- Maven 3.6+
- An [Anthropic API key](https://console.anthropic.com)
- The calculator MCP server JAR at:
  `/Users/mgf/Source/calculator-mcp-server/target/calculator-mcp-server-1.0-SNAPSHOT.jar`

## Running

```bash
export ANTHROPIC_API_KEY=sk-ant-xxxxxxxxxx
mvn package -q
java -jar target/mini-agent-1.0-SNAPSHOT.jar
```

## Example output

```
────────────────────────────────────────────────────────────
User: What is (123 * 456) + (789 / 3)? Use the calculate tool to work it out.
────────────────────────────────────────────────────────────

[Iteration 1] Sending to Claude...
[Stop reason: tool_use]
[Tool call] calculate with input: {"operation":"multiply","a":123,"b":456}
[Tool result] 56088.0
[Tool call] calculate with input: {"operation":"divide","a":789,"b":3}
[Tool result] 263.0

[Iteration 2] Sending to Claude...
[Stop reason: tool_use]
[Tool call] calculate with input: {"operation":"add","a":56088,"b":263}
[Tool result] 56351.0

[Iteration 3] Sending to Claude...
[Stop reason: end_turn]

Claude's answer: The result is 56,351.
```

## Key concepts illustrated

**Tool use** — Claude decides if and when to call a tool. You define what tools are available; Claude decides how to use them.

**Orchestration** — Your Java code runs the loop, executes tools via MCP, and manages the message history. Claude reasons; your code acts.

**MCP (Model Context Protocol)** — Tools are defined once in an MCP server and discovered at runtime. `AgentLoop` has no hardcoded knowledge of any tool — it relays whatever the MCP server exposes. Only `ClaudeClient` is Claude-specific; the rest of the loop structure is provider-agnostic.
