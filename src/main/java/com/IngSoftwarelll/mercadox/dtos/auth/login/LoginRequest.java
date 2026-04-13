package com.IngSoftwarelll.mercadox.dtos.auth.login;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
    
    @NotBlank(message = "El correo electrónico es obligatorio")
    private String email;
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
