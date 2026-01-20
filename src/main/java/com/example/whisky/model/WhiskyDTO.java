package com.example.whisky.model;

import lombok.Data;

@Data
public class WhiskyDTO {
    private String wbCode;
    private String name;
    private String score;
    private int votes;
    private String image;
    private String category;
    private String distillery;
    private String bottler;
    private String series; // Bottling series
    private String age; // Stated Age
    private String bottled;
    private String caskType; // Casktype
    private String casknumber;
    private String strength;
    private String size;
}