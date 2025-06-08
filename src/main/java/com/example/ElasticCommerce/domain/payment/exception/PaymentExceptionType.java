package com.example.ElasticCommerce.domain.payment.exception;

import com.example.ElasticCommerce.global.exception.ExceptionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentExceptionType implements ExceptionType {
    PAYMENT_NOT_FOUND    (4001, "해당 결제 내역을 찾을 수 없습니다."),
    PAYMENT_NOT_ALLOWED  (4002, "결제할 수 없는 상태입니다."),
    PAYMENT_CREATION_FAILED (4003, "결제 처리에 실패했습니다.");

    private final int statusCode;
    private final String message;
}
