package com.example.consumer;

/** Evento de domínio: algo que já aconteceu (nome no passado). */
public record OrderCreatedEvent(Order order) {
}
