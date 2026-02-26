package mortengf.ai.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A simple tool that can add, subtract, multiply and divide.
 * In a real agent this would typically call an external API or service.
 */
public class CalculatorTool {

    public static final String DEFINITION = """
        {
            "name": "calculate",
            "description": "Perform a simple mathematical calculation. Use this tool whenever you need to compute a numeric result.",
            "input_schema": {
                "type": "object",
                "properties": {
                    "operation": {
                        "type": "string",
                        "enum": ["add", "subtract", "multiply", "divide"],
                        "description": "The mathematical operation to perform"
                    },
                    "a": {
                        "type": "number",
                        "description": "First number"
                    },
                    "b": {
                        "type": "number",
                        "description": "Second number"
                    }
                },
                "required": ["operation", "a", "b"]
            }
        }
        """;

    /**
     * Executes the calculation based on the tool call input from Claude.
     */
    public static String execute(JsonNode input) {
        String operation = input.get("operation").asText();
        double a = input.get("a").asDouble();
        double b = input.get("b").asDouble();

        double result = switch (operation) {
            case "add"      -> a + b;
            case "subtract" -> a - b;
            case "multiply" -> a * b;
            case "divide"   -> b != 0 ? a / b : Double.NaN;
            default         -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };

        if (Double.isNaN(result)) {
            return "Error: Division by zero is not allowed";
        }

        return String.valueOf(result);
    }
}
