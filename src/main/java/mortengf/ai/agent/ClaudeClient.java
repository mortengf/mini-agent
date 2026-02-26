package mortengf.ai.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * Handles HTTP communication with the Anthropic API.
 */
public class ClaudeClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL   = "claude-opus-4-6";

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String apiKey;

    public ClaudeClient(String apiKey) {
        this.apiKey     = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.mapper     = new ObjectMapper();
    }

    /**
     * Sends the full conversation history to Claude and returns the response.
     *
     * @param messages  The growing list of messages in the conversation so far
     * @param toolsDef  JSON string with tool definitions available to Claude
     * @return          Claude's raw API response as a JsonNode
     */
    public JsonNode sendMessages(List<ObjectNode> messages, String toolsDef) throws Exception {
        // Build request body
        ObjectNode body = mapper.createObjectNode();
        body.put("model", MODEL);
        body.put("max_tokens", 1024);

        // Add conversation history
        ArrayNode messagesArray = mapper.createArrayNode();
        messages.forEach(messagesArray::add);
        body.set("messages", messagesArray);

        // Add tool definitions
        body.set("tools", mapper.readTree(toolsDef));

        String requestBody = mapper.writeValueAsString(body);

        // Build HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // Send and receive
        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API error %d: %s"
                    .formatted(response.statusCode(), response.body()));
        }

        return mapper.readTree(response.body());
    }

    public ObjectMapper getMapper() {
        return mapper;
    }
}
