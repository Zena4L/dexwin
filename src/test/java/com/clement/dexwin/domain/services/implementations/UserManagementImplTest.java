package com.clement.dexwin.domain.services.implementations;

import com.clement.dexwin.domain.models.users.UserListProjection;
import com.clement.dexwin.domain.repository.UserRepository;
import com.clement.dexwin.exceptions.BadRequestException;
import com.clement.dexwin.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.clement.dexwin.utils.ConstantMessages.INVALID_PAGE_NUMBER;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserManagementImpl userManagementService;

    private UUID testUserId;
    private UserListProjection testUserProjection;
    private Pageable defaultPageable;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        testUserProjection = new UserListProjection() {
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
                return "VIEWER";
            }
        };

        defaultPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"));
    }

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should successfully delete user by ID")
        void testDeleteUser_Success() {
            // Arrange
            doNothing().when(userRepository).deleteUserById(testUserId);

            // Act
            assertDoesNotThrow(() -> userManagementService.deleteUser(testUserId));

            // Assert
            verify(userRepository, times(1)).deleteUserById(testUserId);
        }

        @Test
        @DisplayName("Should call deleteUserById even if user does not exist")
        void testDeleteUser_NonExistentUser() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            doNothing().when(userRepository).deleteUserById(nonExistentId);

            // Act
            assertDoesNotThrow(() -> userManagementService.deleteUser(nonExistentId));

            // Assert
            verify(userRepository, times(1)).deleteUserById(nonExistentId);
        }

        @Test
        @DisplayName("Should handle multiple delete operations")
        void testDeleteUser_MultipleDeletes() {
            // Arrange
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();
            UUID userId3 = UUID.randomUUID();

            doNothing().when(userRepository).deleteUserById(any(UUID.class));

            // Act
            userManagementService.deleteUser(userId1);
            userManagementService.deleteUser(userId2);
            userManagementService.deleteUser(userId3);

            // Assert
            verify(userRepository).deleteUserById(userId1);
            verify(userRepository).deleteUserById(userId2);
            verify(userRepository).deleteUserById(userId3);
            verify(userRepository, times(3)).deleteUserById(any(UUID.class));
        }
    }

    @Nested
    @DisplayName("Get All Users Tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should successfully retrieve all users with default pagination")
        void testGetAllUser_Success() {
            // Arrange
            List<UserListProjection> userList = Arrays.asList(
                    testUserProjection,
                    createUserProjection(UUID.randomUUID(), "Jane", "Smith", "ADMIN")
            );
            Page<UserListProjection> expectedPage = new PageImpl<>(userList, defaultPageable, userList.size());

            when(userRepository.findAllUserByProjection(any(Pageable.class), eq("")))
                    .thenReturn(expectedPage);

            // Act
            Page<UserListProjection> result = userManagementService.getAllUser(0, 10, "");

            // Assert
            assertNotNull(result);
            assertEquals(2, result.getContent().size());
            assertEquals(2, result.getTotalElements());
            assertEquals("John", result.getContent().get(0).getFirstName());
            assertEquals("Jane", result.getContent().get(1).getFirstName());

            verify(userRepository).findAllUserByProjection(any(Pageable.class), eq(""));
        }

        @Test
        @DisplayName("Should retrieve users with search query and trim whitespace")
        void testGetAllUser_WithSearchQuery() {
            // Arrange
            String searchQuery = "  John  ";
            List<UserListProjection> userList = Collections.singletonList(testUserProjection);
            Page<UserListProjection> expectedPage = new PageImpl<>(userList, defaultPageable, 1);

            when(userRepository.findAllUserByProjection(any(Pageable.class), eq("John")))
                    .thenReturn(expectedPage);

            // Act
            Page<UserListProjection> result = userManagementService.getAllUser(0, 10, searchQuery);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals("John", result.getContent().get(0).getFirstName());

            verify(userRepository).findAllUserByProjection(any(Pageable.class), eq("John"));
        }

        @Test
        @DisplayName("Should return empty page when no users found")
        void testGetAllUser_EmptyResult() {
            // Arrange
            Page<UserListProjection> emptyPage = new PageImpl<>(Collections.emptyList(), defaultPageable, 0);

            when(userRepository.findAllUserByProjection(any(Pageable.class), eq("")))
                    .thenReturn(emptyPage);

            // Act
            Page<UserListProjection> result = userManagementService.getAllUser(0, 10, "");

            // Assert
            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
            assertEquals(0, result.getTotalElements());

            verify(userRepository).findAllUserByProjection(any(Pageable.class), eq(""));
        }

        @Test
        @DisplayName("Should throw BadRequestException when page number is negative")
        void testGetAllUser_NegativePageNumber() {
            // Act & Assert
            BadRequestException exception = assertThrows(
                    BadRequestException.class,
                    () -> userManagementService.getAllUser(-1, 10, "")
            );

            assertEquals(INVALID_PAGE_NUMBER, exception.getMessage());
            verify(userRepository, never()).findAllUserByProjection(any(Pageable.class), anyString());
        }

        @Test
        @DisplayName("Should throw BadRequestException when page is -5")
        void testGetAllUser_NegativePageNumberVariation() {
            // Act & Assert
            BadRequestException exception = assertThrows(
                    BadRequestException.class,
                    () -> userManagementService.getAllUser(-5, 20, "test")
            );

            assertEquals(INVALID_PAGE_NUMBER, exception.getMessage());
            verify(userRepository, never()).findAllUserByProjection(any(Pageable.class), anyString());
        }

        @Test
        @DisplayName("Should handle different page sizes")
        void testGetAllUser_DifferentPageSizes() {
            // Arrange
            List<UserListProjection> userList = Arrays.asList(
                    testUserProjection,
                    createUserProjection(UUID.randomUUID(), "Jane", "Smith", "ADMIN"),
                    createUserProjection(UUID.randomUUID(), "Bob", "Johnson", "VIEWER")
            );

            Pageable customPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "createdAt"));
            Page<UserListProjection> expectedPage = new PageImpl<>(userList, customPageable, 3);

            when(userRepository.findAllUserByProjection(any(Pageable.class), eq("")))
                    .thenReturn(expectedPage);

            // Act
            Page<UserListProjection> result = userManagementService.getAllUser(0, 5, "");

            // Assert
            assertNotNull(result);
            assertEquals(3, result.getContent().size());
            verify(userRepository).findAllUserByProjection(any(Pageable.class), eq(""));
        }

        @Test
        @DisplayName("Should handle pagination on second page")
        void testGetAllUser_SecondPage() {
            // Arrange
            List<UserListProjection> userList = Collections.singletonList(
                    createUserProjection(UUID.randomUUID(), "Alice", "Williams", "ADMIN")
            );

            Pageable secondPageable = PageRequest.of(1, 10, Sort.by(Sort.Direction.ASC, "createdAt"));
            Page<UserListProjection> expectedPage = new PageImpl<>(userList, secondPageable, 11);

            when(userRepository.findAllUserByProjection(any(Pageable.class), eq("")))
                    .thenReturn(expectedPage);

            // Act
            Page<UserListProjection> result = userManagementService.getAllUser(1, 10, "");

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals("Alice", result.getContent().get(0).getFirstName());
            verify(userRepository).findAllUserByProjection(any(Pageable.class), eq(""));
        }

        @Test
        @DisplayName("Should verify correct sorting is applied")
        void testGetAllUser_VerifySorting() {
            // Arrange
            Page<UserListProjection> expectedPage = new PageImpl<>(Collections.emptyList());
            when(userRepository.findAllUserByProjection(any(Pageable.class), anyString()))
                    .thenReturn(expectedPage);

            // Act
            userManagementService.getAllUser(0, 10, "");

            // Assert
            verify(userRepository).findAllUserByProjection(
                    argThat(pageable ->
                            pageable.getSort().getOrderFor("createdAt") != null &&
                                    pageable.getSort().getOrderFor("createdAt").getDirection() == Sort.Direction.ASC
                    ),
                    anyString()
            );
        }
    }

    @Nested
    @DisplayName("Get User Tests")
    class GetUserTests {

        @Test
        @DisplayName("Should successfully retrieve user by ID")
        void testGetUser_Success() {
            // Arrange
            when(userRepository.findUserById(testUserId))
                    .thenReturn(Optional.of(testUserProjection));

            // Act
            UserListProjection result = userManagementService.getUser(testUserId);

            // Assert
            assertNotNull(result);
            assertEquals(testUserId.toString(), result.getId());
            assertEquals("John", result.getFirstName());
            assertEquals("Doe", result.getLastName());
            assertEquals("VIEWER", result.getRoles());

            verify(userRepository, times(1)).findUserById(testUserId);
        }

        @Test
        @DisplayName("Should throw NotFoundException when user does not exist")
        void testGetUser_NotFound() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(userRepository.findUserById(nonExistentId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            NotFoundException exception = assertThrows(
                    NotFoundException.class,
                    () -> userManagementService.getUser(nonExistentId)
            );

            assertEquals("User not found", exception.getMessage());
            verify(userRepository, times(1)).findUserById(nonExistentId);
        }

        @Test
        @DisplayName("Should retrieve users with different roles")
        void testGetUser_DifferentRoles() {
            // Arrange
            UUID adminUserId = UUID.randomUUID();
            UserListProjection adminUser = createUserProjection(adminUserId, "Admin", "User", "ADMIN");

            when(userRepository.findUserById(adminUserId))
                    .thenReturn(Optional.of(adminUser));

            // Act
            UserListProjection result = userManagementService.getUser(adminUserId);

            // Assert
            assertNotNull(result);
            assertEquals("ADMIN", result.getRoles());
            assertEquals("Admin", result.getFirstName());
        }

        @Test
        @DisplayName("Should handle multiple getUser calls")
        void testGetUser_MultipleCalls() {
            // Arrange
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();

            UserListProjection user1 = createUserProjection(userId1, "User1", "Last1", "VIEWER");
            UserListProjection user2 = createUserProjection(userId2, "User2", "Last2", "ADMIN");

            when(userRepository.findUserById(userId1)).thenReturn(Optional.of(user1));
            when(userRepository.findUserById(userId2)).thenReturn(Optional.of(user2));

            // Act
            UserListProjection result1 = userManagementService.getUser(userId1);
            UserListProjection result2 = userManagementService.getUser(userId2);

            // Assert
            assertNotNull(result1);
            assertNotNull(result2);
            assertEquals("User1", result1.getFirstName());
            assertEquals("User2", result2.getFirstName());
            verify(userRepository).findUserById(userId1);
            verify(userRepository).findUserById(userId2);
        }
    }

    private UserListProjection createUserProjection(UUID id, String firstName, String lastName, String role) {
        return new UserListProjection() {
            @Override
            public String getId() {
                return id.toString();
            }

            @Override
            public String getFirstName() {
                return firstName;
            }

            @Override
            public String getLastName() {
                return lastName;
            }

            @Override
            public String getRoles() {
                return role;
            }
        };
    }
}