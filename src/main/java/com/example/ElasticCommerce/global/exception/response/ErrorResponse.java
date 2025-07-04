package com.example.ElasticCommerce.global.exception.response;

public record ErrorResponse(
        String fieldName,
        String message
) {

    @Override
    public String toString() {
        return "{" +
                "fieldName='" + fieldName + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

}
