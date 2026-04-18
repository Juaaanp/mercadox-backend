package com.IngSoftwarelll.mercadox.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.IngSoftwarelll.mercadox.dtos.auth.forgotpassword.ForgotPasswordRequest;
import com.IngSoftwarelll.mercadox.dtos.auth.forgotpassword.ResetPasswordRequest;
import com.IngSoftwarelll.mercadox.dtos.auth.register.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.IngSoftwarelll.mercadox.models.PasswordResetToken;
import com.IngSoftwarelll.mercadox.models.User;
import com.IngSoftwarelll.mercadox.repositories.PasswordResetTokenRepository;
import com.IngSoftwarelll.mercadox.repositories.UserRepository;
import com.IngSoftwarelll.mercadox.services.interfaces.AuthService;
import com.IngSoftwarelll.mercadox.services.interfaces.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }
        log.info("tipe registrado: {}", request.role());
        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhoneNumber(request.phoneNumber());
        user.setRole(request.role());
        user.setRegisteredDate(ZonedDateTime.now());
        user.setBalance(BigDecimal.ZERO);

        userRepository.save(user);
        log.info("Usuario registrado: {}", request.email());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Siempre responde igual para no revelar si el email existe
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            // Eliminar tokens anteriores del mismo usuario
            tokenRepository.deleteByUser_Id(user.getId());

            String rawToken = UUID.randomUUID().toString();

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(rawToken);
            resetToken.setUser(user);
            resetToken.setExpiry(LocalDateTime.now().plusMinutes(30));
            tokenRepository.save(resetToken);

            emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
            log.info("Reset token generado para: {}", user.getEmail());
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Token inválido o expirado"));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new IllegalArgumentException("El token ha expirado");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        tokenRepository.delete(resetToken); // Token de un solo uso
        log.info("Contraseña actualizada para: {}", user.getEmail());
    }
}