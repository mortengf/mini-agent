package mortengf.ai.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the agent loop:
 *
 *   1. Send the full conversation history to Claude
 *   2. If Claude requests a tool call → execute it via MCP → append result to history
 *   3. Repeat until Claude returns a final text response
 *
 * Claude has no memory between API calls. The growing `messages` list is
 * therefore sent in full on every iteration, so Claude always has the
 * complete context of what has happened since the start.
 */
public class AgentLoop {

    private static final int MAX_ITERATIONS = 10; // safety limit

    private final ClaudeClient client;
    private final McpClientService mcpClientService;
    private final ObjectMapper mapper;
    private final String tools;

    public AgentLoop(ClaudeClient client, McpClientService mcpClientService) throws Exception {
        this.client = client;
        this.mcpClientService = mcpClientService;
        this.mapper = client.getMapper();
        this.tools = mcpClientService.getToolsJson();
    }

    /**
     * Runs the agent loop for a given user prompt.
     * Returns Claude's final text response.
     */
    public String run(String userPrompt) throws Exception {
        // messages is the growing history of the conversation.
        // Everything that happens — user input, Claude responses, tool results —
        // is appended here and sent in full to Claude on every iteration.
        List<ObjectNode> messages = new ArrayList<>();

        // Start the conversation with the user's prompt
        messages.add(buildUserMessage(userPrompt));

        System.out.println("─".repeat(60));
        System.out.println("User: " + userPrompt);
        System.out.println("─".repeat(60));

        for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++) {
            System.out.println("\n[Iteration " + iteration + "] Sending to Claude...");

            JsonNode response = client.sendMessages(messages, tools);

            // stopReason tells us what Claude needs next:
            //   "tool_use" → Claude is saying: "I need you to run a tool for me before I can continue"
            //   "end_turn" → Claude is saying: "I have everything I need — here is your answer"
            String stopReason = response.get("stop_reason").asText();

            System.out.println("[Stop reason: " + stopReason + "]");

            if ("end_turn".equals(stopReason)) {
                // Claude is done — extract and return the text response
                return extractText(response);
            }

            if ("tool_use".equals(stopReason)) {
                // Claude cannot execute code itself — it can only ask us to do it.
                // Our job here is to: find the tool call, execute it, and report back.

                // 1. Append Claude's response (including the tool call request) to history
                messages.add(buildAssistantMessage(response));

                // 2. Find and execute all tool calls in the response
                ArrayNode toolResults = mapper.createArrayNode();

                for (JsonNode block : response.get("content")) {
                    if ("tool_use".equals(block.get("type").asText())) {
                        String toolName  = block.get("name").asText();
                        String toolUseId = block.get("id").asText();
                        JsonNode toolInput = block.get("input");

                        System.out.println("[Tool call] " + toolName +
                                " with input: " + toolInput);

                        String result = mcpClientService.callTool(toolName, toolInput);

                        System.out.println("[Tool result] " + result);

                        // Build the tool_result block Claude expects in return
                        ObjectNode toolResult = mapper.createObjectNode();
                        toolResult.put("type", "tool_result");
                        toolResult.put("tool_use_id", toolUseId); // links result back to the request
                        toolResult.put("content", result);
                        toolResults.add(toolResult);
                    }
                }

                // 3. Append tool results to history as a user message and loop again
                ObjectNode toolResultMessage = mapper.createObjectNode();
                toolResultMessage.put("role", "user");
                toolResultMessage.set("content", toolResults);
                messages.add(toolResultMessage);

            } else {
                throw new RuntimeException("Unexpected stop_reason: " + stopReason);
            }
        }

        throw new RuntimeException("Maximum iterations reached (" + MAX_ITERATIONS + ")");
    }

    private String extractText(JsonNode response) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode block : response.get("content")) {
            if ("text".equals(block.get("type").asText())) {
                sb.append(block.get("text").asText());
            }
        }
        return sb.toString();
    }

    private ObjectNode buildUserMessage(String text) {
        ObjectNode message = mapper.createObjectNode();
        message.put("role", "user");
        message.put("content", text);
        return message;
    }

    private ObjectNode buildAssistantMessage(JsonNode response) {
        ObjectNode message = mapper.createObjectNode();
        message.put("role", "assistant");
        message.set("content", response.get("content"));
        return message;
    }
}
