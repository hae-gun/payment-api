package com.hyegeun.payment.infra;

public enum PgResultCode {
    /* PG 사 요청 후 거래 승인 완료*/
    APPROVED,
    /* PG 사 요청 후 거래 불가*/
    DECLINE,
    /* PG 사 요청이후 TIME_OUT 발생*/
    UNKNOWN;

    public boolean isTransactionPossible(){
        return switch (this){
            case APPROVED,UNKNOWN -> true;
            case DECLINE -> false;
        };
    }
}
