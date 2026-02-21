package com.smartexpense.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartexpense.model.Category;
import com.smartexpense.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * LLM-based categorization using Google Gemini API.
 * Called as fallback when keyword matching fails.
 * If a new category is discovered, it is persisted to the Category table.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiLLMService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    /**
     * Calls Gemini to categorize the expense description.
     * If Gemini returns a category not in the DB, it is saved automatically.
     *
     * @param description expense description
     * @return matched or newly created Category, or empty on error
     */
    public Optional<Category> categorize(String description) {
        try {
            List<String> existingCategories = categoryRepository.findAll()
                    .stream().map(Category::getName).toList();

            String prompt = buildPrompt(description, existingCategories);
            String geminiResponse = callGeminiApi(prompt);

            if (geminiResponse == null || geminiResponse.isBlank()) {
                return Optional.empty();
            }

            String categoryName = geminiResponse.trim();
            log.info("Gemini categorized '{}' → '{}'", description, categoryName);

            // Try to find existing category (case-insensitive)
            Optional<Category> existing = categoryRepository.findByNameIgnoreCase(categoryName);
            if (existing.isPresent()) {
                return existing;
            }

            // New category discovered by LLM — persist it
            Category newCategory = Category.builder()
                    .name(categoryName)
                    .keywords(description.toLowerCase())
                    .createdBy("LLM")
                    .build();
            newCategory = categoryRepository.save(newCategory);
            log.info("✨ New category discovered and saved: '{}'", categoryName);
            return Optional.of(newCategory);

        } catch (Exception e) {
            log.error("Gemini categorization failed for '{}': {}", description, e.getMessage());
            return Optional.empty();
        }
    }

    private String callGeminiApi(String prompt) {
        try {
            WebClient client = WebClient.builder().build();

            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", prompt)
                    ))
                )
            );

            String response = client.post()
                    .uri(geminiApiUrl + "?key=" + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Parse response: candidates[0].content.parts[0].text
            JsonNode root = objectMapper.readTree(response);
            return root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText()
                    .trim();

        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            return null;
        }
    }

    private String buildPrompt(String description, List<String> categories) {
        return String.format("""
            You are a financial expense categorizer.
            Given this expense description: "%s"
            
            Classify it into exactly ONE category from this list:
            %s
            
            Rules:
            - Return ONLY the category name, nothing else
            - If none fit perfectly, suggest a concise new category name (1-2 words max)
            - Do NOT return sentences, explanations, or punctuation
            """,
            description,
            String.join(", ", categories)
        );
    }
}
