package com.example.whisky.service;

import com.example.whisky.model.WhiskyDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class WhiskyService {

    private static final Logger logger = LoggerFactory.getLogger(WhiskyService.class);

    @Value("${scraping.api.url}")
    private String apiUrl;

    @Value("${scraping.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Cacheable(value = "whiskyInfo", key = "#code")
    public WhiskyDTO getWhiskyInfo(String code) {
        String targetUrl = "https://www.whiskybase.com/whiskies/whisky/" + code;
        logger.info("Cache miss for code: {}. Calling Scraping API for url: {}", code, targetUrl);

        String url = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("api_key", apiKey)
                .queryParam("url", targetUrl)
                .toUriString();

        String html = restTemplate.getForObject(url, String.class);

        if (html == null || html.isEmpty()) {
            logger.warn("Received empty HTML response for code: {}", code);
            return new WhiskyDTO();
        }

        return parseWhiskyInfo(html);
    }

    private WhiskyDTO parseWhiskyInfo(String html) {
        Document doc = Jsoup.parse(html);
        WhiskyDTO dto = new WhiskyDTO();

        // Use the null-safe helper method for robust parsing
        dto.setName(safeGetText(doc, "h1[itemprop=name]"));
        dto.setBottler(safeGetText(doc, "a[href*='/distilleries/']")); // Example selector
        dto.setStrength(safeGetText(doc, "span[itemprop=strength]")); // Example selector
        // ... Add other fields using safeGetText with their correct selectors

        return dto;
    }

    /**
     * Safely extracts text from an element found by a CSS selector.
     * Returns null if the element is not found, preventing NullPointerException.
     *
     * @param parent   The parent element (e.g., the Document)
     * @param selector The CSS selector to find the element
     * @return The text of the element, or null if not found.
     */
    private String safeGetText(Element parent, String selector) {
        try {
            Element element = parent.selectFirst(selector);
            return (element != null) ? element.text() : null;
        } catch (Exception e) {
            logger.warn("Could not parse element with selector '{}'", selector, e);
            return null;
        }
    }
}