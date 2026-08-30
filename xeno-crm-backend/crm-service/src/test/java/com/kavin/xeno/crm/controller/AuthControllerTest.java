package com.kavin.xeno.crm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kavin.xeno.crm.entity.User;
import com.kavin.xeno.crm.repository.UserRepository;
import com.kavin.xeno.crm.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthController authController;

    @Test
    public void testRegisterUser_Success() {
        Map<String, String> request = new HashMap<>();
        request.put("username", "testuser");
        request.put("password", "password123");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        ResponseEntity<?> response = authController.registerUser(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void testRegisterUser_DuplicateUsername() {
        Map<String, String> request = new HashMap<>();
        request.put("username", "existinguser");
        request.put("password", "password123");

        when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(new User()));

        ResponseEntity<?> response = authController.registerUser(request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    public void testRegisterUser_ValidationFailure() {
        Map<String, String> request = new HashMap<>();
        request.put("username", "");
        request.put("password", "password123");

        ResponseEntity<?> response = authController.registerUser(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testAuthenticateUser_Success() {
        Map<String, String> request = new HashMap<>();
        request.put("username", "testuser");
        request.put("password", "password123");

        User user = new User(1L, "testuser", "encodedPassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(tokenProvider.generateToken("testuser")).thenReturn("mock-jwt-token");

        ResponseEntity<?> response = authController.authenticateUser(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testAuthenticateUser_InvalidCredentials() {
        Map<String, String> request = new HashMap<>();
        request.put("username", "testuser");
        request.put("password", "wrongpassword");

        User user = new User(1L, "testuser", "encodedPassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        ResponseEntity<?> response = authController.authenticateUser(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
