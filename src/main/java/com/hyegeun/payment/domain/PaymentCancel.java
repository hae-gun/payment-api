package com.hyegeun.payment.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 취소 이력. 부분 취소가 여러 번 일어날 수 있으므로 건별로 남긴다.
 * Payment 는 누적 취소액만 들고 있고, 상세 내역은 이 테이블에서 확인한다.
 */
@Entity
@Table(name = "payment_cancel")
public class PaymentCancel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "canceled_at", nullable = false)
    private LocalDateTime canceledAt;

    protected PaymentCancel() {
    }

    static PaymentCancel of(Payment payment, long amount) {
        PaymentCancel cancel = new PaymentCancel();
        cancel.payment = payment;
        cancel.amount = amount;
        cancel.canceledAt = LocalDateTime.now();
        return cancel;
    }

    public Long getId() {
        return id;
    }

    public long getAmount() {
        return amount;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }
}
