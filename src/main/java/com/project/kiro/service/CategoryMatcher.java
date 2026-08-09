package com.project.kiro.service;

import com.project.kiro.model.Category;
import com.project.kiro.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryMatcher {

    private final CategoryRepository categoryRepository;

    // Ordered rule table — first match wins
    private static final List<CategoryRule> RULES = List.of(
        new CategoryRule("Food", List.of("swiggy", "zomato", "bigbasket", "blinkit", "zepto", "dunzo", "dominos", "pizza", "restaurant", "\\bfood\\b", "\\bcafe\\b", "burger", "biryani", "\\bkfc\\b", "mcdonalds")),
        new CategoryRule("Transport", List.of("\\buber\\b", "\\bola\\b", "rapido", "\\bmetro\\b", "petrol", "\\bfuel\\b", "indian oil", "hp petrol", "bharat petroleum", "irctc", "railway", "parking", "redbus", "makemytrip", "goibibo", "cleartrip")),
        new CategoryRule("Shopping", List.of("amazon", "flipkart", "myntra", "meesho", "ajio", "nykaa", "tatacliq")),
        new CategoryRule("Entertainment", List.of("netflix", "spotify", "hotstar", "youtube", "prime", "disney", "jiocinema", "bookmyshow", "pvr", "inox")),
        new CategoryRule("Healthcare", List.of("pharmacy", "hospital", "clinic", "medplus", "apollo", "1mg", "netmeds", "pharmeasy", "doctor")),
        new CategoryRule("Utilities", List.of("\\bjio\\b", "airtel", "\\bvi\\b", "vodafone", "electricity", "\\bwater\\b", "\\bgas\\b", "broadband", "wifi", "recharge")),
        new CategoryRule("Education", List.of("udemy", "coursera", "school", "college", "tuition", "books")),
        new CategoryRule("Investment", List.of("\\blic\\b", "\\blici\\b", "lic of india", "mutual fund", "\\bsip\\b", "\\bmf\\b", "groww", "zerodha", "kuvera", "auto debit", "auto-debit", "mandate", "\\bnach\\b", "\\bemi\\b", "life insurance", "insurance premium", "\\bppf\\b", "\\bnps\\b", "investment", "fixed deposit", "recurring deposit"))
    );

    public CategoryMatcher(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Matches a merchant name to a category using keyword rules.
     * Keywords starting with \b use regex word-boundary matching.
     * Other keywords use simple contains matching.
     * Returns the first matching category in priority order.
     * Falls back to "Other" if no match or null/empty input.
     */
    public Category match(String merchantName) {
        if (merchantName == null || merchantName.isBlank()) {
            return resolveCategory("Other");
        }

        String lower = merchantName.toLowerCase();
        for (CategoryRule rule : RULES) {
            for (String keyword : rule.keywords()) {
                if (keyword.startsWith("\\b")) {
                    // Word-boundary regex match for short keywords
                    if (java.util.regex.Pattern.compile(keyword, java.util.regex.Pattern.CASE_INSENSITIVE).matcher(lower).find()) {
                        return resolveCategory(rule.categoryName());
                    }
                } else {
                    // Simple contains for longer keywords
                    if (lower.contains(keyword)) {
                        return resolveCategory(rule.categoryName());
                    }
                }
            }
        }

        return resolveCategory("Other");
    }

    private Category resolveCategory(String name) {
        return categoryRepository.findByNameLower(name.toLowerCase())
            .orElseGet(() -> categoryRepository.findByNameLower("other")
                .orElseThrow(() -> new IllegalStateException("'Other' category must exist in database")));
    }

    /**
     * Matches category from the full email body text.
     * Scans the body for any known keywords from all category rules.
     * Uses word-boundary matching for short keywords.
     */
    public Category matchFromBody(String emailBody) {
        if (emailBody == null || emailBody.isBlank()) {
            return resolveCategory("Other");
        }

        String lower = emailBody.toLowerCase();
        for (CategoryRule rule : RULES) {
            for (String keyword : rule.keywords()) {
                if (keyword.startsWith("\\b")) {
                    if (java.util.regex.Pattern.compile(keyword, java.util.regex.Pattern.CASE_INSENSITIVE).matcher(lower).find()) {
                        return resolveCategory(rule.categoryName());
                    }
                } else {
                    if (lower.contains(keyword)) {
                        return resolveCategory(rule.categoryName());
                    }
                }
            }
        }

        return resolveCategory("Other");
    }

    private record CategoryRule(String categoryName, List<String> keywords) {}
}
