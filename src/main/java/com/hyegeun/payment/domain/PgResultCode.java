package com.hyegeun.payment.domain;

public enum PgResultCode {
    /* PG 사 요청 후 거래 승인 완료*/
    APPROVED,
    /* PG 사 요청 후 거래 불가*/
    DECLINE,
    /* PG 사 요청이후 TIME_OUT 발생*/
    UNKNOWN;

    public boolean isTransactionPossible(){
        return switch (this){
            case APPROVED -> true;
            case DECLINE,UNKNOWN -> false;
        };
    }
}
