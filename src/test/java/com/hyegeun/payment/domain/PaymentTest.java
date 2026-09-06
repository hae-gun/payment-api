package com.hyegeun.payment.domain;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    private static final String KEY = "test-key";

    @Nested
    @DisplayName("결제 요청")
    class Request {

        @Test
        @DisplayName("요청한 결제는 PENDING 으로 시작하고 취소 가능 금액이 전액이다")
        void 요청한_결제는_PENDING_으로_시작한다() {
            Payment payment = reqeustPayment( 10_000L);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.getAmount()).isEqualTo(10_000L);
            assertThat(payment.getCanceledAmount()).isZero();
            assertThat(payment.cancelableAmount()).isEqualTo(10_000L);
        }

        // 0원, 음수
    }

    @Nested
    @DisplayName("상태 전이 - 허용")
    class AllowedTransition {

        @Test
        @DisplayName("PENDING 상태에서 승인하면 APPROVED 가 된다.")
        void 결제_요청_후_승인() {
            Payment payment = reqeustPayment( 10_000L);

            payment.approve();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);

        }

        @Test
        @DisplayName("PENDING 상태에서 실패하면 FAILED 가 된다.")
        void 결제_요청_후_실패(){
            Payment payment = reqeustPayment( 10_000L);

            payment.fail();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("APPROVED 상태에서 전액 취소하면 CANCELD 상태가 된다.")
        void 결제_승인_후_전액_취소(){
            Payment payment = reqeustPayment( 10_000L);

            payment.approve();

            payment.cancel(10_000L);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        }

        @Test
        @DisplayName("APPROVED 상태에서 잔액보다 작은 양을 부분 취소하면 PARTIAL_CANCELED 상태가 된다.")
        void 결제_승인_후_부분_취소(){
            Payment payment = reqeustPayment( 10_000L);

            payment.approve();

            payment.cancel(5_000L);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIAL_CANCELED);
        }

        @Test
        @DisplayName("PARTIAL_CANCELED 상태에서 잔액보다 작은 양을 부분 취소하면 PARTIAL_CANCELED 상태를 유지한다.")
        void 부분_취소_후_잔액_일부_취소(){
            Payment payment = reqeustPayment( 10_000L);

            payment.approve();
            payment.cancel(5_000L);

            payment.cancel(3_000L);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIAL_CANCELED);
        }

        @Test
        @DisplayName("PARTIAL_CANCELED 상태에서 남은 잔액만큼 취소하면 CANCELD 상태가 된다.")
        void 부분_취소_후_잔액_전체_취소(){
            Payment payment = reqeustPayment( 10_000L);

            payment.approve();
            payment.cancel(5_000L);

            long remain = payment.cancelableAmount();

            payment.cancel(remain);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        }

        @Test
        @DisplayName("부분 취소를 두 번 하면 누적 취소액이 합산되고 취소 가능 금액이 줄어든다.")
        void 부분_취소를_두_번_하면_누적된다(){
            Payment payment = reqeustPayment( 10_000L);

            payment.approve();

            payment.cancel(5_000L);
            payment.cancel(3_000L);

            assertThat(payment.getCanceledAmount()).isEqualTo(8_000L);
            assertThat(payment.cancelableAmount()).isEqualTo(2_000L);

        }
    }

    @Nested
    @DisplayName("상태 전이 - 금지")
    class ForbiddenTransition {

    }

    @Nested
    @DisplayName("금액 경계")
    class AmountBoundary {

    }

    @Nested
    @DisplayName("불변식")
    class Invariant {

    }

    private Payment reqeustPayment(long amount){
        return Payment.request(KEY, amount);
    }

}
