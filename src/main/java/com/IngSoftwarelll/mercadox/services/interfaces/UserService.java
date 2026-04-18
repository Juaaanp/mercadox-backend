package com.IngSoftwarelll.mercadox.services.interfaces;

import java.math.BigDecimal;

import com.IngSoftwarelll.mercadox.models.User;

public interface UserService {
    
    User getUserById(Long userId);
    BigDecimal getBalance (Long userId);
}
