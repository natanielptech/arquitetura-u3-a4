package com.example.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** Consumidores independentes do mesmo evento (publish/subscribe, desacoplados do OrderService). */
@Component
public class OrderEventListeners {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListeners.class);

    @Async
    @EventListener
    public void notifyCustomer(OrderCreatedEvent e) {
        log.info("[Notificação] Pedido {} status={}", e.order().id(), e.order().status());
    }

    @Async
    @EventListener
    public void billing(OrderCreatedEvent e) {
        log.info("[Faturamento] Gerando cobrança do pedido {}", e.order().id());
    }
}
