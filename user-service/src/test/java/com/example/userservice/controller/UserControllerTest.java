
package com.example.userservice.controller;

import com.example.userservice.dto.AuthResponse;
import com.example.userservice.dto.LoginRequest;
import com.example.userservice.dto.RegisterRequest;
import com.example.userservice.dto.UpdateProfileRequest;
import com.example.userservice.dto.UserProfileResponse;
import com.example.userservice.entity.User;
import com.example.userservice.exception.GlobalExceptionHandler;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.service.AuthService;
import com.example.userservice.service.UserProfileService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.hamcrest.Matchers.containsString;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository repository;

    @Mock
    private AuthService authService;

    @Mock
    private UserProfileService profileService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @BeforeEach
    void setUp() {

        LocalValidatorFactoryBean validator =
                new LocalValidatorFactoryBean();

        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .setValidator(validator)
                .build();
    }

    // ============================================
    // GET /users
    // ============================================

    @Test
    void getAllUsers_shouldReturnUsers()
            throws Exception {

        User user1 = new User();
        user1.setId(1L);
        user1.setName("Harsha");
        user1.setEmail("harsha@test.com");

        User user2 = new User();
        user2.setId(2L);
        user2.setName("Test User");
        user2.setEmail("test@test.com");

        when(repository.findAll())
                .thenReturn(List.of(user1, user2));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Harsha"))
                .andExpect(jsonPath("$[0].email")
                        .value("harsha@test.com"))
                .andExpect(jsonPath("$[1].name")
                        .value("Test User"));

        verify(repository).findAll();
    }

    // ============================================
    // POST /users - VALID REGISTRATION
    // ============================================

    @Test
    void register_shouldCreateUser()
            throws Exception {

        RegisterRequest request =
                new RegisterRequest();

        request.setName("Harsha");
        request.setEmail("harsha@test.com");
        request.setPassword("Test@123");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName("Harsha");
        savedUser.setEmail("harsha@test.com");

        when(authService.register(
                "Harsha",
                "harsha@test.com",
                "Test@123"
        )).thenReturn(savedUser);

        mockMvc.perform(
                        post("/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Harsha"))
                .andExpect(jsonPath("$.email")
                        .value("harsha@test.com"));

        verify(authService).register(
                "Harsha",
                "harsha@test.com",
                "Test@123"
        );
    }

    // ============================================
    // POST /users - INVALID EMAIL
    // ============================================

    @Test
    void register_shouldRejectInvalidEmail()
            throws Exception {

        RegisterRequest request =
                new RegisterRequest();

        request.setName("Test User");
        request.setEmail("invalid-email");
        request.setPassword("TestPassword123");

        mockMvc.perform(
                        post("/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        containsString(
                                "Please provide a valid email address"
                        )
                ));

        verifyNoInteractions(authService);
    }

    // ============================================
    // POST /users - BLANK EMAIL
    // ============================================

    @Test
    void register_shouldRejectBlankEmail()
            throws Exception {

        RegisterRequest request =
                new RegisterRequest();

        request.setName("Test User");
        request.setEmail("");
        request.setPassword("TestPassword123");

        mockMvc.perform(
                        post("/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // ============================================
    // POST /users - SHORT PASSWORD
    // ============================================

    @Test
    void register_shouldRejectShortPassword()
            throws Exception {

        RegisterRequest request =
                new RegisterRequest();

        request.setName("Test User");
        request.setEmail("harsha.test@example.com");
        request.setPassword("123");

        mockMvc.perform(
                        post("/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        containsString(
                                "Password must contain at least 8 characters"
                        )
                ));

        verifyNoInteractions(authService);
    }

    // ============================================
    // GET /users/{id} - USER FOUND
    // ============================================

    @Test
    void getUser_shouldReturnUserWhenFound()
            throws Exception {

        User user = new User();
        user.setId(1L);
        user.setName("Harsha");
        user.setEmail("harsha@test.com");

        when(repository.findById(1L))
                .thenReturn(Optional.of(user));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Harsha"))
                .andExpect(jsonPath("$.email")
                        .value("harsha@test.com"));

        verify(repository).findById(1L);
    }

    // ============================================
    // GET /users/{id} - USER NOT FOUND
    // ============================================

    @Test
    void getUser_shouldReturn404WhenUserNotFound()
            throws Exception {

        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/users/999"))
                .andExpect(status().isNotFound());

        verify(repository).findById(999L);
    }

    // ============================================
    // POST /users/login
    // ============================================

    @Test
    void login_shouldReturnToken()
            throws Exception {

        LoginRequest request =
                new LoginRequest();

        request.setEmail("harsha@test.com");
        request.setPassword("Test@123");

        AuthResponse authResponse =
                new AuthResponse(
                        "test-jwt-token",
                        "CUSTOMER"
                );

        when(authService.login(
                "harsha@test.com",
                "Test@123"
        )).thenReturn(authResponse);

        mockMvc.perform(
                        post("/users/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token")
                        .value("test-jwt-token"))
                .andExpect(jsonPath("$.role")
                        .value("CUSTOMER"));

        verify(authService).login(
                "harsha@test.com",
                "Test@123"
        );
    }

    // ============================================
    // GET /users/me - GET PROFILE
    // ============================================

    @Test
    void getMyProfile_shouldReturnAuthenticatedUser()
            throws Exception {

        UserProfileResponse response =
                new UserProfileResponse(
                        5L,
                        "Harsha",
                        "harsha@test.com",
                        "CUSTOMER"
                );

        when(profileService.getProfile(5L))
                .thenReturn(response);

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        5L,
                        null,
                        List.of()
                );

        mockMvc.perform(
                        get("/users/me")
                                .principal(authentication)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name")
                        .value("Harsha"))
                .andExpect(jsonPath("$.email")
                        .value("harsha@test.com"))
                .andExpect(jsonPath("$.role")
                        .value("CUSTOMER"));

        verify(profileService).getProfile(5L);
    }

    // ============================================
    // PUT /users/me - UPDATE PROFILE
    // ============================================

    @Test
    void updateMyProfile_shouldUpdateSuccessfully()
            throws Exception {

        UserProfileResponse response =
                new UserProfileResponse(
                        5L,
                        "Harsha Updated",
                        "new@test.com",
                        "CUSTOMER"
                );

        when(profileService.updateProfile(
                eq(5L),
                any(UpdateProfileRequest.class)
        )).thenReturn(response);

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        5L,
                        null,
                        List.of()
                );

        mockMvc.perform(
                        put("/users/me")
                                .principal(authentication)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                      "name": "Harsha Updated",
                                      "email": "new@test.com"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name")
                        .value("Harsha Updated"))
                .andExpect(jsonPath("$.email")
                        .value("new@test.com"))
                .andExpect(jsonPath("$.role")
                        .value("CUSTOMER"));

        verify(profileService).updateProfile(
                eq(5L),
                any(UpdateProfileRequest.class)
        );
    }

    // ============================================
    // PUT /users/me - INVALID EMAIL
    // ============================================

    @Test
    void updateMyProfile_shouldRejectInvalidEmail()
            throws Exception {

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        5L,
                        null,
                        List.of()
                );

        mockMvc.perform(
                        put("/users/me")
                                .principal(authentication)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                      "name": "Harsha",
                                      "email": "invalid-email"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(profileService);
    }

    // ============================================
    // PUT /users/me - BLANK NAME
    // ============================================

    @Test
    void updateMyProfile_shouldRejectBlankName()
            throws Exception {

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        5L,
                        null,
                        List.of()
                );

        mockMvc.perform(
                        put("/users/me")
                                .principal(authentication)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                      "name": "",
                                      "email": "harsha@test.com"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(profileService);
    }
}
