package com.IngSoftwarelll.mercadox.dtos.auth.login;

import com.IngSoftwarelll.mercadox.models.enums.UserRole;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    
    private String accessToken;
    private UserRole role;
}
