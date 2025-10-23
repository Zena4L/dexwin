package com.clement.dexwin.domain.dtos.users;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
    @NotBlank
    String email,
    @NotBlank
    String password

) {
}