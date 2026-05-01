package com.IngSoftwarelll.mercadox.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.IngSoftwarelll.mercadox.dtos.agent.AgentContextDto;
import com.IngSoftwarelll.mercadox.security.CustomUserDetails;
import com.IngSoftwarelll.mercadox.services.AgentContextService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentContextController {
 
    private final AgentContextService agentContextService;
 
    /**
     * Returns rich context about the authenticated user:
     * their recent purchases, active tickets, balance, etc.
     * This is used by the Next.js API route to build the
     * Gemini system prompt.
     */
    @GetMapping("/context")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<AgentContextDto> getContext(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(
            agentContextService.buildContext(user.getId()));
    }
}