package com.clement.dexwin.domain.dtos;

import lombok.Builder;

@Builder
public record SignedUpSucessResponse(
    String firstName,
    String lastName,
    String email,
    String token
) {
}