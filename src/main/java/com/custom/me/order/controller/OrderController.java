package com.custom.me.order.controller;

import com.custom.me.order.service.OrderService;
import com.custom.me.order.service.dto.MaterialResponse;
import com.custom.me.order.service.dto.OrderRequest;
import com.custom.me.order.service.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({""})
@RequiredArgsConstructor
public class OrderController {
    private final OrderService service;

    @PostMapping("/order")
    public OrderResponse order(@RequestBody OrderRequest request) {
        return service.order(request);
    }

    @GetMapping("/order/{orderNumber}")
    public OrderResponse findByOrderNumber(@PathVariable String orderNumber) { return service.findByOrderNumber(orderNumber); }

    @PostMapping("/material/{code}")
    public MaterialResponse addMaterial(@PathVariable String code) { return service.updateMaterial(code, "ADD"); }

    @PatchMapping("/material/{code}")
    public MaterialResponse refillMaterial(@PathVariable String code) { return service.updateMaterial(code, "ADD"); }

    @DeleteMapping("/material/{code}")
    public MaterialResponse removeMaterial(@PathVariable String code) { return service.updateMaterial(code, "DELETE"); }

    @GetMapping("/material")
    public MaterialResponse getMachine(@PathVariable String code) { return service.getMachine(); }
}
