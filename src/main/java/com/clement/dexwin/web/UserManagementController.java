package com.clement.dexwin.web;


import com.clement.dexwin.domain.models.users.UserListProjection;
import com.clement.dexwin.domain.services.contracts.UserManagement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    public void deleteUser(@PathVariable @Parameter(description = "User ID") UUID userId) {
        log.info("deleting user with id {}", userId);
        userManagementService.deleteUser(userId);
    }

    @GetMapping
    @Operation(summary = "Get all users", description =
        "Retrieve a paginated list of users with optional search. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved users"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public Page<UserListProjection> getAllUser(@RequestParam(defaultValue = DEFAULT_PAGE_NUMBER)
                                               @Parameter(description = "Page number") int page,
                                               @RequestParam(defaultValue = DEFAULT_PAGE_SIZE)
                                               @Parameter(description = "Page size") int size,
                                               @RequestParam(required = false, defaultValue = "")
                                               @Parameter(description = "Search term") String search
    ) {
        return userManagementService.getAllUser(page, size, search);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public UserListProjection getUser(@PathVariable("userId") @Parameter(description = "User ID") UUID request) {
        return userManagementService.getUser(request);
    }
}
