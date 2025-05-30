package com.example.ElasticCommerce.global.exception.type;


import com.example.ElasticCommerce.global.exception.BaseException;
import com.example.ElasticCommerce.global.exception.ExceptionType;

public class TokenException extends BaseException {

    public TokenException(ExceptionType exceptionType){
        super(exceptionType);
    }

}
