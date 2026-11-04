package com.example.consumer;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {
    private final Map<String, Order> store = new ConcurrentHashMap<>();

    public Order save(Order o) {
        store.put(o.id(), o);
        return o;
    }

    public Optional<Order> find(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
