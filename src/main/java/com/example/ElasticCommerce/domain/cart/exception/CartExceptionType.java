package com.example.ElasticCommerce.domain.cart.exception;

import com.example.ElasticCommerce.global.exception.ExceptionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CartExceptionType implements ExceptionType {

    CART_NOT_FOUND        (4001, "장바구니를 찾을 수 없습니다."),
    CART_ITEM_NOT_FOUND   (4002, "장바구니에 담긴 상품을 찾을 수 없습니다."),
    CART_ITEM_DUPLICATE   (4003, "이미 장바구니에 담긴 상품입니다."),
    INVALID_ITEM_QUANTITY (4004, "상품 수량이 유효하지 않습니다.");

    private final int statusCode;
    private final String message;
}
