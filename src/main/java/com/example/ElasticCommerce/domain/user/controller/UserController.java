package com.example.ElasticCommerce.domain.user.controller;

import com.example.ElasticCommerce.domain.user.dto.controller.JoinControllerDTO;
import com.example.ElasticCommerce.domain.user.dto.controller.UpdateUserControllerDTO;
import com.example.ElasticCommerce.domain.user.dto.response.RespondentUserDto;
import com.example.ElasticCommerce.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PostMapping("/join")
    public ResponseEntity<Void> joinUser(@Valid @RequestBody JoinControllerDTO joinControllerDTO) {
        userService.joinUser(joinControllerDTO.toServiceDTO());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public List<RespondentUserDto> getRespondentUsersByIds(@RequestParam List<Long> userIds) {
        List<RespondentUserDto> respondentUserDtos = userService.getRespondentUsersByIds(userIds);
        return respondentUserDtos;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<RespondentUserDto> getUser(@PathVariable Long userId) {
        RespondentUserDto respondentUserDto = userService.getUser(userId);
        return ResponseEntity.ok(respondentUserDto);
    }

    @PostMapping("/update")
    public ResponseEntity<Void> updateUser(@Valid @RequestBody UpdateUserControllerDTO updateUserControllerDTO) {
        String username  = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.updateUser(updateUserControllerDTO.toServiceDTO(), username);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> deleteUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.deleteUser(username);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
