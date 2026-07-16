package io.github.vaquarkhan.loopengine.core.loop;

import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

/**
 * {@link LoopModelClient} backed by Spring AI {@link ChatClient}.
 * Decorates native tool-calling without replacing Spring AI APIs.
 */
public class ChatClientLoopModelClient implements LoopModelClient {

    private final ChatClient chatClient;
    private final boolean internalToolExecutionEnabled;

    public ChatClientLoopModelClient(ChatClient chatClient) {
        this(chatClient, false);
    }

    /**
     * @param internalToolExecutionEnabled when false (default), the loop engine executes tools
     *                                     itself so soft/hard budgets and fingerprinting apply.
     */
    public ChatClientLoopModelClient(ChatClient chatClient, boolean internalToolExecutionEnabled) {
        this.chatClient = Objects.requireNonNull(chatClient, "chatClient");
        this.internalToolExecutionEnabled = internalToolExecutionEnabled;
    }

    @Override
    public ModelRound generate(AgentTurn turn, List<LoopMessage> messages, boolean softWrapUp) {
        List<Message> springMessages = toSpringMessages(messages);
        var request = chatClient.prompt()
                .messages(springMessages)
                .options(ToolCallingChatOptions.builder()
                        .internalToolExecutionEnabled(internalToolExecutionEnabled && !softWrapUp)
                        .build());

        ChatResponse response = request.call().chatResponse();
        if (response == null || response.getResult() == null) {
            return ModelRound.finalAnswer("");
        }

        Generation generation = response.getResult();
        AssistantMessage assistantMessage = generation.getOutput();
        String text = assistantMessage != null && assistantMessage.getText() != null
                ? assistantMessage.getText()
                : "";

        Long inputTokens = null;
        Long outputTokens = null;
        String finishReason = null;
        if (response.getMetadata() != null) {
            Usage usage = response.getMetadata().getUsage();
            if (usage != null) {
                inputTokens = usage.getPromptTokens() == null ? null : usage.getPromptTokens().longValue();
                outputTokens = usage.getCompletionTokens() == null ? null : usage.getCompletionTokens().longValue();
            }
            if (generation.getMetadata() != null) {
                finishReason = generation.getMetadata().getFinishReason();
            }
        }

        List<ModelRound.ToolInvocation> tools = new ArrayList<>();
        if (assistantMessage != null && assistantMessage.hasToolCalls()) {
            assistantMessage.getToolCalls().forEach(tc ->
                    tools.add(new ModelRound.ToolInvocation(tc.id(), tc.name(), tc.arguments())));
        }

        return new ModelRound(text, tools, inputTokens, outputTokens, finishReason);
    }

    private static List<Message> toSpringMessages(List<LoopMessage> messages) {
        List<Message> result = new ArrayList<>(messages.size());
        for (LoopMessage message : messages) {
            switch (message.role().toLowerCase()) {
                case "system" -> result.add(new SystemMessage(message.content()));
                case "assistant" -> result.add(new AssistantMessage(message.content()));
                case "tool" -> result.add(ToolResponseMessage.builder()
                        .responses(List.of(new ToolResponseMessage.ToolResponse(
                                "tool", "tool", message.content())))
                        .build());
                default -> result.add(new UserMessage(message.content()));
            }
        }
        return result;
    }
}
