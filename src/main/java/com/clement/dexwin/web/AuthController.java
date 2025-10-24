package com.clement.dexwin.web;

import com.clement.dexwin.domain.dtos.users.LoginRequestDto;
import com.clement.dexwin.domain.dtos.users.SignedUpSucessResponse;
import com.clement.dexwin.domain.dtos.users.signinResponse;
import com.clement.dexwin.domain.dtos.users.SignupRequestDto;
import com.clement.dexwin.domain.services.contracts.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for user registration and login")
public class AuthController {
    private final AuthService authService;

    @Operation(
        summary = "User signup",
        description = "Register a new user account.",
        method = "POST"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User successfully registered",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = SignedUpSucessResponse.class),
                examples = {
                    @ExampleObject(name = "success",
                        value = "{\n  \"message\": \"User created successfully\",\n  \"userId\": \"8f14e45f-ea9d-4a8d-9a9a-5b7c6d9e2c11\"\n}")
                }
            )
        ),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignedUpSucessResponse createLogin(
            @Valid
            @RequestBody(description = "Signup payload",
                required = true,
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SignupRequestDto.class),
                    examples = {
                        @ExampleObject(name = "signup",
                            value = "{\n  \"firstName\": \"John\",\n  \"middleName\": \"K\",\n  \"lastName\": \"Doe\",\n  \"email\": \"john.doe@example.com\",\n  \"password\": \"S3cur3P@ss!\"\n}")
                    }
                )
            ) SignupRequestDto request) {
        log.info("signup for email {}", request.email());
        return authService.register(request);
    }

    @Operation(
        summary = "Login user",
        description = "Authenticate a user and return tokens.",
        method = "POST"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User successfully authenticated",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = signinResponse.class),
                examples = {
                    @ExampleObject(name = "loginSuccess",
                        value = "{\n  \"accessToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\",\n  \"refreshToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\",\n  \"expiresIn\": 3600\n}")
                }
            )
        ),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public signinResponse login(
            @Valid
            @RequestBody(description = "Login payload",
                required = true,
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LoginRequestDto.class),
                    examples = {
                        @ExampleObject(name = "login",
                            value = "{\n  \"email\": \"john.doe@example.com\",\n  \"password\": \"S3cur3P@ss!\"\n}")
                    }
                )
            ) LoginRequestDto request) {
        log.info("login for email {}", request.email());
        return authService.login(request);
    }
}
