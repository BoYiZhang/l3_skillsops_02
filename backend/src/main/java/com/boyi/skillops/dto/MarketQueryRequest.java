package com.boyi.skillops.dto;

import lombok.Data;

@Data
public class MarketQueryRequest {
    private Long categoryId;
    private String keyword;
    private String sortBy;
    private long page = 1;
    private long size = 12;
}
