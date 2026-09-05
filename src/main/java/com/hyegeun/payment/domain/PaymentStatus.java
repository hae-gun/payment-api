package com.hyegeun.payment.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * 결제 상태와 허용된 전이를 함께 정의한다.
 * 전이 규칙을 상태 자신이 알고 있으므로, 서비스 계층에 if 문이 흩어지지 않는다.
 */
public enum PaymentStatus {

    /** 결제 요청을 받아 승인을 기다리는 상태 */
    PENDING,
    /** 승인 완료. 취소와 부분 취소가 가능하다 */
    APPROVED,
    /** 전액 취소됨. 더 이상 전이할 수 없다 */
    CANCELED,
    /** 일부만 취소됨. 남은 금액에 대해 추가 취소가 가능하다 */
    PARTIAL_CANCELED,
    /** 승인 실패. 더 이상 전이할 수 없다 */
    FAILED;

    private static final Set<PaymentStatus> FROM_PENDING = EnumSet.of(APPROVED, FAILED);
    private static final Set<PaymentStatus> FROM_APPROVED = EnumSet.of(CANCELED, PARTIAL_CANCELED);
    private static final Set<PaymentStatus> FROM_PARTIAL = EnumSet.of(CANCELED, PARTIAL_CANCELED);

    public boolean canTransitionTo(PaymentStatus next) {
        return switch (this) {
            case PENDING -> FROM_PENDING.contains(next);
            case APPROVED -> FROM_APPROVED.contains(next);
            case PARTIAL_CANCELED -> FROM_PARTIAL.contains(next);
            case CANCELED, FAILED -> false;
        };
    }

    public boolean isCancelable() {
        return this == APPROVED || this == PARTIAL_CANCELED;
    }
}
