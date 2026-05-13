package com.stockpro.movementservice.entity;

public enum MovementType {
    STOCK_IN,       // Goods received (GRN)
    STOCK_OUT,      // Issued to production/sales
    TRANSFER_IN,    // Received from another warehouse
    TRANSFER_OUT,   // Sent to another warehouse
    ADJUSTMENT,     // Manual correction
    WRITE_OFF,      // Damaged/expired removal
    RETURN          // Supplier/customer return
}