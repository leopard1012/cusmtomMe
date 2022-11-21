package com.custom.me.order.service.dto;

import lombok.Data;

import java.util.Map;

@Data
public class MaterialResponse {
    private Map<String, Integer> material;
    private String result;
}
