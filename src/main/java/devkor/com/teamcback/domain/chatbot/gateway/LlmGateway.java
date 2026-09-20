package devkor.com.teamcback.domain.chatbot.gateway;

public interface LlmGateway {
    String generate(String systemPrompt, String userMessage);
}
