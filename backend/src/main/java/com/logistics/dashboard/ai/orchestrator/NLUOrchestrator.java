package com.logistics.dashboard.ai.orchestrator;

import com.logistics.dashboard.ai.model.QueryRequest;
import com.logistics.dashboard.ai.model.QueryResponse;
import com.logistics.dashboard.ai.tools.AiTools;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class NLUOrchestrator {

    private final ChatLanguageModel model;
    private final AiAssistant assistant;
    private final AiTools aiTools;

    public NLUOrchestrator(
            @Value("${openai.api.key:}") String apiKey,
            AiTools aiTools) {

        this.aiTools = aiTools;

        // Check if API key is provided
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.out.println("WARNING: OPENAI_API_KEY not provided. Natural Language Query functionality will be limited.");
            // Create a mock model that returns placeholder responses
            this.model = null;
            this.assistant = null;
        } else {
            // Create chat model with OpenAI
            this.model = dev.langchain4j.model.openai.OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName("gpt-3.5-turbo")
                    .temperature(0.1)
                    .maxTokens(500)
                    .build();

            // Create chat memory
            ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

            // Create AI assistant with tool support
            this.assistant = AiServices.builder(AiAssistant.class)
                    .chatLanguageModel(model)
                    .chatMemory(chatMemory)
                    .tools(aiTools)
                    .build();
        }
    }

    /**
     * Process a natural language query and return structured response
     */
    public QueryResponse processQuery(QueryRequest request) {
        try {
            // Check if AI model is available
            if (model == null || assistant == null) {
                QueryResponse placeholderResponse = new QueryResponse();
                placeholderResponse.setAnswer("Natural Language Query functionality requires an OpenAI API key to be configured. Please set the OPENAI_API_KEY environment variable.");
                placeholderResponse.setExplanation("This feature uses OpenAI's GPT model to interpret natural language questions about logistics data.");
                placeholderResponse.setChartType("none");

                // You can still use other dashboard features without OpenAI
                if (request.getQuestion() != null && !request.getQuestion().isEmpty()) {
                    String question = request.getQuestion().toLowerCase();
                    if (question.contains("order") || question.contains("delivery") || question.contains("carrier")) {
                        placeholderResponse.setAnswer("Natural language queries are currently unavailable. Please use the dashboard's standard analytics features for order, delivery, and carrier data analysis.");
                    }
                }

                return placeholderResponse;
            }

            // Prepare system message with context
            String systemPrompt = createSystemPrompt(request);

            // Get response from AI assistant - combine system prompt and user question
            String fullMessage = "System: " + systemPrompt + "\n\nUser: " + request.getQuestion();
            String aiResponse = assistant.chat(fullMessage);

            // For now, return a simple response
            // In a full implementation, we would parse the AI's tool calls and execute them
            QueryResponse response = new QueryResponse();
            response.setAnswer(aiResponse);
            response.setExplanation("The AI interpreted your question and selected appropriate analytical tools.");
            response.setChartType("none");

            // Add filters if provided
            if (request.getStartDate() != null || request.getEndDate() != null ||
                request.getCarriers() != null || request.getRegions() != null) {
                Map<String, Object> filters = new HashMap<>();
                if (request.getStartDate() != null) filters.put("startDate", request.getStartDate());
                if (request.getEndDate() != null) filters.put("endDate", request.getEndDate());
                if (request.getCarriers() != null) filters.put("carriers", request.getCarriers());
                if (request.getRegions() != null) filters.put("regions", request.getRegions());
                response.setFilters(filters);
            }

            return response;

        } catch (Exception e) {
            QueryResponse errorResponse = new QueryResponse();
            errorResponse.setError("Failed to process query: " + e.getMessage());
            errorResponse.setAnswer("I encountered an error processing your question. Please try again.");
            return errorResponse;
        }
    }

    /**
     * Create system prompt with context about available tools and data
     */
    private String createSystemPrompt(QueryRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI assistant for a logistics analytics dashboard. ");
        prompt.append("Your role is to interpret user questions about logistics data and select appropriate analytical tools. ");
        prompt.append("\n\n");
        prompt.append("Available data includes: orders, delivery performance, carrier performance, and time series trends. ");
        prompt.append("All data is read-only from a PostgreSQL database. ");
        prompt.append("\n\n");
        prompt.append("You have access to these tools:\n");
        prompt.append("1. getTimeSeries - Get aggregated values over time (day, week, month)\n");
        prompt.append("2. getBreakdown - Get aggregated values by categorical dimension (carrier, region)\n");
        prompt.append("3. getKpi - Get key performance indicators (total orders, delivered, delayed, on-time rate, avg delivery days)\n");
        prompt.append("4. getDeliveryPerformance - Get delivery performance data (on-time vs delayed)\n");
        prompt.append("5. forecastDemand - Forecast future order volume (placeholder for now)\n");
        prompt.append("\n");
        prompt.append("IMPORTANT RULES:\n");
        prompt.append("- NEVER make up or guess data values. Always use the tools.\n");
        prompt.append("- If a user asks about specific dates, use the date range provided.\n");
        prompt.append("- If no date range is specified, suggest using a reasonable default (e.g., last 30 days).\n");
        prompt.append("- Always explain what tools you would use and why.\n");
        prompt.append("\n");

        // Add context about filters if provided
        if (request.getStartDate() != null || request.getEndDate() != null) {
            prompt.append("Context: User has specified date range: ");
            if (request.getStartDate() != null) prompt.append("from ").append(request.getStartDate());
            if (request.getEndDate() != null) prompt.append(" to ").append(request.getEndDate());
            prompt.append("\n");
        }

        if (request.getCarriers() != null && !request.getCarriers().isEmpty()) {
            prompt.append("Context: User filtered by carriers: ").append(String.join(", ", request.getCarriers())).append("\n");
        }

        if (request.getRegions() != null && !request.getRegions().isEmpty()) {
            prompt.append("Context: User filtered by regions: ").append(String.join(", ", request.getRegions())).append("\n");
        }

        prompt.append("\n");
        prompt.append("Your response should be a clear, concise answer to the user's question, ");
        prompt.append("mentioning which tools would be used and what the results would show.");

        return prompt.toString();
    }

    /**
     * AI Assistant interface for LangChain4j
     */
    interface AiAssistant {

        String chat(@UserMessage String message);
    }
}