package com.example.ElasticCommerce.global.exception.type;


import com.example.ElasticCommerce.global.exception.BaseException;
import com.example.ElasticCommerce.global.exception.ExceptionType;

public class BadRequestException extends BaseException {

    public BadRequestException(ExceptionType exceptionType) {
        super(exceptionType);
    }

}
