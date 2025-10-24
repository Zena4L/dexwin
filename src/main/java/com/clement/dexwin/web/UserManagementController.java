package com.clement.dexwin.web;


import com.clement.dexwin.domain.models.users.UserListProjection;
import com.clement.dexwin.domain.services.contracts.UserManagement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.clement.dexwin.utils.ConstantMessages.DEFAULT_PAGE_NUMBER;
import static com.clement.dexwin.utils.ConstantMessages.DEFAULT_PAGE_SIZE;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/user")
@Slf4j
@Tag(name = "User Management", description = "APIs for managing users")
public class UserManagementController {
    private final UserManagement userManagementService;

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete user", description = "Delete a user by their ID. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User successfully deleted"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public void deleteUser(@PathVariable
                           @Parameter(description = "User ID", example = "8f14e45f-ea9d-4a8d-9a9a-5b7c6d9e2c11") UUID userId) {
        log.info("deleting user with id {}", userId);
        userManagementService.deleteUser(userId);
    }

    @GetMapping
    @Operation(summary = "Get all users", description =
        "Retrieve a paginated list of users with optional search. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved users",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = "{\n  \"content\": [\n    {\n      \"id\": \"8f14e45f-ea9d-4a8d-9a9a-5b7c6d9e2c11\",\n      \"firstName\": \"John\",\n      \"lastName\": \"Doe\",\n      \"email\": \"john.doe@example.com\",\n      \"role\": \"ADMIN\"\n    }\n  ],\n  \"pageable\": { \"pageNumber\": 0, \"pageSize\": 10 },\n  \"totalElements\": 1,\n  \"totalPages\": 1\n}"))
        ),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public Page<UserListProjection> getAllUser(@RequestParam(defaultValue = DEFAULT_PAGE_NUMBER)
                                               @Parameter(description = "Page number", example = "0") int page,
                                               @RequestParam(defaultValue = DEFAULT_PAGE_SIZE)
                                               @Parameter(description = "Page size", example = "10") int size,
                                               @RequestParam(required = false, defaultValue = "")
                                               @Parameter(description = "Search term", example = "john") String search
    ) {
        return userManagementService.getAllUser(page, size, search);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserListProjection.class),
                examples = @ExampleObject(value = "{\n  \"id\": \"8f14e45f-ea9d-4a8d-9a9a-5b7c6d9e2c11\",\n  \"firstName\": \"John\",\n  \"lastName\": \"Doe\",\n  \"email\": \"john.doe@example.com\",\n  \"role\": \"ADMIN\"\n}"))
        ),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public UserListProjection getUser(@PathVariable("userId")
                                      @Parameter(description = "User ID", example = "8f14e45f-ea9d-4a8d-9a9a-5b7c6d9e2c11") UUID request) {
        return userManagementService.getUser(request);
    }
}
