package mortengf.ai.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;

/**
 * Connects to an MCP server via stdio, discovers its tools, and executes them.
 */
public class McpClientService implements AutoCloseable {

    private final McpSyncClient mcpClient;
    private final ObjectMapper mapper;

    public McpClientService(ObjectMapper mapper) {
        this.mapper = mapper;

        var params = ServerParameters.builder(
                        "/Users/mgf/.sdkman/candidates/java/17.0.1-tem/bin/java")
                .args("-jar", "/Users/mgf/Source/calculator-mcp-server/target/calculator-mcp-server-1.0-SNAPSHOT.jar")
                .build();

        var transport = new StdioClientTransport(params, new JacksonMcpJsonMapper(mapper));
        this.mcpClient = McpClient.sync(transport).build();
        this.mcpClient.initialize();
    }

    /**
     * Returns a JSON array string of tool definitions in Anthropic format,
     * discovered dynamically from the MCP server.
     */
    public String getToolsJson() throws Exception {
        var toolsResult = mcpClient.listTools();
        var toolsArray = mapper.createArrayNode();

        for (var tool : toolsResult.tools()) {
            var toolNode = mapper.createObjectNode();
            toolNode.put("name", tool.name());
            toolNode.put("description", tool.description() != null ? tool.description() : "");
            toolNode.set("input_schema", mapper.valueToTree(tool.inputSchema()));
            toolsArray.add(toolNode);
        }

        return mapper.writeValueAsString(toolsArray);
    }

    /**
     * Calls the named tool with the given JSON input and returns the text result.
     */
    public String callTool(String name, JsonNode input) throws Exception {
        Map<String, Object> arguments = mapper.convertValue(
                input, new TypeReference<>() {});

        var result = mcpClient.callTool(new McpSchema.CallToolRequest(name, arguments));

        var sb = new StringBuilder();
        for (var content : result.content()) {
            if (content instanceof McpSchema.TextContent textContent) {
                sb.append(textContent.text());
            }
        }
        return sb.toString();
    }

    @Override
    public void close() {
        mcpClient.close();
    }
}
