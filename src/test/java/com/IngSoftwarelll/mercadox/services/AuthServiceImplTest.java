package com.IngSoftwarelll.mercadox.services;

import com.IngSoftwarelll.mercadox.dtos.auth.forgotpassword.ForgotPasswordRequest;
import com.IngSoftwarelll.mercadox.dtos.auth.forgotpassword.ResetPasswordRequest;
import com.IngSoftwarelll.mercadox.dtos.auth.register.RegisterRequest;
import com.IngSoftwarelll.mercadox.exceptions.BusinessException;
import com.IngSoftwarelll.mercadox.models.PasswordResetToken;
import com.IngSoftwarelll.mercadox.models.User;
import com.IngSoftwarelll.mercadox.models.enums.UserRole;
import com.IngSoftwarelll.mercadox.repositories.PasswordResetTokenRepository;
import com.IngSoftwarelll.mercadox.repositories.UserRepository;
import com.IngSoftwarelll.mercadox.services.interfaces.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para AuthServiceImpl.
 *
 * Casos cubiertos (según tabla de casos de prueba del proyecto):
 *   CP-05 — Registro exitoso con datos válidos
 *   CP-04 — Registro rechazado si email duplicado
 *           Registro rechazado si teléfono duplicado
 *   CP-06 — forgotPassword siempre responde sin revelar info (email existe)
 *   CP-07 — forgotPassword siempre responde sin revelar info (email NO existe)
 *   CP-08 — resetPassword con token válido actualiza contraseña
 *           resetPassword con token expirado lanza excepción
 *           resetPassword con token inválido lanza excepción
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl — pruebas unitarias")
class AuthServiceImplTest {

    // ─────────────────────────────────────────────
    // Dependencias mockeadas
    // ─────────────────────────────────────────────
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    // ════════════════════════════════════════════════════════════
    // BLOQUE 1 — register()
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("register()")
    class RegisterTests {

        /**
         * CP-05: Registro exitoso.
         * El usuario se persiste en BD, la contraseña se hashea
         * y el balance inicial es 0.
         */
        @Test
        @DisplayName("CP-05 — registro exitoso con datos válidos")
        void register_withValidData_savesUserWithHashedPassword() {
            // Arrange
            RegisterRequest request = new RegisterRequest(
                    "nuevo@test.com", "Admin123!", "3001234567", UserRole.CONSUMER);

            when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
            when(userRepository.findByPhoneNumber(request.phoneNumber())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(request.password())).thenReturn("hashed_password");

            // Act
            authService.register(request);

            // Assert — capturamos el objeto que se pasó a save()
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getEmail()).isEqualTo("nuevo@test.com");
            assertThat(savedUser.getPassword()).isEqualTo("hashed_password");
            assertThat(savedUser.getPhoneNumber()).isEqualTo("3001234567");
            assertThat(savedUser.getRole()).isEqualTo(UserRole.CONSUMER);
            // CP-05: balance inicial debe ser 0
            assertThat(savedUser.getBalance()).isEqualByComparingTo("0");
        }

        /**
         * CP-04 (variante email): email duplicado lanza BusinessException.
         */
        @Test
        @DisplayName("CP-04 — registro rechazado si el email ya existe")
        void register_withDuplicateEmail_throwsBusinessException() {
            RegisterRequest request = new RegisterRequest(
                    "existe@test.com", "Admin123!", "3001234567", UserRole.CONSUMER);

            when(userRepository.findByEmail(request.email()))
                    .thenReturn(Optional.of(new User()));

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("correo");

            // El usuario nunca debe persistirse
            verify(userRepository, never()).save(any());
        }

