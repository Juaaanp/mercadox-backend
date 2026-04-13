package com.IngSoftwarelll.mercadox.dtos.auth.register;


import com.IngSoftwarelll.mercadox.models.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password,

        @NotBlank(message = "El número de teléfono es obligatorio")
        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Número de teléfono inválido")
        String phoneNumber,


        @NotNull(message = "El rol es obligatorio")
        @JsonProperty("role")
        UserRole role

) {}
