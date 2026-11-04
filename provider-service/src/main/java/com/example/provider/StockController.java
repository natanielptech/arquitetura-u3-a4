package com.example.provider;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Serviço "instável" de estoque. O comportamento é controlado em runtime via /chaos,
 * permitindo simular lentidão e falhas para demonstrar timeout, retry e circuit breaker.
 */
@RestController
public class StockController {

    private final AtomicLong delayMs = new AtomicLong(0);
    private final AtomicInteger failureRate = new AtomicInteger(0); // 0..100 (%)

    @GetMapping("/stock/{sku}")
    public Map<String, Object> stock(@PathVariable String sku) throws InterruptedException {
        long delay = delayMs.get();
        if (delay > 0) {
            Thread.sleep(delay);
        }
        if (ThreadLocalRandom.current().nextInt(100) < failureRate.get()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha simulada");
        }
        return Map.of("sku", sku, "available", 42);
    }

    @PostMapping("/chaos")
    public Map<String, Object> chaos(@RequestParam(defaultValue = "0") long delayMs,
                                     @RequestParam(defaultValue = "0") int failureRate) {
        this.delayMs.set(Math.max(0, delayMs));
        this.failureRate.set(Math.min(100, Math.max(0, failureRate)));
        return Map.of("delayMs", this.delayMs.get(), "failureRate", this.failureRate.get());
    }
}
