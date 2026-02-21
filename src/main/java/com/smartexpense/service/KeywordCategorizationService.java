package com.smartexpense.service;

import com.smartexpense.model.Category;
import com.smartexpense.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * First-pass categorization using keyword matching.
 * Loads all categories and performs case-insensitive keyword lookup on the description.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeywordCategorizationService {

    private final CategoryRepository categoryRepository;

    /**
     * Attempts to categorize by matching description words against stored keywords.
     * @param description expense description
     * @return matched Category, or empty if no match
     */
    public Optional<Category> categorize(String description) {
        if (description == null || description.isBlank()) {
            return Optional.empty();
        }

        String normalizedDesc = description.toLowerCase().trim();
        List<Category> categories = categoryRepository.findAll();

        for (Category category : categories) {
            if (category.getKeywords() == null || category.getKeywords().isBlank()) {
                continue;
            }

            List<String> keywords = Arrays.stream(category.getKeywords().split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .toList();

            boolean matched = keywords.stream().anyMatch(normalizedDesc::contains);
            if (matched) {
                log.debug("Keyword match: '{}' → category '{}'", description, category.getName());
                return Optional.of(category);
            }
        }

        log.debug("No keyword match found for: '{}'", description);
        return Optional.empty();
    }
}
