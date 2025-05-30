package com.example.ElasticCommerce.domain.user.service;


import com.example.ElasticCommerce.domain.user.dto.response.RespondentUserDto;
import com.example.ElasticCommerce.domain.user.dto.response.UserResponseDTO;
import com.example.ElasticCommerce.domain.user.dto.service.JoinServiceDTO;
import com.example.ElasticCommerce.domain.user.dto.service.UpdateUserServiceDTO;
import com.example.ElasticCommerce.domain.user.entity.User;
import com.example.ElasticCommerce.domain.user.exception.UserExceptionType;
import com.example.ElasticCommerce.domain.user.repository.UserRepository;
import com.example.ElasticCommerce.global.exception.type.BadRequestException;
import com.example.ElasticCommerce.global.exception.type.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Transactional
    public void joinUser(JoinServiceDTO joinServiceDTO) {

        checkUsernameValid(joinServiceDTO.getUsername());


        User user = User.builder()
                        .username(joinServiceDTO.getUsername())
                        .password((bCryptPasswordEncoder.encode(joinServiceDTO.getPassword())))
                        .role("USER")
                        .email(joinServiceDTO.getEmail())
                        .build();

        userRepository.save(user);
    }

    private void checkUsernameValid(String userName) {
        if (userName == null || userName.isEmpty()) {
            throw new BadRequestException(UserExceptionType.INVALID_NICKNAME);
        }

        if (userRepository.existsByUsername(userName)) {
            throw new BadRequestException(UserExceptionType.DUPLICATED_NICKNAME);
        }
    }

    public UserResponseDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException(UserExceptionType.NOT_FOUND_USER));

        UserResponseDTO userResponseDTO = UserResponseDTO.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPassword())
                .role(user.getRole())
                .birthDay((user.getBirthDay()))
                .build();

        return userResponseDTO;
    }

    @Transactional
    public void updateUser(UpdateUserServiceDTO updateUserServiceDTO, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->new NotFoundException(UserExceptionType.NOT_FOUND_USER));

        user.setEmail(updateUserServiceDTO.getBio());

        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException(UserExceptionType.NOT_FOUND_USER));

        userRepository.delete(user);
    }

    public RespondentUserDto getUser(Long userId) {
        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> new NotFoundException(UserExceptionType.NOT_FOUND_USER));

        return new RespondentUserDto(userId, user.getUsername(), user.getRole());
    }

    public List<RespondentUserDto> getRespondentUsersByIds(List<Long> userIds) {
        List<User> users = userRepository.findAllById(userIds);
        return users.stream()
                    .map(RespondentUserDto::fromEntity)
                    .toList();
    }
}
