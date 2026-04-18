package com.IngSoftwarelll.mercadox.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.IngSoftwarelll.mercadox.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);

    @Query("""
            SELECT u.balance
            FROM User u
            WHERE u.id = :userId
            """)
    Long findBalanceByUserId (@Param("userId") Long userId);
}
