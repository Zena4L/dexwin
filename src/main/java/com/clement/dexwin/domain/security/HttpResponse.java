package com.clement.dexwin.domain.security;


import lombok.Builder;

@Builder
public record HttpResponse(
    int httpStatusCode,
    String httpStatus,
    String reason,
    String message
) {}