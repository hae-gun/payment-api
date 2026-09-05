package com.hyegeun.payment.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 결제 건. 상태 전이와 금액 불변식을 이 객체가 직접 지킨다.
 *
 * 금액은 원 단위 long 으로 다룬다. 원화는 소수점이 없어 정밀도 손실이 없고,
 * 비교와 누적이 단순하다. 다통화로 확장할 때는 통화별 최소 단위와 함께
 * BigDecimal 로 바꾸는 편이 안전하다.
 */
@Entity
@Table(
        name = "payment",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_idempotency_key", columnNames = "idempotency_key")
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 같은 요청이 두 번 들어와도 결제가 한 번만 생기도록 하는 키 */
    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "canceled_amount", nullable = false)
    private Long canceledAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentCancel> cancels = new ArrayList<>();

    protected Payment() {
    }

    private Payment(String idempotencyKey, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다. amount=" + amount);
        }
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.canceledAmount = 0L;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public static Payment request(String idempotencyKey, long amount) {
        return new Payment(idempotencyKey, amount);
    }

    public void approve() {
        transitionTo(PaymentStatus.APPROVED);
    }

    public void fail() {
        transitionTo(PaymentStatus.FAILED);
    }

    /**
     * 부분 취소를 포함한 취소. 취소 요청마다 이력을 남기고,
     * 누적 취소액이 결제 금액에 도달하면 전액 취소로 전이한다.
     */
    public PaymentCancel cancel(long cancelAmount) {
        if (!status.isCancelable()) {
            throw new InvalidPaymentStateException("취소할 수 없는 상태입니다. status=" + status);
        }
        if (cancelAmount <= 0) {
            throw new IllegalArgumentException("취소 금액은 0보다 커야 합니다. amount=" + cancelAmount);
        }
        if (cancelAmount > cancelableAmount()) {
            throw new IllegalArgumentException(
                    "취소 가능 금액을 초과했습니다. 요청=%d, 가능=%d".formatted(cancelAmount, cancelableAmount()));
        }

        long newCanceledAmount = this.canceledAmount + cancelAmount;
        PaymentStatus nextStatus = (newCanceledAmount == this.amount)
                ? PaymentStatus.CANCELED
                : PaymentStatus.PARTIAL_CANCELED;

        transitionTo(nextStatus);
        this.canceledAmount = newCanceledAmount;

        PaymentCancel cancel = PaymentCancel.of(this, cancelAmount);
        this.cancels.add(cancel);
        return cancel;
    }

    public long cancelableAmount() {
        return amount - canceledAmount;
    }

    private void transitionTo(PaymentStatus next) {
        if (!status.canTransitionTo(next)) {
            throw InvalidPaymentStateException.transition(status, next);
        }
        this.status = next;
    }

    public Long getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public long getAmount() {
        return amount;
    }

    public long getCanceledAmount() {
        return canceledAmount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public List<PaymentCancel> getCancels() {
        return List.copyOf(cancels);
    }
}
