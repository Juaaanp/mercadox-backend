package com.IngSoftwarelll.mercadox.services.interfaces;


import com.IngSoftwarelll.mercadox.dtos.auth.forgotpassword.ForgotPasswordRequest;
import com.IngSoftwarelll.mercadox.dtos.auth.forgotpassword.ResetPasswordRequest;
import com.IngSoftwarelll.mercadox.dtos.auth.register.RegisterRequest;

public interface AuthService {

    void register(RegisterRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}