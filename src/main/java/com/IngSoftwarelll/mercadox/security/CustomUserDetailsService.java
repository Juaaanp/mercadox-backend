package com.IngSoftwarelll.mercadox.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.IngSoftwarelll.mercadox.models.User;
import com.IngSoftwarelll.mercadox.repositories.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + identifier));

        SimpleGrantedAuthority scope = new SimpleGrantedAuthority(user.getRole().toString());
        log.info("hola {}", scope);
        return new CustomUserDetails(user, List.of(scope));
    }
}
