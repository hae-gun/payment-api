package com.hyegeun.payment.domain;

public class InvalidPaymentStateException extends RuntimeException {

    public InvalidPaymentStateException(String message) {
        super(message);
    }

    public static InvalidPaymentStateException transition(PaymentStatus from, PaymentStatus to) {
        return new InvalidPaymentStateException(
                "허용되지 않은 상태 전이입니다. from=%s, to=%s".formatted(from, to));
    }
}
