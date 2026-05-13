package com.stockpro.warehouseservice.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class StockEventPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // Called when stock drops below reorder level
    public void publishLowStockEvent(Long productId, Long warehouseId,
                                     Integer currentQty, Integer reorderLevel) {
        Map<String, Object> event = Map.of(
                "productId",    productId,
                "warehouseId",  warehouseId,
                "currentQty",   currentQty,
                "reorderLevel", reorderLevel,
                "eventType",    "LOW_STOCK"
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.STOCKPRO_EXCHANGE,
                RabbitMQConfig.LOW_STOCK_ROUTING_KEY,
                event
        );
        log.info("Published LOW_STOCK event for product {} in warehouse {}",
                productId, warehouseId);
    }

    // Called when stock exceeds max stock level
    public void publishOverstockEvent(Long productId, Long warehouseId,
                                      Integer currentQty, Integer maxLevel) {
        Map<String, Object> event = Map.of(
                "productId",   productId,
                "warehouseId", warehouseId,
                "currentQty",  currentQty,
                "maxLevel",    maxLevel,
                "eventType",   "OVERSTOCK"
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.STOCKPRO_EXCHANGE,
                RabbitMQConfig.OVERSTOCK_ROUTING_KEY,
                event
        );
        log.info("Published OVERSTOCK event for product {} in warehouse {}",
                productId, warehouseId);
    }

    // Called when goods are received (PO receipt)
    public void publishGoodsReceivedEvent(Long productId, Long warehouseId,
                                          Integer receivedQty, Long poId) {
        Map<String, Object> event = Map.of(
                "productId",   productId,
                "warehouseId", warehouseId,
                "receivedQty", receivedQty,
                "poId",        poId,
                "eventType",   "GOODS_RECEIVED"
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.STOCKPRO_EXCHANGE,
                RabbitMQConfig.GOODS_RECEIVED_ROUTING_KEY,
                event
        );
        log.info("Published GOODS_RECEIVED event for product {} in warehouse {}",
                productId, warehouseId);
    }
}