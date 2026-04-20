package com.IngSoftwarelll.mercadox.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.IngSoftwarelll.mercadox.models.SecurityEventLog;
import com.IngSoftwarelll.mercadox.repositories.SecurityEventLogRepository;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class AuthenticationFailureListener {

    @Autowired
    private SecurityEventLogRepository repository;

    @Autowired
    private HttpServletRequest request;

    @EventListener
    public void onFailure(org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent event) {

        String username = (String) event.getAuthentication().getPrincipal();
        String ip = request.getRemoteAddr();

        SecurityEventLog log = new SecurityEventLog();
        log.setUsername(username);
        log.setIpAddress(ip);
        log.setEventType("LOGIN_FAILED");
        log.setDescription("Bad credentials");

        repository.save(log);
    }
}