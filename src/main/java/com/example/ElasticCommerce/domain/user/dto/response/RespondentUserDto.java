package com.example.ElasticCommerce.domain.user.dto.response;


import com.example.ElasticCommerce.domain.user.entity.User;

public record RespondentUserDto(
        Long userId,
        String username,
        String userRole
) {
    public static RespondentUserDto fromEntity(User user) {
        return new RespondentUserDto(
                user.getUserId(),
                user.getUsername(),
                user.getRole()
        );
    }
}
