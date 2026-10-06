package io.akatsuki.basic_security.controller;

import io.acmwchsd.web.response.ACMResponseBuilder;
import io.acmwchsd.web.vo.ApiResponse;
import io.akatsuki.basic_security.common.enums.AgeType;
import io.akatsuki.basic_security.dto.request.CreateUserRequestDto;
import io.akatsuki.basic_security.dto.response.CreateUserResponseDto;
import io.akatsuki.basic_security.dto.response.UserResponse;
import io.akatsuki.basic_security.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @PostMapping("/create")
    private ResponseEntity<ApiResponse<CreateUserResponseDto>> create(
            @RequestBody CreateUserRequestDto request) {

        CreateUserResponseDto userResult = userService.createUser(request);

        return ACMResponseBuilder.ok(userResult);
    }

    @GetMapping("/getAllUsers")
    private ResponseEntity<ApiResponse<List<CreateUserResponseDto>>> getAllUsers() {
        List<CreateUserResponseDto> result = userService.getAllUsers();

        return ACMResponseBuilder.ok(result);
    }

    @GetMapping("/by-age")
    public ResponseEntity<ApiResponse<List<CreateUserResponseDto>>> getUsersByAgeType(@RequestParam AgeType ageType) {
        List<CreateUserResponseDto> resultAge = userService.getAgeQuery(ageType);
        return ACMResponseBuilder.ok(resultAge);
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<CreateUserResponseDto>>> searchUsers(
            @RequestParam(required = false) AgeType ageType,
            @RequestParam(required = false) String school) {
        return ACMResponseBuilder.ok(userService.getUserByFilters(ageType, school));
    }
}
