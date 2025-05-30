package com.example.ElasticCommerce.global.exception.type;


import com.example.ElasticCommerce.global.exception.BaseException;
import com.example.ElasticCommerce.global.exception.ExceptionType;

public class NotFoundException extends BaseException {

    public NotFoundException(ExceptionType exceptionType) {
        super(exceptionType);
    }

}