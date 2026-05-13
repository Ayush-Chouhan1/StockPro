package com.stockpro.paymentservice.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.stockpro.paymentservice.dto.PaymentOrderRequestDTO;
import com.stockpro.paymentservice.dto.PaymentOrderResponseDTO;
import com.stockpro.paymentservice.dto.PaymentResponseDTO;
import com.stockpro.paymentservice.dto.PaymentVerificationDTO;
import com.stockpro.paymentservice.entity.Payment;
import com.stockpro.paymentservice.entity.PaymentStatus;
import com.stockpro.paymentservice.exception.PaymentException;
import com.stockpro.paymentservice.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

@Service
@Slf4j
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RazorpayClient razorpayClient;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency:INR}")
    private String defaultCurrency;

    // ==================== CREATE ORDER ====================

    @Transactional
    public PaymentOrderResponseDTO createOrder(PaymentOrderRequestDTO dto) {
        log.info("Creating Razorpay order for PO: {}", dto.getPurchaseOrderId());

        try {
            int amountInPaise = dto.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .intValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency",
                    dto.getCurrency() != null ? dto.getCurrency() : defaultCurrency);
            orderRequest.put("receipt", "PO-" + dto.getPurchaseOrderId());
            orderRequest.put("payment_capture", 1);

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            // Save payment record
            Payment payment = Payment.builder()
                    .razorpayOrderId(razorpayOrderId)
                    .purchaseOrderId(dto.getPurchaseOrderId())
                    .userId(dto.getUserId())
                    .amount(dto.getAmount())
                    .currency(dto.getCurrency() != null ? dto.getCurrency() : defaultCurrency)
                    .status(PaymentStatus.CREATED)
                    .description(dto.getDescription())
                    .build();

            Payment saved = paymentRepository.save(payment);
            log.info("Payment order created: {}", razorpayOrderId);

            return PaymentOrderResponseDTO.builder()
                    .paymentId(saved.getPaymentId())
                    .razorpayOrderId(razorpayOrderId)
                    .amount(dto.getAmount())
                    .currency(payment.getCurrency())
                    .status("CREATED")
                    .description(dto.getDescription())
                    .razorpayKeyId(razorpayKeyId)
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new PaymentException("Failed to create payment order: " + e.getMessage());
        }
    }

    // ==================== VERIFY PAYMENT ====================

    @Transactional
    public PaymentResponseDTO verifyPayment(PaymentVerificationDTO dto) {
        log.info("Verifying payment for order: {}", dto.getRazorpayOrderId());

        Payment payment = paymentRepository
                .findByRazorpayOrderId(dto.getRazorpayOrderId())
                .orElseThrow(() -> new PaymentException(
                        "Payment not found for order: " + dto.getRazorpayOrderId()));

        boolean isValid = verifySignature(
                dto.getRazorpayOrderId(),
                dto.getRazorpayPaymentId(),
                dto.getRazorpaySignature());

        if (!isValid) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Invalid payment signature");
            paymentRepository.save(payment);
            throw new PaymentException("Payment verification failed: invalid signature");
        }

        payment.setRazorpayPaymentId(dto.getRazorpayPaymentId());
        payment.setRazorpaySignature(dto.getRazorpaySignature());
        payment.setStatus(PaymentStatus.SUCCESS);

        Payment saved = paymentRepository.save(payment);
        log.info("Payment verified successfully: {}", dto.getRazorpayPaymentId());
        return mapToDTO(saved);
    }

    // ==================== GET ====================

    public PaymentResponseDTO getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentException("Payment not found with ID: " + id));
        return mapToDTO(payment);
    }

    public PaymentResponseDTO getPaymentByOrderId(String orderId) {
        Payment payment = paymentRepository
                .findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new PaymentException(
                        "Payment not found for order: " + orderId));
        return mapToDTO(payment);
    }

    public List<PaymentResponseDTO> getPaymentsByPO(Long purchaseOrderId) {
        return paymentRepository.findByPurchaseOrderId(purchaseOrderId)
                .stream().map(this::mapToDTO).toList();
    }

    public List<PaymentResponseDTO> getPaymentsByUser(Long userId) {
        return paymentRepository.findByUserId(userId)
                .stream().map(this::mapToDTO).toList();
    }

    public List<PaymentResponseDTO> getAllPayments() {
        return paymentRepository.findAll()
                .stream().map(this::mapToDTO).toList();
    }

    public List<PaymentResponseDTO> getPaymentsByStatus(String status) {
        try {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            return paymentRepository.findByStatus(paymentStatus)
                    .stream().map(this::mapToDTO).toList();
        } catch (IllegalArgumentException e) {
            throw new PaymentException("Invalid payment status: " + status);
        }
    }

    // ==================== SIGNATURE VERIFICATION ====================

    private boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            String data = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String generated = HexFormat.of().formatHex(hash);
            return generated.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    // ==================== HELPER ====================

    private PaymentResponseDTO mapToDTO(Payment p) {
        return PaymentResponseDTO.builder()
                .paymentId(p.getPaymentId())
                .razorpayOrderId(p.getRazorpayOrderId())
                .razorpayPaymentId(p.getRazorpayPaymentId())
                .purchaseOrderId(p.getPurchaseOrderId())
                .userId(p.getUserId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .description(p.getDescription())
                .failureReason(p.getFailureReason())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}