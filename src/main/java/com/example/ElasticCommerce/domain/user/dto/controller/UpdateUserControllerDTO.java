package com.example.ElasticCommerce.domain.user.dto.controller;


import com.example.ElasticCommerce.domain.user.dto.service.UpdateUserServiceDTO;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UpdateUserControllerDTO {

    @Email(message = "유효한 이메일 형식이어야 합니다.")
    private String email;

    @Size(max = 200, message = "자기소개는 최대 200자 까지만 입력할 수 있습니다.")
    private String bio;

    @Builder
    public UpdateUserControllerDTO(String email, String bio) {
        this.email = email;
        this.bio = bio;
    }

    public UpdateUserServiceDTO toServiceDTO() {
        return UpdateUserServiceDTO.builder()
                .email(this.email)
                .bio(this.bio)
                .build();
    }
}
