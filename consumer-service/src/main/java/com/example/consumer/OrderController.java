package com.example.consumer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrderController {

    private final OrderService service;
    private final OrderRepository repository;
    private final InventoryClient inventory;

    public OrderController(OrderService service, OrderRepository repository, InventoryClient inventory) {
        this.service = service;
        this.repository = repository;
        this.inventory = inventory;
    }

    @PostMapping("/orders/sync")
    public Order sync(@RequestParam String sku, @RequestParam(defaultValue = "1") int qty) {
        return service.createSync(sku, qty);
    }

    @PostMapping("/orders/async")
    public ResponseEntity<Order> async(@RequestParam String sku, @RequestParam(defaultValue = "1") int qty) {
        return ResponseEntity.accepted().body(service.createAsync(sku, qty));
    }

    @PostMapping("/orders/timelimiter")
    public CompletableFuture<Order> timeLimiter(@RequestParam String sku, @RequestParam(defaultValue = "1") int qty) {
        return service.createWithTimeLimiter(sku, qty);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> get(@PathVariable String id) {
        return repository.find(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /** Chamada crua (sem fallback): expõe a exceção para observar retry / circuit breaker. */
    @GetMapping("/inventory/{sku}")
    public Map<String, Object> inventory(@PathVariable String sku) {
        return inventory.checkStock(sku);
    }
}
