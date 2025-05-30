package com.example.ElasticCommerce.global.exception.type;


import com.example.ElasticCommerce.global.exception.BaseException;
import com.example.ElasticCommerce.global.exception.ExceptionType;

public class ForbiddenException extends BaseException {

    public ForbiddenException(ExceptionType exceptionType) {
        super(exceptionType);
    }

}
