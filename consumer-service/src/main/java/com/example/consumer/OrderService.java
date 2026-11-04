package com.example.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final InventoryClient inventory;
    private final OrderRepository repository;
    private final ApplicationEventPublisher events;

    public OrderService(InventoryClient inventory, OrderRepository repository, ApplicationEventPublisher events) {
        this.inventory = inventory;
        this.repository = repository;
        this.events = events;
    }

    /** Fluxo SÍNCRONO: o cliente espera a verificação de estoque. Em falha, degrada (fallback). */
    public Order createSync(String sku, int qty) {
        Order order = repository.save(Order.create(sku, qty, verify(sku)));
        events.publishEvent(new OrderCreatedEvent(order));
        return order;
    }

    /** Fluxo ASSÍNCRONO: responde 202 imediatamente e processa em outra thread. */
    public Order createAsync(String sku, int qty) {
        Order order = repository.save(Order.create(sku, qty, "PROCESSING"));
        processAsync(order);
        return order;
    }

    @Async
    public void processAsync(Order order) {
        Order done = repository.save(order.withStatus(verify(order.sku())));
        events.publishEvent(new OrderCreatedEvent(done));
    }

    /** Fluxo assíncrono com TimeLimiter + Circuit Breaker. */
    public java.util.concurrent.CompletableFuture<Order> createWithTimeLimiter(String sku, int qty) {
        return inventory.checkStockAsync(sku)
                .thenApply(r -> repository.save(Order.create(sku, qty, "CONFIRMED")))
                .exceptionally(ex -> {
                    log.warn("Falha/timeout assíncrono: {}", ex.toString());
                    return repository.save(Order.create(sku, qty, "PENDING_STOCK_CHECK"));
                });
    }

    private String verify(String sku) {
        try {
            inventory.checkStock(sku);
            return "CONFIRMED";
        } catch (Exception e) {
            // Fallback / degradação graciosa: aceita o pedido e confere o estoque depois
            log.warn("Estoque indisponível ({}). Aplicando fallback.", e.getClass().getSimpleName());
            return "PENDING_STOCK_CHECK";
        }
    }
}
