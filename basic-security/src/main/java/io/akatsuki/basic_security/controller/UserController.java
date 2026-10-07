package io.akatsuki.basic_security.controller;

import io.acmwchsd.web.response.ACMResponseBuilder;
import io.acmwchsd.web.vo.ApiResponse;
import io.akatsuki.basic_security.common.enums.GroupAge;
import io.akatsuki.basic_security.dto.request.UserRequest;
import io.akatsuki.basic_security.dto.response.UserResponse;
import io.akatsuki.basic_security.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.createUser(request);
        return ACMResponseBuilder.created(response);
    }

    @GetMapping("/count")
    private Long countUserByAge(
            @RequestParam(required = false) GroupAge query, @RequestParam(required = false) String school) {
        return userService.countUserByGroup(query, school);
    }
}
