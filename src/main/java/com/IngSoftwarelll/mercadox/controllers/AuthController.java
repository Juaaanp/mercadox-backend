package com.IngSoftwarelll.mercadox.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.IngSoftwarelll.mercadox.dtos.auth.LoginRequest;
import com.IngSoftwarelll.mercadox.dtos.auth.LoginResponse;
import com.IngSoftwarelll.mercadox.models.enums.UserRole;
import com.IngSoftwarelll.mercadox.security.CustomUserDetails;
import com.IngSoftwarelll.mercadox.security.JwtTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthenticationManager authManager;
    private final JwtTokenService jwtTokenService;

    /**
     * Login: Autentica al usuario y genera un par de tokens (access + refresh)
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Validated @RequestBody LoginRequest request) {

        log.info("Login attempt for user: {}", request.getEmail());

        // Autenticación con Spring Security
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        String accessToken = jwtTokenService.generateAccessToken(authentication);

        String role = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .findFirst()
            .orElseThrow();

        log.info("Login successful for user: {}", authentication.getName());

        // Respuesta limpia: solo access token + scope (nunca refresh token)
        return ResponseEntity.ok(new LoginResponse(accessToken, UserRole.valueOf(role)));
    }

    @GetMapping("/test")
    public ResponseEntity<String> test(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok("Id del usuario autenticado: " + user.getId());
    }
}
