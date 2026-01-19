package com.example.whisky.service;

import com.example.whisky.model.WhiskyDTO;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.Jsoup;
import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class WhiskyService {
    private static final Logger logger = LoggerFactory.getLogger(WhiskyService.class);

    @Value("${scraping.api.key}")
    private String scrapingApiKey;

    @Value("${scraping.api.url}")
    private String scrapingApiUrl;

    @Cacheable(value = "whiskyInfo", key = "#wbCode")
    public WhiskyDTO getWhiskyInfo(String wbCode) throws IOException, InterruptedException {
        String targetUrl = "https://www.whiskybase.com/whiskies/whisky/" + wbCode;
        logger.info("Cache miss for code: {}. Calling Scraping API for url: {}", wbCode, targetUrl);

        // 1. ScraperAPI를 호출하여 Cloudflare를 우회하고 HTML을 가져옵니다.
        String encodedUrl = URLEncoder.encode(targetUrl, StandardCharsets.UTF_8);
        // ScraperAPI의 URL 형식: {api_url}?api_key={key}&url={target_url}
        String apiUrl = scrapingApiUrl + "?api_key=" + scrapingApiKey + "&url=" + encodedUrl;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.error("Scraping API failed with status code: {} and body: {}", response.statusCode(), response.body());
            // 실제로는 API 실패에 대한 더 정교한 예외 처리가 필요합니다.
            throw new IOException("Scraping API request failed");
        }

        // 2. Jsoup으로 응답받은 HTML을 파싱합니다.
        Document doc = Jsoup.parse(response.body());
        try {
            return parseWhiskyData(doc, wbCode);
        } catch (org.json.JSONException e) {
            logger.error("Failed to parse JSON-LD data for code: {}. The page structure might have changed.", wbCode, e);
            throw new IOException("Failed to parse JSON data from the page.", e);
        }
    }

    private WhiskyDTO parseWhiskyData(Document doc, String wbCode) {
        Element scriptElement = doc.selectFirst("script[type=\"application/ld+json\"]");
        if (scriptElement == null) {
            logger.warn("JSON-LD script tag not found for code: {}", wbCode);
            return new WhiskyDTO(); // 구조화된 데이터가 없으면 빈 객체 반환
        }

        String jsonData = scriptElement.data();
        JSONObject json = new JSONObject(jsonData);

        WhiskyDTO dto = new WhiskyDTO();

        dto.setWbCode(json.optString("sku", wbCode));
        dto.setName(json.optString("name"));
        dto.setImage(json.optString("image"));

        // 평점 정보는 중첩된 객체 안에 있으므로 한 단계 더 들어감
        JSONObject ratingInfo = json.optJSONObject("aggregateRating");
        if (ratingInfo != null) {
            dto.setScore(ratingInfo.optString("ratingValue"));
            dto.setVotes(ratingInfo.optInt("reviewCount"));
        }

        // 이전의 안정적인 개별 파싱 방식으로 복원하고, 사용자의 요청에 따라 Casktype을 수정합니다.
        dto.setCategory(safeSelectText(doc, "dt:contains(Category) + dd"));
        dto.setDistillery(safeSelectText(doc, "dt:contains(Distillery) + dd"));
        dto.setBottler(safeSelectText(doc, "dt:contains(Bottler) + dd"));
        dto.setSeries(safeSelectText(doc, "dt:contains(Bottling serie) + dd"));
        dto.setAge(safeSelectText(doc, "dt:contains(Stated Age) + dd"));
        // 사용자의 요청에 따라 'Casktype' 대신 'Cask'를 포함하는 라벨을 찾도록 수정합니다.
        dto.setCaskType(safeSelectText(doc, "dt:contains(Cask) + dd"));
        dto.setStrength(safeSelectText(doc, "dt:contains(Strength) + dd"));

        return dto;
    }

    private String safeSelectText(Document doc, String cssQuery) {
        Element el = doc.selectFirst(cssQuery);
        return (el != null) ? el.text() : null;
    }
}