package com.example.ElasticCommerce.domain.order.exception;

import com.example.ElasticCommerce.global.exception.ExceptionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderExceptionType implements ExceptionType {

    ORDER_NOT_FOUND          (3001, "해당 주문을 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND        (3002, "해당 상품을 찾을 수 없습니다."),
    ORDER_CANNOT_CANCEL      (3003, "주문을 취소할 수 없는 상태입니다."),
    ORDER_CREATION_FAILED    (3004, "주문 생성에 실패했습니다."),
    ORDER_CANCEL_FAILED      (3005, "주문 취소에 실패했습니다.");

    private final int statusCode;
    private final String message;
}
