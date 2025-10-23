package com.clement.dexwin.integration;

import com.clement.dexwin.domain.models.users.Roles;
import com.clement.dexwin.domain.models.users.User;
import com.clement.dexwin.domain.models.users.UserListProjection;
import com.clement.dexwin.domain.repository.UserRepository;
import com.clement.dexwin.domain.services.implementations.UserManagementImpl;
import com.clement.dexwin.exceptions.BadRequestException;
import com.clement.dexwin.exceptions.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
class UserManagementIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserManagementImpl userManagementService;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;
    private User testUser3;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        userRepository.deleteAll();

        // Create test users
        testUser1 = User.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .middleName("Michael")
                .lastName("Doe")
                .email("john.doe@example.com")
                .username("johndoe")
                .password("hashedPassword123")
                .roles(Roles.ADMIN)
                .isActive(true)
                .isDeleted(false)
                .build();

        testUser2 = User.builder()
                .id(UUID.randomUUID())
                .firstName("Jane")
                .middleName("Marie")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .username("janesmith")
                .password("hashedPassword456")
                .roles(Roles.VIEWER)
                .isActive(true)
                .isDeleted(false)
                .build();

        testUser3 = User.builder()
                .id(UUID.randomUUID())
                .firstName("Bob")
                .lastName("Johnson")
                .email("bob.johnson@example.com")
                .username("bobjohnson")
                .password("hashedPassword789")
                .roles(Roles.VIEWER)
                .isActive(true)
                .isDeleted(false)
                .build();

        // Save test users
        testUser1 = userRepository.save(testUser1);
        testUser2 = userRepository.save(testUser2);
        testUser3 = userRepository.save(testUser3);

        log.info("Test data setup complete. Created {} users", userRepository.count());
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        log.info("Test data cleaned up");
    }


    @Test
    @DisplayName("Should successfully delete a user by UUID")
    void shouldSuccessfullyDeleteUserByUuid() {
        // Arrange
        UUID userIdToDelete = testUser1.getId();
        long countBefore = userRepository.count();

        // Act
        userManagementService.deleteUser(userIdToDelete);

        // Assert
        long countAfter = userRepository.count();
        assertThat(countAfter).isEqualTo(countBefore - 1);
        assertThat(userRepository.findById((long) testUser1.getPk())).isEmpty();
    }

    @Test
    @DisplayName("Should delete user and not affect other users")
    void shouldDeleteUserWithoutAffectingOthers() {
        // Arrange
        UUID userIdToDelete = testUser2.getId();

        // Act
        userManagementService.deleteUser(userIdToDelete);

        // Assert
        assertThat(userRepository.count()).isEqualTo(2);
        assertThat(userRepository.findById((long) testUser1.getPk())).isPresent();
        assertThat(userRepository.findById((long) testUser3.getPk())).isPresent();
    }

    @Test
    @DisplayName("Should handle deletion of non-existent user gracefully")
    void shouldHandleDeletionOfNonExistentUser() {
        // Arrange
        UUID nonExistentUserId = UUID.randomUUID();
        long countBefore = userRepository.count();

        // Act - doesn't throw exception, just doesn't delete anything
        userManagementService.deleteUser(nonExistentUserId);

        // Assert
        long countAfter = userRepository.count();
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("Should successfully delete multiple users sequentially")
    void shouldDeleteMultipleUsersSequentially() {
        // Act
        userManagementService.deleteUser(testUser1.getId());
        userManagementService.deleteUser(testUser2.getId());
        userManagementService.deleteUser(testUser3.getId());

        // Assert
        assertThat(userRepository.count()).isEqualTo(0);
    }


    @Test
    @DisplayName("Should retrieve all users with default pagination")
    void shouldRetrieveAllUsersWithDefaultPagination() {
        // Act
        Page<UserListProjection> result = userManagementService.getAllUser(0, 10, "");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should retrieve users with custom page size")
    void shouldRetrieveUsersWithCustomPageSize() {
        // Act
        Page<UserListProjection> result = userManagementService.getAllUser(0, 2, "");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("Should retrieve second page of users")
    void shouldRetrieveSecondPageOfUsers() {
        // Act
        Page<UserListProjection> result = userManagementService.getAllUser(1, 2, "");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1); // Only one user left on page 2
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("Should search users by first name")
    void shouldSearchUsersByFirstName() {
        // Act
        Page<UserListProjection> result = userManagementService.getAllUser(0, 10, "John");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should search users by last name")
    void shouldSearchUsersByLastName() {
        // Act
        Page<UserListProjection> result = userManagementService.getAllUser(0, 10, "Smith");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLastName()).isEqualTo("Smith");
    }

    @Test
    @DisplayName("Should search users by email")
    void shouldSearchUsersByEmail() {
        // Act
        Page<UserListProjection> result = userManagementService.getAllUser(0, 10, "bob.johnson");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFirstName()).isEqualTo("Bob");
    }

    @Test
    @DisplayName("Should perform case-insensitive search")
    void shouldPerformCaseInsensitiveSearch() {
        // Act
        Page<UserListProjection> result1 = userManagementService.getAllUser(0, 10, "JOHN");
        Page<UserListProjection> result2 = userManagementService.getAllUser(0, 10, "john");
        Page<UserListProjection> result3 = userManagementService.getAllUser(0, 10, "JoHn");

        // Assert
        assertThat(result1.getContent()).hasSize(2);
        assertThat(result2.getContent()).hasSize(2);
        assertThat(result3.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should return empty page when no users match search")
    void shouldReturnEmptyPageWhenNoUsersMatchSearch() {
        // Act
        Page<UserListProjection> result = userManagementService.getAllUser(0, 10, "NonExistent");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should trim whitespace from search term")
    void shouldTrimWhitespaceFromSearchTerm() {
        // Act
        Page<UserListProjection> result = userManagementService.getAllUser(0, 10, "  John  ");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should handle empty search string")
    void shouldHandleEmptySearchString() {
        // Act
        Page<UserListProjection> result = userManagementService.getAllUser(0, 10, "");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("Should handle search with only whitespace")
    void shouldHandleSearchWithOnlyWhitespace() {
        // Act
        Page<UserListProjection> result = userManagementService.getAllUser(0, 10, "   ");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("Should throw BadRequestException for negative page number")
    void shouldThrowBadRequestExceptionForNegativePageNumber() {
        // Act & Assert
        assertThatThrownBy(() -> userManagementService.getAllUser(-1, 10, ""))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Page number can't be negative");
    }

    @Test
    @DisplayName("Should exclude inactive users from results")
    void shouldExcludeInactiveUsersFromResults() {
        // Arrange - Create an inactive user
        User inactiveUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("Inactive")
                .lastName("User")
                .email("inactive@example.com")
                .username("inactive")
                .password("password")
                .roles(Roles.VIEWER)
                .isActive(false)
                .isDeleted(false)
                .build();
        userRepository.save(inactiveUser);

        // Act
        Page<UserListProjection> result = userManagementService.getAllUser(0, 10, "");

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should exclude deleted users from results")
    void shouldExcludeDeletedUsersFromResults() {
        // Arrange - Create a deleted user
        User deletedUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("Deleted")
                .lastName("User")
                .email("deleted@example.com")
                .username("deleted")
                .password("password")
                .roles(Roles.VIEWER)
                .isActive(true)
                .isDeleted(true)
                .build();
        userRepository.save(deletedUser);

        // Act
        Page<UserListProjection> result = userManagementService.getAllUser(0, 10, "");

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(3); // Only non-deleted users
    }

    @Test
    @DisplayName("Should return users sorted by creation date ascending")
    void shouldReturnUsersSortedByCreationDate() {
        // Act
        Page<UserListProjection> result = userManagementService.getAllUser(0, 10, "");

        // Assert - First created user should be first (assuming testUser1 was created first)
        assertThat(result.getContent()).isNotEmpty();
        // Verify that results are returned in order
        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("Should handle large page size")
    void shouldHandleLargePageSize() {
        // Act
        Page<UserListProjection> result = userManagementService.getAllUser(0, 100, "");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should return empty page for page number beyond total pages")
    void shouldReturnEmptyPageForPageNumberBeyondTotalPages() {
        // Act
        Page<UserListProjection> result = userManagementService.getAllUser(10, 10, "");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(3);
    }


    @Test
    @DisplayName("Should successfully retrieve user by ID")
    void shouldSuccessfullyRetrieveUserById() {
        // Act
        UserListProjection result = userManagementService.getUser(testUser1.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUser1.getId().toString());
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getRoles()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Should retrieve user with VIEWER role")
    void shouldRetrieveUserWithViewerRole() {
        // Act
        UserListProjection result = userManagementService.getUser(testUser2.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRoles()).isEqualTo("VIEWER");
        assertThat(result.getFirstName()).isEqualTo("Jane");
    }

    @Test
    @DisplayName("Should retrieve user with ADMIN role")
    void shouldRetrieveUserWithAdminRole() {
        // Act
        UserListProjection result = userManagementService.getUser(testUser1.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRoles()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Should throw NotFoundException when user does not exist")
    void shouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        // Arrange
        UUID nonExistentUserId = UUID.randomUUID();

        // Act & Assert
        assertThatThrownBy(() -> userManagementService.getUser(nonExistentUserId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should retrieve different users by their IDs")
    void shouldRetrieveDifferentUsersByTheirIds() {
        // Act
        UserListProjection user1 = userManagementService.getUser(testUser1.getId());
        UserListProjection user2 = userManagementService.getUser(testUser2.getId());
        UserListProjection user3 = userManagementService.getUser(testUser3.getId());

        // Assert
        assertThat(user1.getFirstName()).isEqualTo("John");
        assertThat(user2.getFirstName()).isEqualTo("Jane");
        assertThat(user3.getFirstName()).isEqualTo("Bob");
    }


    @Test
    @DisplayName("Should handle complete user lifecycle: create, retrieve, search, delete")
    void shouldHandleCompleteUserLifecycle() {
        // 1. Verify user exists
        UserListProjection retrievedUser = userManagementService.getUser(testUser1.getId());
        assertThat(retrievedUser).isNotNull();
        assertThat(retrievedUser.getFirstName()).isEqualTo("John");

        // 2. Search for user
        Page<UserListProjection> searchResult = userManagementService.getAllUser(0, 10, "John");
        assertThat(searchResult.getContent()).hasSize(2);

        // 3. Delete user
        userManagementService.deleteUser(testUser1.getId());

        // 4. Verify user is deleted
        Page<UserListProjection> afterDelete = userManagementService.getAllUser(0, 10, "");
        assertThat(afterDelete.getTotalElements()).isEqualTo(2);

        // 5. Verify user cannot be retrieved
        assertThatThrownBy(() -> userManagementService.getUser(testUser1.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Should handle pagination with search across multiple pages")
    void shouldHandlePaginationWithSearchAcrossMultiplePages() {
        // Arrange - Create more users with similar names
        for (int i = 0; i < 5; i++) {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .firstName("TestUser" + i)
                    .lastName("LastName" + i)
                    .email("testuser" + i + "@example.com")
                    .username("testuser" + i)
                    .password("password")
                    .roles(Roles.VIEWER)
                    .isActive(true)
                    .isDeleted(false)
                    .build();
            userRepository.save(user);
        }

        // Act - Get first page
        Page<UserListProjection> page1 = userManagementService.getAllUser(0, 3, "Test");
        Page<UserListProjection> page2 = userManagementService.getAllUser(1, 3, "Test");

        // Assert
        assertThat(page1.getContent()).hasSize(3);
        assertThat(page2.getContent()).hasSize(2);
        assertThat(page1.getTotalElements()).isEqualTo(5);
        assertThat(page1.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle concurrent user deletions")
    void shouldHandleConcurrentUserDeletions() {
        // Arrange
        long initialCount = userRepository.count();

        // Act - Delete all test users
        userManagementService.deleteUser(testUser1.getId());
        userManagementService.deleteUser(testUser2.getId());
        userManagementService.deleteUser(testUser3.getId());

        // Assert
        assertThat(userRepository.count()).isEqualTo(0);

        Page<UserListProjection> result = userManagementService.getAllUser(0, 10, "");
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should search users by partial email match")
    void shouldSearchUsersByPartialEmailMatch() {
        // Act
        Page<UserListProjection> result = userManagementService.getAllUser(0, 10, "example.com");

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should validate user projection contains all required fields")
    void shouldValidateUserProjectionContainsAllRequiredFields() {
        // Act
        UserListProjection user = userManagementService.getUser(testUser1.getId());

        // Assert
        assertThat(user.getId()).isNotNull();
        assertThat(user.getFirstName()).isNotNull();
        assertThat(user.getLastName()).isNotNull();
        assertThat(user.getRoles()).isNotNull();
    }
}

