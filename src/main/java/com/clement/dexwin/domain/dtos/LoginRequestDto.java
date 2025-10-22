package com.clement.dexwin.domain.dtos;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
    @NotBlank
    String email,
    @NotBlank
    String password

) {
}