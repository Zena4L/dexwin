package com.clement.dexwin.web;

import com.clement.dexwin.domain.models.users.UserListProjection;
import com.clement.dexwin.domain.services.contracts.UserManagement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserManagement userManagementService;

    private UUID testUserId;
    private UserListProjection mockUser;
    private List<UserListProjection> mockUserList;
    private Page<UserListProjection> mockUserPage;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        // Create mock user projection
        mockUser = new UserListProjection() {
            @Override
            public String getId() {
                return testUserId.toString();
            }

            @Override
            public String getFirstName() {
                return "John";
            }

            @Override
            public String getLastName() {
                return "Doe";
            }

            @Override
            public String getRoles() {
                return "ADMIN";
            }
        };

        UserListProjection mockUser2 = new UserListProjection() {
            @Override
            public String getId() {
                return UUID.randomUUID().toString();
            }

            @Override
            public String getFirstName() {
                return "Jane";
            }

            @Override
            public String getLastName() {
                return "Smith";
            }

            @Override
            public String getRoles() {
                return "VIEWER";
            }
        };

        mockUserList = Arrays.asList(mockUser, mockUser2);
        mockUserPage = new PageImpl<>(mockUserList, PageRequest.of(0, 10), mockUserList.size());
    }

    // ============== DELETE USER ENDPOINT TESTS ==============

    @Test
    @DisplayName("DELETE /api/v1/user/{userId} - Should successfully delete user")
    void shouldSuccessfullyDeleteUser() throws Exception {
        // Arrange
        doNothing().when(userManagementService).deleteUser(testUserId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/user/{userId}", testUserId))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(userManagementService, times(1)).deleteUser(testUserId);
    }

    @Test
    @DisplayName("DELETE /api/v1/user/{userId} - Should handle valid UUID format")
    void shouldHandleValidUuidFormat() throws Exception {
        // Arrange
        UUID validUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        doNothing().when(userManagementService).deleteUser(validUuid);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/user/{userId}", validUuid))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(userManagementService, times(1)).deleteUser(validUuid);
    }

    @Test
    @DisplayName("DELETE /api/v1/user/{userId} - Should return 400 for invalid UUID format")
    void shouldReturnBadRequestForInvalidUuidFormat() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/user/{userId}", "invalid-uuid"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(userManagementService, never()).deleteUser(any(UUID.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/user/{userId} - Should call service with correct userId")
    void shouldCallServiceWithCorrectUserId() throws Exception {
        // Arrange
        UUID specificUserId = UUID.randomUUID();
        doNothing().when(userManagementService).deleteUser(specificUserId);

        // Act
        mockMvc.perform(delete("/api/v1/user/{userId}", specificUserId))
                .andExpect(status().isNoContent());

        // Assert
        verify(userManagementService).deleteUser(eq(specificUserId));
    }

    // ============== GET ALL USERS ENDPOINT TESTS ==============

    @Test
    @DisplayName("GET /api/v1/user - Should successfully retrieve all users with default pagination")
    void shouldSuccessfullyRetrieveAllUsersWithDefaultPagination() throws Exception {
        // Arrange
        when(userManagementService.getAllUser(0, 10, "")).thenReturn(mockUserPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/user"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].firstName").value("John"))
                .andExpect(jsonPath("$.content[0].lastName").value("Doe"))
                .andExpect(jsonPath("$.content[0].roles").value("ADMIN"))
                .andExpect(jsonPath("$.content[1].firstName").value("Jane"))
                .andExpect(jsonPath("$.content[1].lastName").value("Smith"))
                .andExpect(jsonPath("$.content[1].roles").value("VIEWER"));

        verify(userManagementService, times(1)).getAllUser(0, 10, "");
    }

    @Test
    @DisplayName("GET /api/v1/user - Should retrieve users with custom page number")
    void shouldRetrieveUsersWithCustomPageNumber() throws Exception {
        // Arrange
        Page<UserListProjection> page2 = new PageImpl<>(Collections.emptyList(), PageRequest.of(2, 10), 0);
        when(userManagementService.getAllUser(2, 10, "")).thenReturn(page2);

        // Act & Assert
        mockMvc.perform(get("/api/v1/user")
                        .param("page", "2"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userManagementService, times(1)).getAllUser(2, 10, "");
    }

    @Test
    @DisplayName("GET /api/v1/user - Should retrieve users with custom page size")
    void shouldRetrieveUsersWithCustomPageSize() throws Exception {
        // Arrange
        Page<UserListProjection> customSizePage = new PageImpl<>(mockUserList, PageRequest.of(0, 20), mockUserList.size());
        when(userManagementService.getAllUser(0, 20, "")).thenReturn(customSizePage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/user")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userManagementService, times(1)).getAllUser(0, 20, "");
    }

    @Test
    @DisplayName("GET /api/v1/user - Should retrieve users with custom page and size")
    void shouldRetrieveUsersWithCustomPageAndSize() throws Exception {
        // Arrange
        Page<UserListProjection> customPage = new PageImpl<>(mockUserList, PageRequest.of(3, 5), mockUserList.size());
        when(userManagementService.getAllUser(3, 5, "")).thenReturn(customPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/user")
                        .param("page", "3")
                        .param("size", "5"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userManagementService, times(1)).getAllUser(3, 5, "");
    }

    @Test
    @DisplayName("GET /api/v1/user - Should retrieve users with search term")
    void shouldRetrieveUsersWithSearchTerm() throws Exception {
        // Arrange
        String searchTerm = "John";
        when(userManagementService.getAllUser(0, 10, searchTerm)).thenReturn(mockUserPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/user")
                        .param("search", searchTerm))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userManagementService, times(1)).getAllUser(0, 10, searchTerm);
    }

    @Test
    @DisplayName("GET /api/v1/user - Should handle empty search term")
    void shouldHandleEmptySearchTerm() throws Exception {
        // Arrange
        when(userManagementService.getAllUser(0, 10, "")).thenReturn(mockUserPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/user")
                        .param("search", ""))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userManagementService, times(1)).getAllUser(0, 10, "");
    }

    @Test
    @DisplayName("GET /api/v1/user - Should handle search with special characters")
    void shouldHandleSearchWithSpecialCharacters() throws Exception {
        // Arrange
        String searchTerm = "john@example.com";
        when(userManagementService.getAllUser(0, 10, searchTerm)).thenReturn(mockUserPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/user")
                        .param("search", searchTerm))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userManagementService, times(1)).getAllUser(0, 10, searchTerm);
    }

    @Test
    @DisplayName("GET /api/v1/user - Should return empty page when no users found")
    void shouldReturnEmptyPageWhenNoUsersFound() throws Exception {
        // Arrange
        Page<UserListProjection> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(userManagementService.getAllUser(0, 10, "")).thenReturn(emptyPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/user"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        verify(userManagementService, times(1)).getAllUser(0, 10, "");
    }

    @Test
    @DisplayName("GET /api/v1/user - Should handle all parameters together")
    void shouldHandleAllParametersTogether() throws Exception {
        // Arrange
        Page<UserListProjection> customPage = new PageImpl<>(mockUserList, PageRequest.of(1, 15), mockUserList.size());
        when(userManagementService.getAllUser(1, 15, "test")).thenReturn(customPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/user")
                        .param("page", "1")
                        .param("size", "15")
                        .param("search", "test"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userManagementService, times(1)).getAllUser(1, 15, "test");
    }

    @Test
    @DisplayName("GET /api/v1/user - Should use default page 0 when page parameter is not provided")
    void shouldUseDefaultPageWhenNotProvided() throws Exception {
        // Arrange
        when(userManagementService.getAllUser(0, 10, "")).thenReturn(mockUserPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/user"))
                .andExpect(status().isOk());

        verify(userManagementService).getAllUser(eq(0), anyInt(), anyString());
    }

    @Test
    @DisplayName("GET /api/v1/user - Should use default size 10 when size parameter is not provided")
    void shouldUseDefaultSizeWhenNotProvided() throws Exception {
        // Arrange
        when(userManagementService.getAllUser(0, 10, "")).thenReturn(mockUserPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/user"))
                .andExpect(status().isOk());

        verify(userManagementService).getAllUser(anyInt(), eq(10), anyString());
    }

    @Test
    @DisplayName("GET /api/v1/user - Should use empty string for search when not provided")
    void shouldUseEmptyStringForSearchWhenNotProvided() throws Exception {
        // Arrange
        when(userManagementService.getAllUser(0, 10, "")).thenReturn(mockUserPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/user"))
                .andExpect(status().isOk());

        verify(userManagementService).getAllUser(anyInt(), anyInt(), eq(""));
    }

    // ============== GET USER BY ID ENDPOINT TESTS ==============

    @Test
    @DisplayName("GET /api/v1/user/{userId} - Should successfully retrieve user by ID")
    void shouldSuccessfullyRetrieveUserById() throws Exception {
        // Arrange
        when(userManagementService.getUser(testUserId)).thenReturn(mockUser);

        // Act & Assert
        mockMvc.perform(get("/api/v1/user/{userId}", testUserId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.roles").value("ADMIN"));

        verify(userManagementService, times(1)).getUser(testUserId);
    }

    @Test
    @DisplayName("GET /api/v1/user/{userId} - Should retrieve user with VIEWER role")
    void shouldRetrieveUserWithViewerRole() throws Exception {
        // Arrange
        UUID viewerUserId = UUID.randomUUID();
        UserListProjection viewerUser = new UserListProjection() {
            @Override
            public String getId() {
                return viewerUserId.toString();
            }

            @Override
            public String getFirstName() {
                return "Viewer";
            }

            @Override
            public String getLastName() {
                return "User";
            }

            @Override
            public String getRoles() {
                return "VIEWER";
            }
        };

        when(userManagementService.getUser(viewerUserId)).thenReturn(viewerUser);

        // Act & Assert
        mockMvc.perform(get("/api/v1/user/{userId}", viewerUserId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").value("VIEWER"));

        verify(userManagementService, times(1)).getUser(viewerUserId);
    }

    @Test
    @DisplayName("GET /api/v1/user/{userId} - Should return 400 for invalid UUID format")
    void shouldReturnBadRequestForInvalidUuidFormatInGetUser() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/user/{userId}", "not-a-valid-uuid"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(userManagementService, never()).getUser(any(UUID.class));
    }

    @Test
    @DisplayName("GET /api/v1/user/{userId} - Should call service with correct userId")
    void shouldCallServiceWithCorrectUserIdForGetUser() throws Exception {
        // Arrange
        UUID specificUserId = UUID.randomUUID();
        when(userManagementService.getUser(specificUserId)).thenReturn(mockUser);

        // Act
        mockMvc.perform(get("/api/v1/user/{userId}", specificUserId))
                .andExpect(status().isOk());

        // Assert
        verify(userManagementService).getUser(eq(specificUserId));
    }

    @Test
    @DisplayName("GET /api/v1/user/{userId} - Should handle different valid UUID formats")
    void shouldHandleDifferentValidUuidFormats() throws Exception {
        // Arrange
        UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID uuid2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        when(userManagementService.getUser(uuid1)).thenReturn(mockUser);
        when(userManagementService.getUser(uuid2)).thenReturn(mockUser);

        // Act & Assert
        mockMvc.perform(get("/api/v1/user/{userId}", uuid1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/user/{userId}", uuid2))
                .andExpect(status().isOk());

        verify(userManagementService, times(1)).getUser(uuid1);
        verify(userManagementService, times(1)).getUser(uuid2);
    }

    @Test
    @DisplayName("GET /api/v1/user - Should handle large page numbers")
    void shouldHandleLargePageNumbers() throws Exception {
        // Arrange
        Page<UserListProjection> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(1000, 10), 0);
        when(userManagementService.getAllUser(1000, 10, "")).thenReturn(emptyPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/user")
                        .param("page", "1000"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userManagementService, times(1)).getAllUser(1000, 10, "");
    }

    @Test
    @DisplayName("GET /api/v1/user - Should handle large page sizes")
    void shouldHandleLargePageSizes() throws Exception {
        // Arrange
        Page<UserListProjection> largePage = new PageImpl<>(mockUserList, PageRequest.of(0, 100), mockUserList.size());
        when(userManagementService.getAllUser(0, 100, "")).thenReturn(largePage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/user")
                        .param("size", "100"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userManagementService, times(1)).getAllUser(0, 100, "");
    }

    @Test
    @DisplayName("GET /api/v1/user - Should handle search with whitespace")
    void shouldHandleSearchWithWhitespace() throws Exception {
        // Arrange
        String searchWithSpaces = "John Doe";
        when(userManagementService.getAllUser(0, 10, searchWithSpaces)).thenReturn(mockUserPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/user")
                        .param("search", searchWithSpaces))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userManagementService, times(1)).getAllUser(0, 10, searchWithSpaces);
    }

    @Test
    @DisplayName("DELETE /api/v1/user/{userId} - Should return no content without body")
    void shouldReturnNoContentWithoutBody() throws Exception {
        // Arrange
        doNothing().when(userManagementService).deleteUser(testUserId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/user/{userId}", testUserId))
                .andDo(print())
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(userManagementService, times(1)).deleteUser(testUserId);
    }
}

