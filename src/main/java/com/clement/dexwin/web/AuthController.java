package com.clement.dexwin.web;

import com.clement.dexwin.domain.dtos.SignedUpSucessResponse;
import com.clement.dexwin.domain.dtos.SignupRequestDto;
import com.clement.dexwin.domain.services.contracts.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
@Slf4j
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignedUpSucessResponse createLogin(@Valid @RequestBody SignupRequestDto request) {
        return authService.register(request);
    }
}
