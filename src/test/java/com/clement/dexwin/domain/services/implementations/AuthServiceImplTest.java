package com.clement.dexwin.domain.services.implementations;

import com.clement.dexwin.domain.dtos.users.LoginRequestDto;
import com.clement.dexwin.domain.dtos.users.SignedUpSucessResponse;
import com.clement.dexwin.domain.dtos.users.SignupRequestDto;
import com.clement.dexwin.domain.dtos.users.signinResponse;
import com.clement.dexwin.domain.models.users.Roles;
import com.clement.dexwin.domain.models.users.User;
import com.clement.dexwin.domain.repository.UserRepository;
import com.clement.dexwin.domain.security.JwtService;
import com.clement.dexwin.domain.security.SecurityUser;
import com.clement.dexwin.exceptions.DuplicateEmailException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static com.clement.dexwin.utils.ConstantMessages.DUPLICATE_EMAIL_MSG;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private SignupRequestDto signupRequest;
    private LoginRequestDto loginRequest;
    private User testUser;
    private String testToken;

    @BeforeEach
    void setUp() {
        signupRequest = SignupRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .middleName("Michael")
                .email("john.doe@example.com")
                .password("password123")
                .build();

        loginRequest = new LoginRequestDto("john.doe@example.com", "password123");

        testUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .middleName("Michael")
                .email("john.doe@example.com")
                .password("encodedPassword")
                .roles(Roles.VIEWER)
                .isActive(true)
                .build();

        testToken = "test.jwt.token";
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should successfully register a new user with all fields")
        void testRegister_Success_WithAllFields() {
            // Arrange
            when(userRepository.findByEmail(signupRequest.email())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(signupRequest.password())).thenReturn("encodedPassword");
            when(jwtService.generateToken(any(User.class))).thenReturn(testToken);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            SignedUpSucessResponse response = authService.register(signupRequest);

            // Assert
            assertNotNull(response);
            assertEquals("John", response.firstName());
            assertEquals("Doe", response.lastName());
            assertEquals("john.doe@example.com", response.email());
            assertEquals(testToken, response.token());

            // Verify interactions
            verify(userRepository).findByEmail(signupRequest.email());
            verify(passwordEncoder).encode(signupRequest.password());
            verify(jwtService).generateToken(any(User.class));
            verify(userRepository).save(any(User.class));

            // Verify saved user properties
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertEquals("John", savedUser.getFirstName());
            assertEquals("Doe", savedUser.getLastName());
            assertEquals("Michael", savedUser.getMiddleName());
            assertEquals("john.doe@example.com", savedUser.getEmail());
            assertEquals(Roles.VIEWER, savedUser.getRoles());
            assertTrue(savedUser.isActive());
            assertEquals("encodedPassword", savedUser.getPassword());
        }

        @Test
        @DisplayName("Should successfully register a new user without middle name")
        void testRegister_Success_WithoutMiddleName() {
            // Arrange
            SignupRequestDto requestWithoutMiddleName = SignupRequestDto.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .middleName(null)
                    .email("jane.smith@example.com")
                    .password("password456")
                    .build();

            when(userRepository.findByEmail(requestWithoutMiddleName.email())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(requestWithoutMiddleName.password())).thenReturn("encodedPassword");
            when(jwtService.generateToken(any(User.class))).thenReturn(testToken);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            SignedUpSucessResponse response = authService.register(requestWithoutMiddleName);

            // Assert
            assertNotNull(response);
            verify(userRepository).save(any(User.class));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertNull(savedUser.getMiddleName());
        }

        @Test
        @DisplayName("Should trim whitespace from user input fields")
        void testRegister_Success_TrimsWhitespace() {
            // Arrange
            SignupRequestDto requestWithSpaces = SignupRequestDto.builder()
                    .firstName("  John  ")
                    .lastName("  Doe  ")
                    .middleName("  Michael  ")
                    .email("  john.doe@example.com  ")
                    .password("password123")
                    .build();

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(jwtService.generateToken(any(User.class))).thenReturn(testToken);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            authService.register(requestWithSpaces);

            // Assert
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertEquals("John", savedUser.getFirstName());
            assertEquals("Doe", savedUser.getLastName());
            assertEquals("Michael", savedUser.getMiddleName());
            assertEquals("john.doe@example.com", savedUser.getEmail());
        }

        @Test
        @DisplayName("Should throw DuplicateEmailException when email already exists in repository")
        void testRegister_ThrowsDuplicateEmailException_WhenEmailExists() {
            // Arrange
            when(userRepository.findByEmail(signupRequest.email())).thenReturn(Optional.of(testUser));

            // Act & Assert
            DuplicateEmailException exception = assertThrows(
                    DuplicateEmailException.class,
                    () -> authService.register(signupRequest)
            );

            assertEquals(DUPLICATE_EMAIL_MSG, exception.getMessage());
            verify(userRepository).findByEmail(signupRequest.email());
            verify(userRepository, never()).save(any(User.class));
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("Should throw DuplicateEmailException when DataIntegrityViolationException occurs")
        void testRegister_ThrowsDuplicateEmailException_OnDataIntegrityViolation() {
            // Arrange
            when(userRepository.findByEmail(signupRequest.email())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(signupRequest.password())).thenReturn("encodedPassword");
            when(jwtService.generateToken(any(User.class))).thenReturn(testToken);
            when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("Duplicate key"));

            // Act & Assert
            DuplicateEmailException exception = assertThrows(
                    DuplicateEmailException.class,
                    () -> authService.register(signupRequest)
            );

            assertEquals(DUPLICATE_EMAIL_MSG, exception.getMessage());
            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should successfully login user with valid credentials")
        void testLogin_Success() {
            // Arrange
            SecurityUser securityUser = new SecurityUser(testUser);
            Authentication authentication = mock(Authentication.class);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(securityUser);
            when(jwtService.generateToken(testUser)).thenReturn(testToken);

            // Act
            signinResponse response = authService.login(loginRequest);

            // Assert
            assertNotNull(response);
            assertEquals("John", response.firstName());
            assertEquals("Doe", response.lastName());
            assertEquals("john.doe@example.com", response.email());
            assertEquals(Roles.VIEWER, response.role());
            assertEquals(testToken, response.token());

            // Verify authentication
            ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor =
                    ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(authCaptor.capture());

            UsernamePasswordAuthenticationToken capturedAuth = authCaptor.getValue();
            assertEquals("john.doe@example.com", capturedAuth.getPrincipal());
            assertEquals("password123", capturedAuth.getCredentials());

            verify(jwtService).generateToken(testUser);
        }

        @Test
        @DisplayName("Should throw AuthenticationCredentialsNotFoundException when user is not active")
        void testLogin_ThrowsException_WhenUserNotActive() {
            // Arrange
            User inactiveUser = User.builder()
                    .id(UUID.randomUUID())
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .password("encodedPassword")
                    .roles(Roles.VIEWER)
                    .isActive(false)
                    .build();

            SecurityUser securityUser = new SecurityUser(inactiveUser);
            Authentication authentication = mock(Authentication.class);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(securityUser);

            // Act & Assert
            AuthenticationCredentialsNotFoundException exception = assertThrows(
                    AuthenticationCredentialsNotFoundException.class,
                    () -> authService.login(loginRequest)
            );

            assertEquals("User is not verified", exception.getMessage());
            verify(jwtService, never()).generateToken(any(User.class));
        }

        @Test
        @DisplayName("Should login user with different roles")
        void testLogin_Success_WithDifferentRoles() {
            // Arrange
            User adminUser = User.builder()
                    .id(UUID.randomUUID())
                    .firstName("Admin")
                    .lastName("User")
                    .email("admin@example.com")
                    .password("encodedPassword")
                    .roles(Roles.ADMIN)
                    .isActive(true)
                    .build();

            SecurityUser securityUser = new SecurityUser(adminUser);
            Authentication authentication = mock(Authentication.class);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(securityUser);
            when(jwtService.generateToken(adminUser)).thenReturn(testToken);

            LoginRequestDto adminLoginRequest = new LoginRequestDto("admin@example.com", "adminPassword");

            // Act
            signinResponse response = authService.login(adminLoginRequest);

            // Assert
            assertNotNull(response);
            assertEquals(Roles.ADMIN, response.role());
        }
    }

    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should successfully clear security context on logout")
        void testLogout_Success() {
            // Arrange
            SecurityUser securityUser = new SecurityUser(testUser);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    securityUser, null, securityUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Verify context is set
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());

            // Act
            authService.logout();

            // Assert
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Should handle logout when context is already empty")
        void testLogout_WhenContextAlreadyEmpty() {
            // Arrange
            SecurityContextHolder.clearContext();
            assertNull(SecurityContextHolder.getContext().getAuthentication());

            // Act
            assertDoesNotThrow(() -> authService.logout());

            // Assert
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should use both userRepository instances correctly")
        void testRegister_UsesBothRepositoryInstances() {
            // This tests that both repository and userRepository are used
            when(userRepository.findByEmail(signupRequest.email())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(signupRequest.password())).thenReturn("encodedPassword");
            when(jwtService.generateToken(any(User.class))).thenReturn(testToken);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            authService.register(signupRequest);

            // Both repository instances should be the same in the service
            verify(userRepository, times(1)).findByEmail(signupRequest.email());
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Should encode password before saving user")
        void testRegister_EncodesPasswordBeforeSaving() {
            when(userRepository.findByEmail(signupRequest.email())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(signupRequest.password())).thenReturn("super-encoded-password");
            when(jwtService.generateToken(any(User.class))).thenReturn(testToken);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            authService.register(signupRequest);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertEquals("super-encoded-password", userCaptor.getValue().getPassword());
        }
    }
}