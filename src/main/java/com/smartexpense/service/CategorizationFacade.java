package com.smartexpense.service;

import com.smartexpense.model.Category;
import com.smartexpense.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Orchestrates the categorization pipeline:
 * 1. Try keyword matching (fast, no external call)
 * 2. Try Gemini LLM (intelligent fallback)
 * 3. Fallback to "Uncategorized" if both fail
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategorizationFacade {

    private final KeywordCategorizationService keywordService;
    private final GeminiLLMService llmService;
    private final CategoryRepository categoryRepository;

    public Category categorize(String description) {
        // Step 1: Keyword match
        Optional<Category> keywordMatch = keywordService.categorize(description);
        if (keywordMatch.isPresent()) {
            log.debug("Using keyword categorization for: '{}'", description);
            return keywordMatch.get();
        }

        // Step 2: LLM fallback
        log.info("No keyword match, invoking Gemini LLM for: '{}'", description);
        Optional<Category> llmMatch = llmService.categorize(description);
        if (llmMatch.isPresent()) {
            return llmMatch.get();
        }

        // Step 3: Final fallback — create/get Uncategorized
        log.warn("LLM also failed for: '{}'. Using Uncategorized.", description);
        return categoryRepository.findByNameIgnoreCase("Uncategorized")
                .orElseGet(() -> categoryRepository.save(
                    Category.builder()
                        .name("Uncategorized")
                        .keywords("")
                        .createdBy("SYSTEM")
                        .build()
                ));
    }
}
