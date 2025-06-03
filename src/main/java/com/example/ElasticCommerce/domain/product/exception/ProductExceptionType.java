package com.example.ElasticCommerce.domain.product.exception;

import com.example.ElasticCommerce.global.exception.ExceptionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
@Getter
@AllArgsConstructor
public enum ProductExceptionType implements ExceptionType{

    PRODUCT_NOT_FOUND(2001, "해당 상품을 찾을 수 없습니다."),
    DUPLICATED_PRODUCT_CODE(2002, "중복된 상품 코드입니다."),
    INVALID_PRODUCT_CODE(2003, "유효하지 않은 상품 코드 형식입니다."),
    INVALID_PRICE(2004, "상품 가격은 0 이상이어야 합니다."),
    INVALID_CATEGORY(2005, "유효하지 않은 상품 카테고리입니다."),
    PRODUCT_NOT_AVAILABLE(2006, "현재 판매 가능한 상품이 아닙니다."),
    PRODUCT_CREATION_FAILED(2007, "상품 등록에 실패했습니다."),
    PRODUCT_UPDATE_FAILED(2008, "상품 수정에 실패했습니다."),
    PRODUCT_DELETE_FAILED(2009, "상품 삭제에 실패했습니다."),
    PRICE_UPDATE_FORBIDDEN(2010, "상품 가격은 변경할 수 없습니다."),
    INVALID_STOCK_QUANTITY(2011, "재고수량은 0 이상이어야 합니다.");


    private final int statusCode;
    private final String message;

}
