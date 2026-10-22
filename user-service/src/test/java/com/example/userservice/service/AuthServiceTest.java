
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

    // ============================================
    // REGISTER - SUCCESS
    // ============================================

    @Test
    void register_shouldCreateCustomerSuccessfully() {

        String name = "Harsha";
        String email = "harsha@test.com";
        String password = "Test@123";

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName(name);
        savedUser.setEmail(email);

        when(userRepository.existsByEmail(email))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        when(passwordEncoder.encode(password))
                .thenReturn("hashed-password");

        User result = authService.register(
                name,
                email,
                password
        );

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Harsha", result.getName());
        assertEquals("harsha@test.com", result.getEmail());

        verify(userRepository).existsByEmail(email);
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(password);

        verify(authenticationRepository)
                .save(argThat(authentication ->
                        authentication.getUser() == savedUser
                                && "CUSTOMER".equals(
                                authentication.getRole()
                        )
                                && "hashed-password".equals(
                                authentication.getPasswordHash()
                        )
                ));
    }

    // ============================================
    // REGISTER - DUPLICATE EMAIL
    // ============================================

    @Test
    void register_shouldRejectDuplicateEmail() {

        String email = "harsha@test.com";

        when(userRepository.existsByEmail(email))
                .thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> authService.register(
                                "Harsha",
                                email,
                                "Test@123"
                        )
                );

        assertEquals(
                "Email already registered",
                exception.getMessage()
        );

        verify(userRepository, never())
                .save(any(User.class));

        verifyNoInteractions(
                passwordEncoder,
                authenticationRepository
        );
    }

    // ============================================
    // REGISTER - EMPTY NAME
    // ============================================

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

        verifyNoInteractions(
                userRepository,
                authenticationRepository,
                passwordEncoder
        );
    }

    // ============================================
    // REGISTER - EMPTY EMAIL
    // ============================================

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

        verifyNoInteractions(
                userRepository,
                authenticationRepository,
                passwordEncoder
        );
    }

    // ============================================
    // REGISTER - EMPTY PASSWORD
    // ============================================

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

        verifyNoInteractions(
                userRepository,
                authenticationRepository,
                passwordEncoder
        );
    }

    // ============================================
    // REGISTER - TRIM NAME AND EMAIL
    // ============================================

    @Test
    void register_shouldTrimNameAndEmail() {

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName("Harsha");
        savedUser.setEmail("harsha@test.com");

        when(userRepository.existsByEmail("harsha@test.com"))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        when(passwordEncoder.encode("Test@123"))
                .thenReturn("hashed-password");

        User result = authService.register(
                "  Harsha  ",
                "  harsha@test.com  ",
                "Test@123"
        );

        assertEquals("Harsha", result.getName());
        assertEquals("harsha@test.com", result.getEmail());

        verify(userRepository)
                .existsByEmail("harsha@test.com");

        verify(userRepository).save(argThat(user ->
                "Harsha".equals(user.getName())
                        && "harsha@test.com".equals(user.getEmail())
        ));

        verify(authenticationRepository)
                .save(any(Authentication.class));
    }

    // ============================================
    // LOGIN - VALID CREDENTIALS
    // ============================================

    @Test
    void login_shouldReturnTokenForValidCredentials() {

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

        // JWT now uses database user ID
        when(jwtUtil.generateToken(
                1L,
                "CUSTOMER"
        )).thenReturn("test-jwt-token");

        AuthResponse result =
                authService.login(email, password);

        assertNotNull(result);
        assertEquals(
                "test-jwt-token",
                result.getToken()
        );
        assertEquals(
                "CUSTOMER",
                result.getRole()
        );

        verify(userRepository)
                .findByEmail(email);

        verify(authenticationRepository)
                .findByUserId(1L);

        verify(passwordEncoder)
                .matches(password, "hashed-password");

        verify(jwtUtil)
                .generateToken(1L, "CUSTOMER");
    }

    // ============================================
    // LOGIN - UNKNOWN EMAIL
    // ============================================

    @Test
    void login_shouldRejectUnknownEmail() {

        String email = "unknown@test.com";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

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

        verifyNoInteractions(
                passwordEncoder,
                jwtUtil
        );
    }

    // ============================================
    // LOGIN - WRONG PASSWORD
    // ============================================

    @Test
    void login_shouldRejectWrongPassword() {

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
                .generateToken(anyLong(), anyString());
    }

    // ============================================
    // LOGIN - EMPTY EMAIL
    // ============================================

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

        verifyNoInteractions(
                userRepository,
                authenticationRepository,
                passwordEncoder,
                jwtUtil
        );
    }

    // ============================================
    // LOGIN - EMPTY PASSWORD
    // ============================================

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

        verifyNoInteractions(
                userRepository,
                authenticationRepository,
                passwordEncoder,
                jwtUtil
        );
    }
}
