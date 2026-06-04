package com.example.Backend.service;

import com.example.Backend.model.Billing;
import com.example.Backend.model.BillingStatus;
import com.example.Backend.model.Patient;
import com.example.Backend.model.Receipt;
import com.example.Backend.repository.BillingRepository;
import com.example.Backend.repository.ReceiptRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Atomic billing payment transitions. Split into two REQUIRES_NEW transactions so the
 * PROCESSING write actually commits before the PAID write — anyone querying the DB
 * mid-flight will see status=PROCESSING.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillingPaymentService {

    private final BillingRepository billingRepository;
    private final ReceiptRepository receiptRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Billing markProcessing(Long billingId, Long patientUserId) {
        Billing billing = loadOwned(billingId, patientUserId);
        if (billing.getStatus() == BillingStatus.PAID) {
            return billing;
        }
        billing.setStatus(BillingStatus.PROCESSING);
        return billingRepository.saveAndFlush(billing);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentResult markPaidAndIssueReceipt(Long billingId, Long patientUserId, String paymentMethod) {
        Billing billing = loadOwned(billingId, patientUserId);

        if (billing.getStatus() == BillingStatus.PAID) {
            Receipt existing = receiptRepository.findByBillingId(billing.getId()).orElse(null);
            return new PaymentResult(billing, existing);
        }

        billing.setStatus(BillingStatus.PAID);
        billing.setPaidAt(LocalDateTime.now());
        billing.setPaymentMethod(normalizeMethod(paymentMethod));
        Billing saved = billingRepository.saveAndFlush(billing);

        Receipt receipt = Receipt.builder()
                .billing(saved)
                .receiptNumber("RCPT-" + System.currentTimeMillis())
                .transactionRef("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase())
                .paymentMethod(saved.getPaymentMethod())
                .amount(saved.getTotalCost())
                .patientName(saved.getPatient() != null ? saved.getPatient().getName() : null)
                .description(saved.getDescription())
                .build();
        Receipt savedReceipt = receiptRepository.save(receipt);

        log.info("Patient[{}] paid Billing[{}] ₹{} via {} → Receipt[{}]",
                patientUserId, saved.getId(), saved.getTotalCost(),
                saved.getPaymentMethod(), savedReceipt.getReceiptNumber());

        return new PaymentResult(saved, savedReceipt);
    }

    private Billing loadOwned(Long billingId, Long patientUserId) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new EntityNotFoundException("Billing not found: " + billingId));
        Patient patient = billing.getPatient();
        if (patient == null || patient.getUser() == null
                || !patientUserId.equals(patient.getUser().getId())) {
            throw new AccessDeniedException("This billing entry does not belong to you");
        }
        return billing;
    }

    private String normalizeMethod(String method) {
        if (method == null || method.isBlank()) return "UPI";
        String upper = method.trim().toUpperCase();
        return switch (upper) {
            case "UPI", "CARD", "NET_BANKING", "NETBANKING" -> upper.equals("NETBANKING") ? "NET_BANKING" : upper;
            default -> "UPI";
        };
    }

    public record PaymentResult(Billing billing, Receipt receipt) {}
}
