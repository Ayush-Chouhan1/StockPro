package com.stockpro.purchaseservice.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

	public static final String PO_APPROVED_QUEUE  = "stockpro.po.approved";
    public static final String PO_PENDING_QUEUE   = "stockpro.po.pending";
    public static final String PO_OVERDUE_QUEUE   = "stockpro.po.overdue";
    public static final String EXCHANGE           = "stockpro.exchange";
    public static final String PO_APPROVED_KEY    = "po.approved";
    public static final String PO_PENDING_KEY     = "po.pending";
    public static final String PO_OVERDUE_KEY     = "po.overdue";

    @Bean
    public Queue poApprovedQueue() {
        return QueueBuilder.durable(PO_APPROVED_QUEUE).build();
    }

    @Bean
    public Queue poPendingQueue() {
        return QueueBuilder.durable(PO_PENDING_QUEUE).build();
    }

    @Bean
    public Queue poOverdueQueue() {
        return QueueBuilder.durable(PO_OVERDUE_QUEUE).build();
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Binding poApprovedBinding() {
        return BindingBuilder
                .bind(poApprovedQueue())
                .to(exchange())
                .with(PO_APPROVED_KEY);
    }

    @Bean
    public Binding poPendingBinding() {
        return BindingBuilder
                .bind(poPendingQueue())
                .to(exchange())
                .with(PO_PENDING_KEY);
    }

    @Bean
    public Binding poOverdueBinding() {
        return BindingBuilder
                .bind(poOverdueQueue())
                .to(exchange())
                .with(PO_OVERDUE_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
