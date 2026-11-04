package com.example.consumer;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP do serviço de estoque com padrões de resiliência.
 * Ordem dos aspectos (de fora para dentro): Retry -> CircuitBreaker -> chamada HTTP (com timeout).
 */
@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);

    private final RestClient http;

    public InventoryClient(@Value("${inventory.base-url}") String baseUrl,
                           @Value("${inventory.connect-timeout-ms}") long connectMs,
                           @Value("${inventory.read-timeout-ms}") long readMs) {
        var settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(connectMs))
                .withReadTimeout(Duration.ofMillis(readMs));
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }

    /** Síncrono: timeout HTTP + retry com backoff exponencial + circuit breaker. */
    @Retry(name = "inventory")
    @CircuitBreaker(name = "inventory")
    public Map<String, Object> checkStock(String sku) {
        log.info("Chamando estoque (sync) sku={}", sku);
        return call(sku);
    }

    /** Assíncrono: TimeLimiter limita o tempo de espera do CompletableFuture. */
    @TimeLimiter(name = "inventory")
    @CircuitBreaker(name = "inventory")
    public CompletableFuture<Map<String, Object>> checkStockAsync(String sku) {
        log.info("Chamando estoque (async) sku={}", sku);
        return CompletableFuture.supplyAsync(() -> call(sku));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(String sku) {
        return http.get().uri("/stock/{sku}", sku).retrieve().body(Map.class);
    }
}
