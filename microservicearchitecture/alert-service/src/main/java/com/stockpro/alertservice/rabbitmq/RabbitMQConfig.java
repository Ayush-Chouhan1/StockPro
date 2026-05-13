package com.stockpro.alertservice.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Must match exact names from producer services
    public static final String LOW_STOCK_QUEUE   = "stockpro.low.stock";
    public static final String OVERSTOCK_QUEUE   = "stockpro.overstock";
    public static final String PO_APPROVED_QUEUE = "stockpro.po.approved";
    public static final String PO_PENDING_QUEUE  = "stockpro.po.pending";
    public static final String PO_OVERDUE_QUEUE  = "stockpro.po.overdue";
    public static final String EXCHANGE          = "stockpro.exchange";

    @Bean
    public Queue lowStockQueue() {
        return QueueBuilder.durable(LOW_STOCK_QUEUE).build();
    }

    @Bean
    public Queue overstockQueue() {
        return QueueBuilder.durable(OVERSTOCK_QUEUE).build();
    }

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
    public Binding lowStockBinding() {
        return BindingBuilder.bind(lowStockQueue())
                .to(exchange()).with("stock.low");
    }

    @Bean
    public Binding overstockBinding() {
        return BindingBuilder.bind(overstockQueue())
                .to(exchange()).with("stock.over");
    }

    @Bean
    public Binding poApprovedBinding() {
        return BindingBuilder.bind(poApprovedQueue())
                .to(exchange()).with("po.approved");
    }

    @Bean
    public Binding poPendingBinding() {
        return BindingBuilder.bind(poPendingQueue())
                .to(exchange()).with("po.pending");
    }

    @Bean
    public Binding poOverdueBinding() {
        return BindingBuilder.bind(poOverdueQueue())
                .to(exchange()).with("po.overdue");
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
