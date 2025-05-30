package com.example.ElasticCommerce.global.exception.type;


import com.example.ElasticCommerce.global.exception.BaseException;
import com.example.ElasticCommerce.global.exception.ExceptionType;

public class SignInException extends BaseException {

    public Object data;
    public SignInException(ExceptionType exceptionType) {
        super(exceptionType);
    }

    public SignInException(ExceptionType exceptionType, Object data) {
        super(exceptionType);
        this.data = data;
    }

}
