package com.IngSoftwarelll.mercadox.services.interfaces;

import com.IngSoftwarelll.mercadox.models.Purchase;

public interface EmailService {
    void sendPurchaseConfirmation(Purchase purchase, String userEmail);

    void sendPasswordResetEmail(String to, String token);
}
