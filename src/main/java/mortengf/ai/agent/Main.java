package mortengf.ai.agent;

public class Main {

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Please set the ANTHROPIC_API_KEY environment variable");
            System.exit(1);
        }

        ClaudeClient client = new ClaudeClient(apiKey);
        AgentLoop agent = new AgentLoop(client);

        // Task 1: single-step calculation
        String answer1 = agent.run(
                "What is (123 * 456) + (789 / 3)? Use the calculate tool to work it out."
        );
        System.out.println("\nClaude's answer: " + answer1);

        System.out.println("\n" + "═".repeat(60) + "\n");

        // Task 2: Claude must decide to use the tool multiple times
        String answer2 = agent.run(
                "I have 3 boxes with 144 apples each. I eat 27 apples. " +
                        "How many apples do I have left? Use the calculate tool."
        );
        System.out.println("\nClaude's answer: " + answer2);
    }
}
