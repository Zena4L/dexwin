package com.clement.dexwin.domain.dtos;

import com.clement.dexwin.domain.models.Roles;
import lombok.Builder;

@Builder
public record signinResponse(
    String token,
    String firstName,
    String middleName,
    String lastName,
    String email,
    Roles role
) {
}
