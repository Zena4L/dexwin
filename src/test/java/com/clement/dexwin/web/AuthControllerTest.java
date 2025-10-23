package com.clement.dexwin.web;

import com.clement.dexwin.domain.dtos.users.LoginRequestDto;
import com.clement.dexwin.domain.dtos.users.SignedUpSucessResponse;
import com.clement.dexwin.domain.dtos.users.SignupRequestDto;
import com.clement.dexwin.domain.dtos.users.signinResponse;
import com.clement.dexwin.domain.models.users.Roles;
import com.clement.dexwin.domain.services.contracts.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    private SignupRequestDto validSignupRequest;
    private LoginRequestDto validLoginRequest;
    private SignedUpSucessResponse signupResponse;
    private signinResponse loginResponse;

    @BeforeEach
    void setUp() {
        validSignupRequest = SignupRequestDto.builder()
                .firstName("John")
                .middleName("Michael")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("SecurePassword123")
                .build();

        validLoginRequest = new LoginRequestDto("john.doe@example.com", "SecurePassword123");

        signupResponse = SignedUpSucessResponse.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .token("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token")
                .build();

        loginResponse = signinResponse.builder()
                .token("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token")
                .firstName("John")
                .middleName("Michael")
                .lastName("Doe")
                .email("john.doe@example.com")
                .role(Roles.VIEWER)
                .build();
    }


    @Test
    @DisplayName("POST /api/v1/signup - Should successfully register a new user with valid data")
    void shouldSuccessfullyRegisterNewUser() throws Exception {
        // Arrange
        when(authService.register(any(SignupRequestDto.class))).thenReturn(signupResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSignupRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.token").exists());

        verify(authService, times(1)).register(any(SignupRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/signup - Should register user without middle name")
    void shouldRegisterUserWithoutMiddleName() throws Exception {
        // Arrange
        SignupRequestDto requestWithoutMiddleName = SignupRequestDto.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .password("SecurePass123")
                .build();

        SignedUpSucessResponse response = SignedUpSucessResponse.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .token("token.value")
                .build();

        when(authService.register(any(SignupRequestDto.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithoutMiddleName)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.email").value("jane.smith@example.com"));

        verify(authService, times(1)).register(any(SignupRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/signup - Should return 400 when first name is blank")
    void shouldReturnBadRequestWhenFirstNameIsBlank() throws Exception {
        // Arrange
        SignupRequestDto invalidRequest = SignupRequestDto.builder()
                .firstName("")
                .lastName("Doe")
                .email("test@example.com")
                .password("SecurePass123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(SignupRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/signup - Should return 400 when first name is null")
    void shouldReturnBadRequestWhenFirstNameIsNull() throws Exception {
        // Arrange
        SignupRequestDto invalidRequest = SignupRequestDto.builder()
                .firstName(null)
                .lastName("Doe")
                .email("test@example.com")
                .password("SecurePass123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(SignupRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/signup - Should return 400 when last name is blank")
    void shouldReturnBadRequestWhenLastNameIsBlank() throws Exception {
        // Arrange
        SignupRequestDto invalidRequest = SignupRequestDto.builder()
                .firstName("John")
                .lastName("")
                .email("test@example.com")
                .password("SecurePass123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(SignupRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/signup - Should return 400 when last name is null")
    void shouldReturnBadRequestWhenLastNameIsNull() throws Exception {
        // Arrange
        SignupRequestDto invalidRequest = SignupRequestDto.builder()
                .firstName("John")
                .lastName(null)
                .email("test@example.com")
                .password("SecurePass123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(SignupRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/signup - Should return 400 when email is invalid")
    void shouldReturnBadRequestWhenEmailIsInvalid() throws Exception {
        // Arrange
        SignupRequestDto invalidRequest = SignupRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("invalid-email")
                .password("SecurePass123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(SignupRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/signup - Should return 400 when email is null")
    void shouldReturnBadRequestWhenEmailIsNull() throws Exception {
        // Arrange
        SignupRequestDto invalidRequest = SignupRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email(null)
                .password("SecurePass123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(SignupRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/signup - Should return 400 when password is blank")
    void shouldReturnBadRequestWhenPasswordIsBlank() throws Exception {
        // Arrange
        SignupRequestDto invalidRequest = SignupRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("test@example.com")
                .password("")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(SignupRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/signup - Should return 400 when password is null")
    void shouldReturnBadRequestWhenPasswordIsNull() throws Exception {
        // Arrange
        SignupRequestDto invalidRequest = SignupRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("test@example.com")
                .password(null)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(SignupRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/signup - Should return 400 when password is too short")
    void shouldReturnBadRequestWhenPasswordIsTooShort() throws Exception {
        // Arrange
        SignupRequestDto invalidRequest = SignupRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("test@example.com")
                .password("Short1")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(SignupRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/signup - Should return 400 when password is too long")
    void shouldReturnBadRequestWhenPasswordIsTooLong() throws Exception {
        // Arrange
        SignupRequestDto invalidRequest = SignupRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("test@example.com")
                .password("ThisPasswordIsWayTooLongAndExceedsTheMaximumAllowedLength123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(SignupRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/signup - Should return 400 when request body is missing")
    void shouldReturnBadRequestWhenSignupRequestBodyIsMissing() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/signup")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(SignupRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/signup - Should return 400 when Content-Type is not JSON")
    void shouldReturnBadRequestWhenContentTypeIsNotJson() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/signup")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("invalid content"))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());

        verify(authService, never()).register(any(SignupRequestDto.class));
    }

    // ============== LOGIN ENDPOINT TESTS ==============

    @Test
    @DisplayName("POST /api/v1/login - Should successfully login user with valid credentials")
    void shouldSuccessfullyLoginUser() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequestDto.class))).thenReturn(loginResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.middleName").value("Michael"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.role").value("VIEWER"));

        verify(authService, times(1)).login(any(LoginRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/login - Should login user with ADMIN role")
    void shouldLoginUserWithAdminRole() throws Exception {
        // Arrange
        signinResponse adminResponse = signinResponse.builder()
                .token("admin.token")
                .firstName("Admin")
                .lastName("User")
                .email("admin@example.com")
                .role(Roles.ADMIN)
                .build();

        when(authService.login(any(LoginRequestDto.class))).thenReturn(adminResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        verify(authService, times(1)).login(any(LoginRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/login - Should return 400 when email is blank")
    void shouldReturnBadRequestWhenLoginEmailIsBlank() throws Exception {
        // Arrange
        LoginRequestDto invalidRequest = new LoginRequestDto("", "password123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/login - Should return 400 when password is blank")
    void shouldReturnBadRequestWhenLoginPasswordIsBlank() throws Exception {
        // Arrange
        LoginRequestDto invalidRequest = new LoginRequestDto("test@example.com", "");

        // Act & Assert
        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/login - Should return 400 when both email and password are blank")
    void shouldReturnBadRequestWhenBothEmailAndPasswordAreBlank() throws Exception {
        // Arrange
        LoginRequestDto invalidRequest = new LoginRequestDto("", "");

        // Act & Assert
        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/login - Should return 400 when request body is missing")
    void shouldReturnBadRequestWhenLoginRequestBodyIsMissing() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/login - Should return user without middle name if not present")
    void shouldReturnUserWithoutMiddleNameIfNotPresent() throws Exception {
        // Arrange
        signinResponse responseWithoutMiddleName = signinResponse.builder()
                .token("token.value")
                .firstName("Jane")
                .middleName(null)
                .lastName("Doe")
                .email("jane@example.com")
                .role(Roles.VIEWER)
                .build();

        when(authService.login(any(LoginRequestDto.class))).thenReturn(responseWithoutMiddleName);

        // Act & Assert
        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("jane@example.com"));

        verify(authService, times(1)).login(any(LoginRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/login - Should accept valid JSON with extra whitespace")
    void shouldAcceptValidJsonWithWhitespace() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequestDto.class))).thenReturn(loginResponse);
        String jsonWithWhitespace = """
                {
                  "email"  :  "test@example.com"  ,
                  "password"  :  "SecurePass123"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithWhitespace))
                .andDo(print())
                .andExpect(status().isOk());

        verify(authService, times(1)).login(any(LoginRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/login - Should return 400 when JSON is malformed")
    void shouldReturnBadRequestWhenJsonIsMalformed() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invalid\": \"json\"}"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/signup - Should accept password with exactly 8 characters")
    void shouldAcceptPasswordWithExactly8Characters() throws Exception {
        // Arrange
        SignupRequestDto requestWith8CharPassword = SignupRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("test@example.com")
                .password("Pass1234")
                .build();

        when(authService.register(any(SignupRequestDto.class))).thenReturn(signupResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWith8CharPassword)))
                .andDo(print())
                .andExpect(status().isCreated());

        verify(authService, times(1)).register(any(SignupRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/signup - Should accept password with exactly 32 characters")
    void shouldAcceptPasswordWithExactly32Characters() throws Exception {
        // Arrange
        SignupRequestDto requestWith32CharPassword = SignupRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("test@example.com")
                .password("12345678901234567890123456789012")
                .build();

        when(authService.register(any(SignupRequestDto.class))).thenReturn(signupResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWith32CharPassword)))
                .andDo(print())
                .andExpect(status().isCreated());

        verify(authService, times(1)).register(any(SignupRequestDto.class));
    }
}

