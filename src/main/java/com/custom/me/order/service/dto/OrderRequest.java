package com.custom.me.order.service.dto;

import lombok.Data;

@Data
public class OrderRequest {
    private String order_number;
    private String order;
    private String order_date;
}
