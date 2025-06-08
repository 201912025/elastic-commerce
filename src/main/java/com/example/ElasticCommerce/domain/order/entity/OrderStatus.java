package com.example.ElasticCommerce.domain.order.entity;

public enum OrderStatus {
    /** 주문 생성됨(대기 상태) */
    CREATED,
    /** 결제 완료됨 */
    PAID,
    /** 주문 취소됨 */
    CANCELLED,
    /** 상품이 발송됨 */
    SHIPPED,
    /** 상품이 배송 완료됨 */
    DELIVERED;
}
