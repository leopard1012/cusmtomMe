package com.custom.me.order.service.dto;

import lombok.Data;

@Data
public class OrderResponse {
    private String order_number;
    private String order;
    private String send_date;
    private String status;
    private String error;
}
