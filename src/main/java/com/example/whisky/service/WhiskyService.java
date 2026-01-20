package com.example.whisky.service;

import com.example.whisky.model.WhiskyDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.json.JSONObject;
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

    private final RestTemplate restTemplate;

    public WhiskyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

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

        return parseWhiskyInfo(Jsoup.parse(html), code);
    }

    private WhiskyDTO parseWhiskyInfo(Document doc, String code) {
        WhiskyDTO dto = new WhiskyDTO();
        Element scriptElement = doc.selectFirst("script[type=\"application/ld+json\"]");
        if (scriptElement != null) {
            JSONObject json = new JSONObject(scriptElement.data());
            dto.setName(json.optString("name"));
            dto.setImage(json.optString("image"));
            JSONObject ratingInfo = json.optJSONObject("aggregateRating");
            if (ratingInfo != null) {
                dto.setScore(ratingInfo.optString("ratingValue"));
                dto.setVotes(ratingInfo.optInt("reviewCount"));
            }
        }

        dto.setWbCode(code);
        dto.setCategory(safeSelectText(doc, "dt:contains(Category) + dd"));
        dto.setDistillery(safeSelectText(doc, "dt:contains(Distillery) + dd"));
        dto.setBottler(safeSelectText(doc, "dt:contains(Bottler) + dd"));
        dto.setStrength(safeSelectText(doc, "dt:contains(Strength) + dd"));

        // 이전에 누락되었던 상세 정보들을 다시 추가합니다.
        dto.setSeries(safeSelectText(doc, "dt:contains(Bottling serie) + dd"));
        dto.setAge(safeSelectText(doc, "dt:contains(Stated Age) + dd"));
        dto.setBottled(safeSelectText(doc, "dt:contains(Bottled) + dd"));
        dto.setCaskType(safeSelectText(doc, "dt:contains(Cask) + dd"));
        dto.setCasknumber(safeSelectText(doc, "dt:contains(Casknumber) + dd"));
        dto.setSize(safeSelectText(doc, "dt:contains(Size) + dd"));
        return dto;
    }

    private String safeSelectText(Document doc, String cssQuery) {
        Element el = doc.selectFirst(cssQuery);
        return (el != null) ? el.text() : null;
    }
}