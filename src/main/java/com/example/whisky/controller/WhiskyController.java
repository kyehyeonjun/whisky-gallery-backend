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

    private final WhiskyService whiskyService;

    public WhiskyController(WhiskyService whiskyService) {
        this.whiskyService = whiskyService;
    }

    @GetMapping("/{code}")
    public ResponseEntity<WhiskyDTO> getWhisky(@PathVariable String code) {
        // Exceptions are now handled globally by GlobalExceptionHandler.
        WhiskyDTO whiskyInfo = whiskyService.getWhiskyInfo(code);
        if (whiskyInfo.getName() == null || whiskyInfo.getName().isEmpty()) {
            logger.warn("Whisky not found for code: {}", code);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(whiskyInfo, HttpStatus.OK);
    }
}