package io.akatsuki.basic_security.controller;

import io.acmwchsd.web.response.ACMResponseBuilder;
import io.acmwchsd.web.vo.ApiResponse;
import io.akatsuki.basic_security.dto.response.UserResponse;
import io.akatsuki.basic_security.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    private ResponseEntity<ApiResponse<UserResponse>> getUserId(@PathVariable("id") String id) {
        UserResponse result = userService.getId(id);
        return ACMResponseBuilder.ok(result);
    }
}
