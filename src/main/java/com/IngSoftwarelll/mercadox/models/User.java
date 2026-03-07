package com.IngSoftwarelll.mercadox.models;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.IngSoftwarelll.mercadox.models.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Users")
@Data
@NoArgsConstructor
public class User{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
    private String password; //JsonIgnore, encrypt

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private UserRole role;
   
    private ZonedDateTime registeredDate;

    private BigDecimal balance;


    public void subtractBalance(BigDecimal amount){
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount to subtract must be positive");
        }

        if (amount.compareTo(balance) > 0) {
            throw new IllegalArgumentException("Amount to subtract is higher than current balance");
        }

        this.balance = balance.subtract(amount);
    }

}
