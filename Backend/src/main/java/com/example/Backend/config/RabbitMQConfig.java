package com.example.Backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.document-processing}")
    private String queueName;

    @Value("${rabbitmq.exchange.document}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key.document-processing}")
    private String routingKey;

    @Bean
    public Queue documentProcessingQueue() {
        return new Queue(queueName, true); // durable — survives broker restart
    }

    @Bean
    public TopicExchange documentExchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Binding documentBinding(Queue documentProcessingQueue, TopicExchange documentExchange) {
        return BindingBuilder.bind(documentProcessingQueue).to(documentExchange).with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}