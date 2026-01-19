package com.example.whisky.controller;

import com.example.whisky.model.WhiskyDTO;
import com.example.whisky.service.WhiskyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/whisky")
public class WhiskyController {

    private static final Logger logger = LoggerFactory.getLogger(WhiskyController.class);

    @Autowired
    private WhiskyService whiskyService;

    @GetMapping("/{code}")
    public ResponseEntity<WhiskyDTO> getWhisky(@PathVariable String code) {
        try {
            // Scraping API를 사용하므로, 이제 메서드는 충분히 빠릅니다.
            // Spring의 @Cacheable이 자동으로 캐시를 확인하고, 없으면 메서드를 실행합니다.
            WhiskyDTO whiskyInfo = whiskyService.getWhiskyInfo(code);
            if (whiskyInfo.getName() == null || whiskyInfo.getName().isEmpty()) {
                logger.warn("Whisky not found for code: {}", code);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(whiskyInfo, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error fetching whisky info for code: {}", code, e);
            // 서버 측의 예외는 Internal Server Error로 처리
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}