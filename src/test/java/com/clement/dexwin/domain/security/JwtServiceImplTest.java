package com.clement.dexwin.domain.security;

import com.clement.dexwin.domain.models.Roles;
import com.clement.dexwin.domain.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceImplTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @InjectMocks
    private JwtServiceImpl jwtService;

    private User testUser;
    private Jwt mockJwt;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .pk(1)
                .id(UUID.randomUUID())
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .username("johndoe")
                .password("password123")
                .roles(Roles.ADMIN)
                .isActive(true)
                .isDeleted(false)
                .build();

        // Create mock JWT with proper builder
        mockJwt = Jwt.withTokenValue("mock.jwt.token")
                .header("alg", "RS256")
                .claim("scope", "ADMIN")
                .claim("sub", "test@example.com")
                .claim("iss", "self")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    @DisplayName("Should generate token successfully for user with ADMIN role")
    void shouldGenerateToken_ForAdminUser() {
        // Arrange
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        // Act
        String token = jwtService.generateToken(testUser);

        // Assert
        assertNotNull(token);
        assertEquals("mock.jwt.token", token);
        verify(jwtEncoder, times(1)).encode(any(JwtEncoderParameters.class));
    }

    @Test
    @DisplayName("Should generate token successfully for user with VIEWER role")
    void shouldGenerateToken_ForViewerUser() {
        // Arrange
        testUser.setRoles(Roles.VIEWER);
        Jwt viewerJwt = Jwt.withTokenValue("viewer.jwt.token")
                .header("alg", "RS256")
                .claim("scope", "VIEWER")
                .claim("sub", "test@example.com")
                .claim("iss", "self")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(viewerJwt);

        // Act
        String token = jwtService.generateToken(testUser);

        // Assert
        assertNotNull(token);
        assertEquals("viewer.jwt.token", token);
        verify(jwtEncoder, times(1)).encode(any(JwtEncoderParameters.class));
    }

    @Test
    @DisplayName("Should use email as subject in token")
    void shouldUseEmailAsSubject() {
        // Arrange
        String expectedEmail = "user@test.com";
        testUser.setEmail(expectedEmail);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        // Act
        jwtService.generateToken(testUser);

        // Assert
        verify(jwtEncoder).encode(argThat(params -> {
            var claims = params.getClaims();
            return claims.getSubject().equals(expectedEmail);
        }));
    }

    @Test
    @DisplayName("Should include role as scope in token claims")
    void shouldIncludeRoleAsScope() {
        // Arrange
        testUser.setRoles(Roles.ADMIN);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        // Act
        jwtService.generateToken(testUser);

        // Assert
        verify(jwtEncoder).encode(argThat(params -> {
            var claims = params.getClaims();
            return claims.getClaim("scope").equals("ADMIN");
        }));
    }


    @Test
    @DisplayName("Should set token expiry to 1 hour from now")
    void shouldSetTokenExpiryTo1Hour() {
        // Arrange
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);
        Instant beforeGeneration = Instant.now();

        // Act
        jwtService.generateToken(testUser);

        // Assert
        verify(jwtEncoder).encode(argThat(params -> {
            var claims = params.getClaims();
            Instant expiresAt = claims.getExpiresAt();
            Instant issuedAt = claims.getIssuedAt();

            assertNotNull(expiresAt);
            assertNotNull(issuedAt);

            // Verify expiry is approximately 1 hour from issuance
            long durationSeconds = expiresAt.getEpochSecond() - issuedAt.getEpochSecond();
            return durationSeconds == 3600; // Exactly 1 hour
        }));
    }

    @Test
    @DisplayName("Should set issuedAt to current time")
    void shouldSetIssuedAtToCurrentTime() {
        // Arrange
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);
        Instant beforeGeneration = Instant.now().minusSeconds(1);

        // Act
        jwtService.generateToken(testUser);

        Instant afterGeneration = Instant.now().plusSeconds(1);

        // Assert
        verify(jwtEncoder).encode(argThat(params -> {
            var claims = params.getClaims();
            Instant issuedAt = claims.getIssuedAt();

            assertNotNull(issuedAt);
            // Verify issuedAt is between before and after generation
            return issuedAt.isAfter(beforeGeneration) && issuedAt.isBefore(afterGeneration);
        }));
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void shouldGenerateDifferentTokensForDifferentUsers() {
        // Arrange
        User user1 = User.builder()
                .email("user1@test.com")
                .roles(Roles.ADMIN)
                .build();

        User user2 = User.builder()
                .email("user2@test.com")
                .roles(Roles.VIEWER)
                .build();

        Jwt jwt1 = Jwt.withTokenValue("token1")
                .header("alg", "RS256")
                .claim("scope", "ADMIN")
                .claim("sub", "user1@test.com")
                .claim("iss", "self")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        Jwt jwt2 = Jwt.withTokenValue("token2")
                .header("alg", "RS256")
                .claim("scope", "VIEWER")
                .claim("sub", "user2@test.com")
                .claim("iss", "self")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(jwt1)
                .thenReturn(jwt2);

        // Act
        String token1 = jwtService.generateToken(user1);
        String token2 = jwtService.generateToken(user2);

        // Assert
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        verify(jwtEncoder, times(2)).encode(any(JwtEncoderParameters.class));
    }

    @Test
    @DisplayName("Should handle user with all required fields")
    void shouldHandleUserWithAllRequiredFields() {
        // Arrange
        User completeUser = User.builder()
                .pk(100)
                .id(UUID.randomUUID())
                .email("complete@test.com")
                .firstName("Jane")
                .middleName("Marie")
                .lastName("Smith")
                .username("janesmith")
                .password("securepass")
                .roles(Roles.VIEWER)
                .isActive(true)
                .isDeleted(false)
                .build();

        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        // Act
        String token = jwtService.generateToken(completeUser);

        // Assert
        assertNotNull(token);
        verify(jwtEncoder, times(1)).encode(any(JwtEncoderParameters.class));
    }

    @Test
    @DisplayName("Should correctly extract role name from enum")
    void shouldCorrectlyExtractRoleNameFromEnum() {
        // Arrange
        testUser.setRoles(Roles.VIEWER);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        // Act
        jwtService.generateToken(testUser);

        // Assert
        verify(jwtEncoder).encode(argThat(params -> {
            var claims = params.getClaims();
            String scope = (String) claims.getClaim("scope");
            return "VIEWER".equals(scope);
        }));
    }

    @Test
    @DisplayName("Should call encoder exactly once per token generation")
    void shouldCallEncoderExactlyOnce() {
        // Arrange
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        // Act
        jwtService.generateToken(testUser);

        // Assert
        verify(jwtEncoder, times(1)).encode(any(JwtEncoderParameters.class));
        verifyNoMoreInteractions(jwtEncoder);
    }

    @Test
    @DisplayName("Should return token value from encoded JWT")
    void shouldReturnTokenValueFromEncodedJwt() {
        // Arrange
        String expectedTokenValue = "expected.token.value";
        Jwt customJwt = Jwt.withTokenValue(expectedTokenValue)
                .header("alg", "RS256")
                .claim("scope", "ADMIN")
                .claim("sub", "test@example.com")
                .claim("iss", "self")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(customJwt);

        // Act
        String actualToken = jwtService.generateToken(testUser);

        // Assert
        assertEquals(expectedTokenValue, actualToken);
    }

    @Test
    @DisplayName("Should generate token for user with minimal required fields")
    void shouldGenerateTokenForMinimalUser() {
        // Arrange
        User minimalUser = User.builder()
                .email("minimal@test.com")
                .roles(Roles.ADMIN)
                .build();

        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        // Act
        String token = jwtService.generateToken(minimalUser);

        // Assert
        assertNotNull(token);
        verify(jwtEncoder).encode(argThat(params -> {
            var claims = params.getClaims();
            return "minimal@test.com".equals(claims.getSubject())
                    && "ADMIN".equals(claims.getClaim("scope"));
        }));
    }
}