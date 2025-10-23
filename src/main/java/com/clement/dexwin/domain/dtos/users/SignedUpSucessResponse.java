package com.clement.dexwin.domain.dtos.users;

import lombok.Builder;

@Builder
public record SignedUpSucessResponse(
    String firstName,
    String lastName,
    String email,
    String token
) {
}