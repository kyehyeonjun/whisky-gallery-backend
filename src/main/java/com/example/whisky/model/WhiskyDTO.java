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
    private String series;
    private String age;
    private String caskType;
    private String strength;
}