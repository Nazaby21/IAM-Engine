package io.akatsuki.basic_security.dto.request;

public record UserRequest(
        String id, String name, String email, String password, String gender, Integer age, String school) {}
