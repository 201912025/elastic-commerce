package com.example.ElasticCommerce.domain.review.exception;

import com.example.ElasticCommerce.global.exception.ExceptionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReviewExceptionType implements ExceptionType {

    REVIEW_NOT_FOUND          (3001, "리뷰를 찾을 수 없습니다."),
    INVALID_RATING            (3002, "평점은 1 이상 5 이하의 값이어야 합니다."),
    REVIEW_CREATION_FAILED    (3003, "리뷰 등록에 실패했습니다."),
    REVIEW_UPDATE_FAILED      (3004, "리뷰 수정에 실패했습니다."),
    REVIEW_DELETE_FAILED      (3005, "리뷰 삭제에 실패했습니다.");

    private final int statusCode;
    private final String message;
}
