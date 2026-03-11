# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build (creates fat JAR)
mvn package -q

# Run
export ANTHROPIC_API_KEY=sk-ant-...
java -jar target/mini-agent-1.0-SNAPSHOT.jar
```

No test or lint commands are configured.

## Architecture

**mini-agent** is a Java demonstration of the AI agent loop pattern using the Anthropic Claude API.

### Core loop (AgentLoop.java)

```
Send full message history → Claude reasons
  if stop_reason == "tool_use"  → execute tool → append result → repeat
  if stop_reason == "end_turn"  → return text response
```

Max 10 iterations safety limit. Claude has no memory between API calls, so the full message history is sent on every request.

### Components

- **Main.java** — Entry point. Defines example tasks, wires up client and loop.
- **ClaudeClient.java** — HTTP layer. Sends requests to `https://api.anthropic.com/v1/messages` using Java's built-in `HttpClient` and Jackson for JSON. Sends full message history + tool definitions on every call.
- **AgentLoop.java** — Orchestrates the loop. Manages message history, parses `stop_reason`, dispatches tool calls, extracts final answer.
- **tools/CalculatorTool.java** — Example tool. Contains both the JSON schema (for Claude) and the Java implementation (for execution).

### Adding a new tool

1. Create a class in `src/main/java/mortengf/ai/agent/tools/` with a JSON tool definition and an `execute(String input, String operation)` (or similar) method.
2. Register the tool definition in `ClaudeClient.java` (sent in each API request).
3. Add dispatch logic in `AgentLoop.java` where tool calls are handled.

### Key design note

`AgentLoop` is provider-agnostic in structure — only `ClaudeClient` is Claude-specific. The pattern can be adapted for other LLM providers by swapping the client.
