package com.example.ElasticCommerce.global.exception.type;


import com.example.ElasticCommerce.global.exception.BaseException;
import com.example.ElasticCommerce.global.exception.ExceptionType;

public class UnAuthorizedException extends BaseException {

    public UnAuthorizedException(ExceptionType exceptionType) {
        super(exceptionType);
    }

}
