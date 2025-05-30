package com.example.ElasticCommerce.domain.user.exception;

import com.example.ElasticCommerce.global.exception.ExceptionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserExceptionType implements ExceptionType {

    INVALID_SIGN_TOKEN(1001, "유효하지 않은 sign token 입니다."),
    NOT_FOUND_GENDER(1002, "유효하지 않은 gender 형식입니다."),
    NOT_FOUND_TEAM(1003, "유효하지 않은 팀 형식입니다."),
    NOT_FOUND_USER(1004, "유저를 찾을 수 없습니다."),
    DUPLICATED_NICKNAME(1005, "중복된 유저 닉네임입니다."),
    PREVIOUS_REGISTERED_USER(1006, "이전에 회원 가입한 내역이 있습니다."),
    INVALID_NICKNAME(1007, "유효하지 않은 닉네임 형식입니다.");

    private final int statusCode;
    private final String message;
}

