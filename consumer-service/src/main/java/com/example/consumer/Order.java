package com.example.consumer;

import java.util.UUID;

public record Order(String id, String sku, int quantity, String status) {

    public static Order create(String sku, int quantity, String status) {
        return new Order(UUID.randomUUID().toString(), sku, quantity, status);
    }

    public Order withStatus(String newStatus) {
        return new Order(id, sku, quantity, newStatus);
    }
}
