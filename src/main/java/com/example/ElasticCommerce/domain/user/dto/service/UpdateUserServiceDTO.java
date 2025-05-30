package com.example.ElasticCommerce.domain.user.dto.service;

import lombok.Builder;
import lombok.Getter;

@Getter
public class UpdateUserServiceDTO {

    private String email;
    private String bio;

    @Builder
    public UpdateUserServiceDTO(String email, String bio) {
        this.email = email;
        this.bio = bio;
    }
}
