package com.stockpro.warehouseservice.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Queue names
    public static final String LOW_STOCK_QUEUE      = "low-stock-queue";
    public static final String OVERSTOCK_QUEUE      = "overstock-queue";
    public static final String GOODS_RECEIVED_QUEUE = "goods-received-queue";

    // Exchange name
    public static final String STOCKPRO_EXCHANGE = "stockpro.exchange";

    // Routing keys
    public static final String LOW_STOCK_ROUTING_KEY      = "stock.low";
    public static final String OVERSTOCK_ROUTING_KEY      = "stock.over";
    public static final String GOODS_RECEIVED_ROUTING_KEY = "stock.goods-received";

    // ==================== QUEUES ====================
    // NOTE: The return type MUST be org.springframework.amqp.core.Queue
    //       NOT java.util.Queue — that was the compilation error

    @Bean
    public Queue lowStockQueue() {
        return new Queue(LOW_STOCK_QUEUE, true); // durable = true
    }

    @Bean
    public Queue overstockQueue() {
        return new Queue(OVERSTOCK_QUEUE, true);
    }

    @Bean
    public Queue goodsReceivedQueue() {
        return new Queue(GOODS_RECEIVED_QUEUE, true);
    }

    // ==================== EXCHANGE ====================

    @Bean
    public DirectExchange stockproExchange() {
        return new DirectExchange(STOCKPRO_EXCHANGE);
    }

    // ==================== BINDINGS ====================

    @Bean
    public Binding lowStockBinding(Queue lowStockQueue,
                                   DirectExchange stockproExchange) {
        return BindingBuilder
                .bind(lowStockQueue)
                .to(stockproExchange)
                .with(LOW_STOCK_ROUTING_KEY);
    }

    @Bean
    public Binding overstockBinding(Queue overstockQueue,
                                    DirectExchange stockproExchange) {
        return BindingBuilder
                .bind(overstockQueue)
                .to(stockproExchange)
                .with(OVERSTOCK_ROUTING_KEY);
    }

    @Bean
    public Binding goodsReceivedBinding(Queue goodsReceivedQueue,
                                        DirectExchange stockproExchange) {
        return BindingBuilder
                .bind(goodsReceivedQueue)
                .to(stockproExchange)
                .with(GOODS_RECEIVED_ROUTING_KEY);
    }

    // ==================== TEMPLATE ====================

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}