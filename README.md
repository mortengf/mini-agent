# agent-phase1

Phase 1 of an agentic AI study plan covering: Claude Code, Anthropic Skills, MCP, Codex CLI, Antigravity, and Agentic AI.

## What this is

A minimal Java implementation of an **agent loop** using the Anthropic Claude API with tool use. It demonstrates the core pattern of agentic AI: a loop where Claude reasons about a task, requests tool executions, receives results, and decides when it has enough information to produce a final answer.

Claude has no memory between API calls. The `messages` list grows with every iteration and is sent in full each time — this is how Claude maintains context throughout the conversation.

## Project structure

```
<<<<<<< HEAD
src/main/java/dk/studie/agent/
=======
src/main/java/mortengf/ai/agent/
>>>>>>> e2d4c02 (Initial commit)
├── Main.java                  # Entry point — defines tasks and runs the agent
├── ClaudeClient.java          # HTTP communication with the Anthropic API
├── AgentLoop.java             # The agent loop (send → tool call → result → repeat)
└── tools/
    └── CalculatorTool.java    # A simple calculator tool (add, subtract, multiply, divide)
```

## How the agent loop works

```
[Your code sends full message history]
              ↓
[Claude reasons: do I have enough information?]
              ↓ no
[Claude returns stop_reason: "tool_use"]
              ↓
[Your code executes the tool]
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

## Prerequisites

- Java 17+
- Maven 3.6+
- An [Anthropic API key](https://console.anthropic.com)

## Running

```bash
export ANTHROPIC_API_KEY=sk-ant-xxxxxxxxxx
mvn package -q
java -jar target/agent-phase1-1.0-SNAPSHOT.jar
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

**Orchestration** — Your Java code runs the loop, executes tools, and manages the message history. Claude reasons; your code acts.

**Generality** — `CalculatorTool` is pure Java with no AI dependency. `AgentLoop` is conceptually provider-agnostic — the loop structure is the same regardless of which LLM you use. Only `ClaudeClient` is Claude-specific.

## Next steps

Phase 2 of the study plan introduces MCP (Model Context Protocol) — a standard that decouples tool definitions from any specific AI provider, so tools like `CalculatorTool` can be defined once and used by any MCP-compatible AI.
