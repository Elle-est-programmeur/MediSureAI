package com.example.Backend.service.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.document}")
    private String exchange;

    @Value("${rabbitmq.routing-key.document-processing}")
    private String routingKey;

    public void sendForProcessing(Long documentId) {
        Map<String, Object> message = Map.of(
                "documentId", documentId,
                "timestamp", System.currentTimeMillis()
        );

        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        log.info("Queued document [id={}] for async processing", documentId);
    }
}