package com.hyegeun.payment.domain;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.*;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    private static final String KEY = "test-key";

    @Nested
    @DisplayName("결제 요청")
    class Request {

        @Test
        @DisplayName("요청한 결제는 PENDING 으로 시작하고 취소 가능 금액이 전액이다")
        void 요청한_결제는_PENDING_으로_시작한다() {
            Payment payment = reqeustPayment(10_000L);

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
            Payment payment = reqeustPayment(10_000L);

            payment.approve();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);

        }

        @Test
        @DisplayName("PENDING 상태에서 실패하면 FAILED 가 된다.")
        void 결제_요청_후_실패() {
            Payment payment = reqeustPayment(10_000L);

            payment.fail();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @DisplayName("취소 금액은 1원 이상이어야 한다.")
        @ParameterizedTest
        @ValueSource(longs = {1L, 10L, 100L, 1_000L})
        void 실퍠_금액_1원_이상(long amount){
            Payment payment = reqeustPayment(10_000L);

            payment.approve();

            payment.cancel(amount);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIAL_CANCELED);
        }

        @Test
        @DisplayName("APPROVED 상태에서 전액 취소하면 CANCELED 상태가 된다.")
        void 결제_승인_후_전액_취소() {
            Payment payment = reqeustPayment(10_000L);

            payment.approve();

            payment.cancel(10_000L);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        }

        @Test
        @DisplayName("APPROVED 상태에서 잔액보다 작은 양을 부분 취소하면 PARTIAL_CANCELED 상태가 된다.")
        void 결제_승인_후_부분_취소() {
            Payment payment = reqeustPayment(10_000L);

            payment.approve();

            payment.cancel(5_000L);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIAL_CANCELED);
        }

        @Test
        @DisplayName("PARTIAL_CANCELED 상태에서 잔액보다 작은 양을 부분 취소하면 PARTIAL_CANCELED 상태를 유지한다.")
        void 부분_취소_후_잔액_일부_취소() {
            Payment payment = reqeustPayment(10_000L);

            payment.approve();
            payment.cancel(5_000L);

            payment.cancel(3_000L);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIAL_CANCELED);
        }

        @Test
        @DisplayName("PARTIAL_CANCELED 상태에서 남은 잔액만큼 취소하면 CANCELED 상태가 된다.")
        void 부분_취소_후_잔액_전체_취소() {
            Payment payment = reqeustPayment(10_000L);

            payment.approve();
            payment.cancel(5_000L);

            long remain = payment.cancelableAmount();

            payment.cancel(remain);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        }

        @Test
        @DisplayName("부분 취소를 두 번 하면 누적 취소액이 합산되고 취소 가능 금액이 줄어든다.")
        void 부분_취소를_두_번_하면_누적된다() {
            Payment payment = reqeustPayment(10_000L);

            payment.approve();

            payment.cancel(5_000L);
            payment.cancel(3_000L);

            assertThat(payment.getCanceledAmount()).isEqualTo(8_000L);
            assertThat(payment.cancelableAmount()).isEqualTo(2_000L);

        }

        @Test
        @DisplayName("전액 취소하면 취소 가능 금액이 0이 된다.")
        void 전액_취소_상태(){
            Payment payment = reqeustPayment(10_000L);

            payment.approve();
            payment.cancel(payment.cancelableAmount());

            assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.CANCELED);
            assertThat(payment.cancelableAmount())
                    .isZero();
            assertThat(payment.getCanceledAmount())
                    .isEqualTo(10_000L);

        }

        @Test
        @DisplayName("취소에 실패해도 누적 취소액과 상태는 그대로 유지된다.")
        void 취소_실패_후_상태_유지() {
            Payment payment = reqeustPayment(10_000L);
            payment.approve();

            payment.cancel(5_000L);

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> payment.cancel(payment.cancelableAmount() + 1))
                    .withMessageContaining("취소 가능 금액을 초과했습니다");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIAL_CANCELED);
            assertThat(payment.getCanceledAmount()).isEqualTo(5_000L);
        }

        @Test
        @DisplayName("부분 취소 두 번이면 이력이 두 건 쌓이고 금액이 순서대로 기록된다.")
        void 부분_취소_이력_확인(){
            Payment payment = reqeustPayment(10_000L);
            payment.approve();

            payment.cancel(2_000L);
            payment.cancel(3_000L);

            List<PaymentCancel> cancels = payment.getCancels();

            assertThat(cancels.size()).isEqualTo(2);
            assertThat(cancels)
                    .extracting(PaymentCancel::getAmount)
                    .containsExactly(2_000L, 3_000L);
        }
    }

    @Nested
    @DisplayName("상태 전이 - 금지")
    class ForbiddenTransition {

        @ParameterizedTest()
        @DisplayName("결제금액이 0원 이하이면 예외가 발생한다.")
        @ValueSource(longs = {0L, -1L, -100L})
        void 결제_금액_0_이하_불가(long amount) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Payment.request(KEY, amount))
                    .withMessageContaining("결제 금액은 0보다 커야 합니다");
        }

        @Test
        @DisplayName("PENDING 상태에서 취소하면 예외가 발생한다. ")
        void PENDDING_상태_취소_불가() {
            Payment payment = reqeustPayment(10_000L);

            assertThatExceptionOfType(InvalidPaymentStateException.class)
                    .isThrownBy(() -> payment.cancel(5_000L))
                    .withMessageContaining("취소할 수 없는 상태입니다");

        }

        @ParameterizedTest()
        @DisplayName("취소금액이 0원 이하이면 예외가 발생한다.")
        @ValueSource(longs = {0L, -1L, -100L})
        void 취소_금액_0_이하_금지(long cancelAmount) {
            Payment payment = reqeustPayment(10_000L);
            payment.approve();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> payment.cancel(cancelAmount))
                    .withMessageContaining("취소 금액은 0보다 커야 합니다");
        }

        @Test
        @DisplayName("부분 취소의 누적금액은 승인 금액을 넘을 수 없다.")
        void 부분취소_누적금액_초과() {
            Payment payment = reqeustPayment(10_000L);
            payment.approve();

            long cancelableAmount = payment.cancelableAmount();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> payment.cancel(cancelableAmount + 1))
                    .withMessageContaining("취소 가능 금액을 초과했습니다");
        }

        @Test
        @DisplayName("CANCELED 상태에서는 추가 취소할 수 없다.")
        void 취소_상태_추가_취소_불가(){
            Payment payment = reqeustPayment(10_000L);
            payment.approve();
            payment.cancel(payment.cancelableAmount());

            assertThatExceptionOfType(InvalidPaymentStateException.class)
                    .isThrownBy(() -> payment.cancel( 1))
                    .withMessageContaining("취소할 수 없는 상태입니다");
        }

        @Test
        @DisplayName("부분 취소 상태에서 APPROVED 상태로 변경 할 수 없다.")
        void 부분_취소_상태_후_승인_변경_불가(){
            Payment payment = reqeustPayment(10_000L);
            payment.approve();
            payment.cancel(payment.cancelableAmount()/2);

            assertThatExceptionOfType(InvalidPaymentStateException.class)
                    .isThrownBy(() -> payment.approve())
                    .withMessageContaining("허용되지 않은 상태 전이입니다");
        }

        @Test
        @DisplayName("전액 취소 상태에서 APPROVED 상태로 변경 할 수 없다.")
        void 취소_상태_후_승인_변경_불가(){
            Payment payment = reqeustPayment(10_000L);
            payment.approve();
            payment.cancel(payment.cancelableAmount());

            assertThatExceptionOfType(InvalidPaymentStateException.class)
                    .isThrownBy(() -> payment.approve())
                    .withMessageContaining("허용되지 않은 상태 전이입니다");
        }

        @Test
        @DisplayName("이미 승인된 결제를 다시 승인할 수 없다.")
        void 승인_중복_불가(){
            Payment payment = reqeustPayment(10_000L);

            payment.approve();

            assertThatExceptionOfType(InvalidPaymentStateException.class)
                    .isThrownBy(() -> payment.approve())
                    .withMessageContaining("허용되지 않은 상태 전이입니다");

        }

        @Test
        @DisplayName("승인된 상태는 실패 처리 할 수 없다.")
        void 승인_후_실패_불가(){
            Payment payment = reqeustPayment(10_000L);

            payment.approve();

            assertThatExceptionOfType(InvalidPaymentStateException.class)
                    .isThrownBy(() -> payment.fail())
                    .withMessageContaining("허용되지 않은 상태 전이입니다");

        }

        @Test
        @DisplayName("FAILED 상태에서는 승인상태로 변경 할 수 없다.")
        void 실패_후_승인_불가(){
            Payment payment = reqeustPayment(10_000L);
            payment.fail();

            assertThatExceptionOfType(InvalidPaymentStateException.class)
                    .isThrownBy(() -> payment.approve())
                    .withMessageContaining("허용되지 않은 상태 전이입니다");

        }

        @Test
        @DisplayName("FAILED 상태에서는 중복으로 FAILED 상태로 변경 할 수 없다.")
        void 중복_실패_처리_불가(){
            Payment payment = reqeustPayment(10_000L);
            payment.fail();

            assertThatExceptionOfType(InvalidPaymentStateException.class)
                    .isThrownBy(() -> payment.fail())
                    .withMessageContaining("허용되지 않은 상태 전이입니다");
        }

        @Test
        @DisplayName("FAILED 상태에서는 취소 할 수 없다.")
        void 실패_후_취소_처리_불가(){
            Payment payment = reqeustPayment(10_000L);
            payment.fail();

            assertThatExceptionOfType(InvalidPaymentStateException.class)
                    .isThrownBy(() -> payment.cancel(payment.cancelableAmount()))
                    .withMessageContaining("취소할 수 없는 상태입니다");
        }

        @Test
        @DisplayName("취소 이력은 리스트 변경이 불가능하다.")
        void 취소_이력_변경_불가(){
            Payment payment = reqeustPayment(10_000L);
            payment.approve();

            payment.cancel(2_000L);
            payment.cancel(3_000L);

            List<PaymentCancel> cancels = payment.getCancels();

            assertThatExceptionOfType(UnsupportedOperationException.class)
                    .isThrownBy(() -> cancels.add(PaymentCancel.of(payment, 1_000L)));
        }
    }

    @Nested
    @DisplayName("금액 경계")
    class AmountBoundary {

    }

    @Nested
    @DisplayName("불변식")
    class Invariant {

    }

    private Payment reqeustPayment(long amount) {
        return Payment.request(KEY, amount);
    }

}
