package UASPraktikum.CarWash.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transaction")
    private Long idTransaction;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_booking", nullable = false)
    private Booking booking;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Column(name = "payment_proof")
    private String paymentProof; // Path to uploaded file (for non-cash payments)

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "cashier_name")
    private String cashierName; // Employee who handled cash payment

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Default constructor
    public Transaction() {}

    // Constructor for cash payments
    public Transaction(Booking booking, BigDecimal amount, String cashierName) {
        this.booking = booking;
        this.amount = amount;
        this.transactionDate = LocalDateTime.now();
        this.paymentMethod = PaymentMethod.CASH;
        this.paymentStatus = PaymentStatus.VALID; // Cash payments are immediately valid
        this.cashierName = cashierName;
        this.verifiedBy = cashierName;
        this.verifiedAt = LocalDateTime.now();
    }

    // Constructor for transfer/online payments
    public Transaction(Booking booking, BigDecimal amount, String paymentProof, PaymentMethod method) {
        this.booking = booking;
        this.amount = amount;
        this.transactionDate = LocalDateTime.now();
        this.paymentMethod = method;
        this.paymentProof = paymentProof;
        this.paymentStatus = PaymentStatus.PENDING; // Online payments need verification
    }

    // Getters and Setters
    public Long getIdTransaction() {
        return idTransaction;
    }

    public void setIdTransaction(Long idTransaction) {
        this.idTransaction = idTransaction;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentProof() {
        return paymentProof;
    }

    public void setPaymentProof(String paymentProof) {
        this.paymentProof = paymentProof;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public String getCashierName() {
        return cashierName;
    }

    public void setCashierName(String cashierName) {
        this.cashierName = cashierName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Helper method to verify payment
    public void verify(String verifiedBy, String notes) {
        this.paymentStatus = PaymentStatus.VALID;
        this.verifiedAt = LocalDateTime.now();
        this.verifiedBy = verifiedBy;
        this.notes = notes;
    }

    // Helper method to reject payment
    public void reject(String verifiedBy, String notes) {
        this.paymentStatus = PaymentStatus.INVALID;
        this.verifiedAt = LocalDateTime.now();
        this.verifiedBy = verifiedBy;
        this.notes = notes;
    }

    // Helper method to get formatted transaction ID
    public String getFormattedTransactionId() {
        return String.format("TXN-%04d-%05d", 
            createdAt != null ? createdAt.getYear() : LocalDateTime.now().getYear(), 
            idTransaction != null ? idTransaction : 0);
    }

    // Helper method to check if payment is cash
    public boolean isCashPayment() {
        return this.paymentMethod == PaymentMethod.CASH;
    }

    // Helper method to check if payment needs verification
    public boolean needsVerification() {
        return this.paymentStatus == PaymentStatus.PENDING;
    }
}
