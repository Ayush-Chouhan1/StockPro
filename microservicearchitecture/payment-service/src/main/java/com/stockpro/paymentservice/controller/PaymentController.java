package com.stockpro.paymentservice.controller;

import com.stockpro.paymentservice.dto.*;
import com.stockpro.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@Slf4j
@Tag(name = "Payment Management",
     description = "APIs for Razorpay payment processing")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/create-order")
    @Operation(summary = "Create a Razorpay payment order")
    public ResponseEntity<PaymentOrderResponseDTO> createOrder(
            @Valid @RequestBody PaymentOrderRequestDTO dto) {
        log.info("Creating payment order for PO: {}", dto.getPurchaseOrderId());
        return new ResponseEntity<>(
                paymentService.createOrder(dto), HttpStatus.CREATED);
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify Razorpay payment signature")
    public ResponseEntity<PaymentResponseDTO> verifyPayment(
            @Valid @RequestBody PaymentVerificationDTO dto) {
        log.info("Verifying payment: {}", dto.getRazorpayPaymentId());
        return ResponseEntity.ok(paymentService.verifyPayment(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<PaymentResponseDTO> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment by Razorpay order ID")
    public ResponseEntity<PaymentResponseDTO> getByOrderId(
            @PathVariable String orderId) {
        return ResponseEntity.ok(
                paymentService.getPaymentByOrderId(orderId));
    }

    @GetMapping("/purchase-order/{poId}")
    @Operation(summary = "Get payments by purchase order ID")
    public ResponseEntity<List<PaymentResponseDTO>> getByPO(
            @PathVariable Long poId) {
        return ResponseEntity.ok(paymentService.getPaymentsByPO(poId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get payments by user")
    public ResponseEntity<List<PaymentResponseDTO>> getByUser(
            @PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.getPaymentsByUser(userId));
    }

    @GetMapping
    @Operation(summary = "Get all payments")
    public ResponseEntity<List<PaymentResponseDTO>> getAll() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get payments by status")
    public ResponseEntity<List<PaymentResponseDTO>> getByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(paymentService.getPaymentsByStatus(status));
    }
}