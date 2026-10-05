package com.example.userservice.service;

import com.example.userservice.dto.AuthResponse;
import com.example.userservice.entity.Authentication;
import com.example.userservice.entity.User;
import com.example.userservice.repository.AuthenticationRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationRepository authenticationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;


    // =========================================================
    // REGISTER TESTS
    // =========================================================

    @Test
    void register_shouldCreateCustomerSuccessfully() {

        // Arrange
        String name = "Harsha";
        String email = "harsha@test.com";
        String password = "Test@123";

        User savedUser = new User();
        savedUser.setName(name);
        savedUser.setEmail(email);

        when(userRepository.existsByEmail(email))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        when(passwordEncoder.encode(password))
                .thenReturn("hashed-password");

        // Act
        User result = authService.register(
                name,
                email,
                password
        );

        // Assert
        assertNotNull(result);
        assertEquals("Harsha", result.getName());
        assertEquals("harsha@test.com", result.getEmail());

        verify(userRepository)
                .existsByEmail(email);

        verify(userRepository)
                .save(any(User.class));

        verify(passwordEncoder)
                .encode(password);

        verify(authenticationRepository)
                .save(any(Authentication.class));
    }


    @Test
    void register_shouldRejectDuplicateEmail() {

        // Arrange
        String name = "Harsha";
        String email = "harsha@test.com";
        String password = "Test@123";

        when(userRepository.existsByEmail(email))
                .thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> authService.register(
                                name,
                                email,
                                password
                        )
                );

        assertEquals(
                "Email already registered",
                exception.getMessage()
        );

        verify(userRepository, never())
                .save(any(User.class));

        verify(passwordEncoder, never())
                .encode(anyString());

        verify(authenticationRepository, never())
                .save(any(Authentication.class));
    }


    @Test
    void register_shouldRejectEmptyName() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> authService.register(
                                "",
                                "harsha@test.com",
                                "Test@123"
                        )
                );

        assertEquals(
                "Name cannot be empty",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(authenticationRepository);
    }


    @Test
    void register_shouldRejectEmptyEmail() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> authService.register(
                                "Harsha",
                                "",
                                "Test@123"
                        )
                );

        assertEquals(
                "Email cannot be empty",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(authenticationRepository);
    }


    @Test
    void register_shouldRejectEmptyPassword() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> authService.register(
                                "Harsha",
                                "harsha@test.com",
                                ""
                        )
                );

        assertEquals(
                "Password cannot be empty",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(authenticationRepository);
    }


    @Test
    void register_shouldTrimNameAndEmail() {

        // Arrange
        User savedUser = new User();
        savedUser.setName("Harsha");
        savedUser.setEmail("harsha@test.com");

        when(userRepository.existsByEmail("harsha@test.com"))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        when(passwordEncoder.encode("Test@123"))
                .thenReturn("hashed-password");

        // Act
        User result = authService.register(
                "  Harsha  ",
                "  harsha@test.com  ",
                "Test@123"
        );

        // Assert
        assertEquals("Harsha", result.getName());
        assertEquals("harsha@test.com", result.getEmail());

        verify(userRepository)
                .existsByEmail("harsha@test.com");
    }


    // =========================================================
    // LOGIN TESTS
    // =========================================================

    @Test
    void login_shouldReturnTokenForValidCredentials() {

        // Arrange
        String email = "harsha@test.com";
        String password = "Test@123";

        User user = new User();
        user.setId(1L);
        user.setName("Harsha");
        user.setEmail(email);

        Authentication authentication =
                new Authentication();

        authentication.setUser(user);
        authentication.setPasswordHash("hashed-password");
        authentication.setRole("CUSTOMER");

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(authenticationRepository.findByUserId(1L))
                .thenReturn(Optional.of(authentication));

        when(passwordEncoder.matches(
                password,
                "hashed-password"
        )).thenReturn(true);

        when(jwtUtil.generateToken(
                email,
                "CUSTOMER"
        )).thenReturn("test-jwt-token");

        // Act
        AuthResponse result =
                authService.login(
                        email,
                        password
                );

        // Assert
        assertNotNull(result);
        assertEquals("test-jwt-token", result.getToken());
        assertEquals("CUSTOMER", result.getRole());

        verify(userRepository)
                .findByEmail(email);

        verify(authenticationRepository)
                .findByUserId(1L);

        verify(passwordEncoder)
                .matches(password, "hashed-password");

        verify(jwtUtil)
                .generateToken(email, "CUSTOMER");
    }


    @Test
    void login_shouldRejectUnknownEmail() {

        // Arrange
        String email = "unknown@test.com";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> authService.login(
                                email,
                                "Test@123"
                        )
                );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(authenticationRepository, never())
                .findByUserId(anyLong());

        verify(passwordEncoder, never())
                .matches(anyString(), anyString());

        verify(jwtUtil, never())
                .generateToken(anyString(), anyString());
    }


    @Test
    void login_shouldRejectWrongPassword() {

        // Arrange
        String email = "harsha@test.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        Authentication authentication =
                new Authentication();

        authentication.setUser(user);
        authentication.setPasswordHash("hashed-password");
        authentication.setRole("CUSTOMER");

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(authenticationRepository.findByUserId(1L))
                .thenReturn(Optional.of(authentication));

        when(passwordEncoder.matches(
                "WrongPassword",
                "hashed-password"
        )).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> authService.login(
                                email,
                                "WrongPassword"
                        )
                );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(jwtUtil, never())
                .generateToken(anyString(), anyString());
    }


    @Test
    void login_shouldRejectEmptyEmail() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> authService.login(
                                "",
                                "Test@123"
                        )
                );

        assertEquals(
                "Email cannot be empty",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(authenticationRepository);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtUtil);
    }


    @Test
    void login_shouldRejectEmptyPassword() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> authService.login(
                                "harsha@test.com",
                                ""
                        )
                );

        assertEquals(
                "Password cannot be empty",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(authenticationRepository);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtUtil);
    }
}