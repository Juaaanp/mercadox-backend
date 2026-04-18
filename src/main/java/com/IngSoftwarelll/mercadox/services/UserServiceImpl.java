package com.IngSoftwarelll.mercadox.services;

import java.math.BigDecimal;

import org.hibernate.ObjectNotFoundException;
import org.springframework.stereotype.Service;

import com.IngSoftwarelll.mercadox.models.User;
import com.IngSoftwarelll.mercadox.repositories.UserRepository;
import com.IngSoftwarelll.mercadox.services.interfaces.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;

    @Override
    public User getUserById(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User id must be positive");
        }
        return userRepository.findById(userId).orElseThrow(() -> new ObjectNotFoundException("User id : " + userId + " not found. ", User.class));
    }

    @Override
    public BigDecimal getBalance(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User id must be positive");
        }

        return BigDecimal.valueOf(userRepository.findBalanceByUserId(userId));
    }
    
}
