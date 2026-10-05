package com.example.userservice.controller;

import com.example.userservice.dto.AuthResponse;
import com.example.userservice.dto.LoginRequest;
import com.example.userservice.dto.RegisterRequest;
import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository repository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private UserController userController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;


    private MockMvc getMockMvc() {

        if (mockMvc == null) {
            mockMvc = MockMvcBuilders
                    .standaloneSetup(userController)
                    .build();
        }

        return mockMvc;
    }


    // =========================================================
    // GET /users
    // =========================================================

    @Test
    void getAllUsers_shouldReturnUsers() throws Exception {

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

        getMockMvc()
                .perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Harsha"))
                .andExpect(jsonPath("$[0].email")
                        .value("harsha@test.com"))
                .andExpect(jsonPath("$[1].name")
                        .value("Test User"));

        verify(repository)
                .findAll();
    }


    // =========================================================
    // POST /users
    // =========================================================

    @Test
    void register_shouldCreateUser() throws Exception {

        RegisterRequest request = new RegisterRequest();

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

        getMockMvc()
                .perform(
                        post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
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

        verify(authService)
                .register(
                        "Harsha",
                        "harsha@test.com",
                        "Test@123"
                );
    }


    // =========================================================
    // GET /users/{id} - USER FOUND
    // =========================================================

    @Test
    void getUser_shouldReturnUserWhenFound() throws Exception {

        User user = new User();

        user.setId(1L);
        user.setName("Harsha");
        user.setEmail("harsha@test.com");

        when(repository.findById(1L))
                .thenReturn(Optional.of(user));

        getMockMvc()
                .perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Harsha"))
                .andExpect(jsonPath("$.email")
                        .value("harsha@test.com"));

        verify(repository)
                .findById(1L);
    }


    // =========================================================
    // GET /users/{id} - USER NOT FOUND
    // =========================================================

    @Test
    void getUser_shouldReturn404WhenUserNotFound()
            throws Exception {

        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        getMockMvc()
                .perform(get("/users/999"))
                .andExpect(status().isNotFound());

        verify(repository)
                .findById(999L);
    }


    // =========================================================
    // POST /users/login
    // =========================================================

    @Test
    void login_shouldReturnToken() throws Exception {

        LoginRequest request = new LoginRequest();

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

        getMockMvc()
                .perform(
                        post("/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
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

        verify(authService)
                .login(
                        "harsha@test.com",
                        "Test@123"
                );
    }
}