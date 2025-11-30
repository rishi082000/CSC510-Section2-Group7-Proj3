package FoodSeer.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import FoodSeer.dto.ChatRequestDto;
import FoodSeer.dto.ChatResponseDto;
import FoodSeer.entity.Food;
import FoodSeer.repositories.FoodRepository;
import FoodSeer.service.ChatService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/chat";
    private static final String MODEL = "qwen2.5:1.5b";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    private FoodRepository foodRepository; // fetch foods dynamically

    public ChatServiceImpl() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ChatResponseDto sendMessage(final ChatRequestDto chatRequest) {
        try {
            ArrayNode messages = objectMapper.createArrayNode();

            // System prompt
            ObjectNode systemMsg = objectMapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content",
                "You are FoodSeer AI. Respond naturally to greetings and normal conversation. " +
                "Only provide food recommendations when the user explicitly asks for it. " +
                "Do not add emojis, marketing text, or default food suggestions. Keep answers concise and factual."
            );
            messages.add(systemMsg);

            // User message
            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", chatRequest.getMessage());
            messages.add(userMsg);

            // Determine if user explicitly asked for a recommendation
            String msgLower = chatRequest.getMessage().toLowerCase();
            if (msgLower.contains("what should i eat") || 
                msgLower.contains("recommend food") || 
                msgLower.contains("suggest food")) {

                // Fetch menu from DB
                List<Food> foods = foodRepository.findAll();
                String menuString = foods.stream()
                        .map(f -> f.getFoodName() + " ($" + f.getPrice() + ")"
                                + (f.getAllergies() != null && !f.getAllergies().isEmpty()
                                    ? " Contains: " + String.join(", ", f.getAllergies())
                                    : " No common allergens"))
                        .collect(Collectors.joining("; "));

                ObjectNode menuMsg = objectMapper.createObjectNode();
                menuMsg.put("role", "system");
                menuMsg.put("content", "Available foods with prices and allergies: " + menuString);
                messages.add(menuMsg);
            }

            // Prepare request
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", MODEL);
            requestBody.set("messages", messages);
            requestBody.put("stream", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> httpEntity =
                    new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(OLLAMA_URL, httpEntity, String.class);

            if (response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());
                String aiResponse = json.get("message").get("content").asText();
                return new ChatResponseDto(aiResponse);
            }

            return new ChatResponseDto("No response from AI");

        } catch (Exception e) {
            return new ChatResponseDto("Error: " + e.getMessage());
        }
    }
}
