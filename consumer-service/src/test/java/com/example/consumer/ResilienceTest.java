package com.example.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// Aponta para uma porta sem servidor: toda chamada falha -> retry esgota -> circuito abre.
@SpringBootTest(properties = "inventory.base-url=http://localhost:1")
class ResilienceTest {

    @Autowired OrderService orders;
    @Autowired InventoryClient client;
    @Autowired CircuitBreakerRegistry registry;

    @Test
    void fallbackKeepsOrderAcceptedAndCircuitOpens() {
        Order o = orders.createSync("ABC", 1);
        assertThat(o.status()).isEqualTo("PENDING_STOCK_CHECK");

        for (int i = 0; i < 5; i++) {
            orders.createSync("ABC", 1);
        }
        assertThat(registry.circuitBreaker("inventory").getState())
                .isEqualTo(CircuitBreaker.State.OPEN);
        assertThatThrownBy(() -> client.checkStock("ABC")).isInstanceOf(CallNotPermittedException.class);
    }
}
