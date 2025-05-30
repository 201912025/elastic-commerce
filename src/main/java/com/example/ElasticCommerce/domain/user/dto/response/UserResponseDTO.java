package com.example.ElasticCommerce.domain.user.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
public class UserResponseDTO {

    private String username;

    private String email;

    private String password;

    private String role;

    private String gender;

    private String birthDay;

    private String bio;

    private String teamName;

    @Builder
    public UserResponseDTO(String username, String email, String password, String role, String gender, String birthDay, String bio, String teamName) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.gender = gender;
        this.birthDay = birthDay;
        this.bio = bio;
        this.teamName = teamName;
    }
}