        /**
         * CP-04 (variante teléfono): número duplicado lanza BusinessException.
         */
        @Test
        @DisplayName("CP-04 — registro rechazado si el teléfono ya existe")
        void register_withDuplicatePhone_throwsBusinessException() {
            RegisterRequest request = new RegisterRequest(
                    "nuevo@test.com", "Admin123!", "3001234567", UserRole.CONSUMER);

            when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
            when(userRepository.findByPhoneNumber(request.phoneNumber()))
                    .thenReturn(Optional.of(new User()));

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("teléfono");

            verify(userRepository, never()).save(any());
        }
    }

    // ════════════════════════════════════════════════════════════
    // BLOQUE 2 — forgotPassword()
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("forgotPassword()")
    class ForgotPasswordTests {

        /**
         * CP-06: El correo SÍ existe.
         * Debe generar token y enviar email — sin lanzar excepción.
         */
        @Test
        @DisplayName("CP-06 — correo existente genera token y envía email")
        void forgotPassword_withExistingEmail_generatesTokenAndSendsEmail() {
            User user = new User();
            user.setId(1L);
            user.setEmail("user@test.com");

            ForgotPasswordRequest request = new ForgotPasswordRequest("user@test.com");

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

            // Act — no debe lanzar excepción
            authService.forgotPassword(request);

            // Assert — token eliminado, guardado y correo enviado
            verify(tokenRepository).deleteByUser_Id(1L);
            verify(tokenRepository).save(any(PasswordResetToken.class));
            verify(emailService).sendPasswordResetEmail(eq("user@test.com"), anyString());
        }

        /**
         * CP-07: El correo NO existe.
         * No debe revelar información: ningún email enviado,
         * ningún token guardado, ninguna excepción lanzada.
         */
        @Test
        @DisplayName("CP-07 — correo inexistente no revela información")
        void forgotPassword_withNonExistingEmail_doesNothingAndNoException() {
            ForgotPasswordRequest request = new ForgotPasswordRequest("noexiste@test.com");

            when(userRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

            // Act — no debe lanzar excepción (seguridad por oscuridad)
            authService.forgotPassword(request);

            // Assert — 0 operaciones sobre repositorios ni emailService
            verify(tokenRepository, never()).deleteByUser_Id(any());
            verify(tokenRepository, never()).save(any());
            verify(emailService, never()).sendPasswordResetEmail(any(), any());
        }
    }

    // ════════════════════════════════════════════════════════════
    // BLOQUE 3 — resetPassword()
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("resetPassword()")
    class ResetPasswordTests {

        /**
         * CP-08: Token válido y vigente — contraseña actualizada correctamente.
         */
        @Test
        @DisplayName("CP-08 — token válido actualiza la contraseña del usuario")
        void resetPassword_withValidToken_updatesUserPassword() {
            User user = new User();
            user.setEmail("user@test.com");

            // Token que NO ha expirado
            PasswordResetToken token = mock(PasswordResetToken.class);
            when(token.isExpired()).thenReturn(false);
            when(token.getUser()).thenReturn(user);

            ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "NewPass123!");

            when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
            when(passwordEncoder.encode("NewPass123!")).thenReturn("new_hashed");

            // Act
            authService.resetPassword(request);

            // Assert — contraseña actualizada y token eliminado (single-use)
            assertThat(user.getPassword()).isEqualTo("new_hashed");
            verify(userRepository).save(user);
            verify(tokenRepository).delete(token);
        }

        /**
         * CP-08 (variante expirado): token expirado lanza IllegalArgumentException
         * y el token es eliminado de la BD.
         */
        @Test
        @DisplayName("CP-08 — token expirado lanza excepción y elimina el token")
        void resetPassword_withExpiredToken_throwsAndDeletesToken() {
            PasswordResetToken token = mock(PasswordResetToken.class);
            when(token.isExpired()).thenReturn(true);

            ResetPasswordRequest request = new ResetPasswordRequest("expired-token", "NewPass123!");

            when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expirado");

            // Token debe eliminarse aunque esté expirado
            verify(tokenRepository).delete(token);
            // La contraseña NUNCA debe cambiarse
            verify(userRepository, never()).save(any());
        }

        /**
         * Token completamente inválido (no existe en BD).
         */
        @Test
        @DisplayName("token inválido lanza IllegalArgumentException")
        void resetPassword_withInvalidToken_throwsIllegalArgument() {
            ResetPasswordRequest request = new ResetPasswordRequest("fake-token", "NewPass123!");

            when(tokenRepository.findByToken("fake-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inválido");

            verify(userRepository, never()).save(any());
        }
    }
}