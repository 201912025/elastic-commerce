package com.example.ElasticCommerce.domain.user.dto.service;

import lombok.Builder;
import lombok.Getter;

@Getter
public class JoinServiceDTO {

    private String username;
    private String password;
    private String email;
    private String role;
    private String birthDay;
    private String phoneNumber;

    @Builder
    public JoinServiceDTO(String username, String password, String email, String role, String birthDay, String phoneNumber) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
        this.birthDay = birthDay;
        this.phoneNumber = phoneNumber;
    }
}
