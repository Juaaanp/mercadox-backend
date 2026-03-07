package com.IngSoftwarelll.mercadox.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;

@Service
public class JwtTokenService {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;

    public JwtTokenService(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.access-token.expiration:900000}") long accessTokenExpirationMs) {  // 15 minutos por defecto

        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationMs = accessTokenExpirationMs;

        logger.info("JwtTokenService inicializado - Access token dura {} ms", accessTokenExpirationMs);
    }

    public String generateAccessToken(Authentication authentication) {
        String username = authentication.getName();
        String scope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        Instant now = Instant.now();
        Instant expiration = now.plusMillis(accessTokenExpirationMs);

        String token = Jwts.builder()
                .subject(username)
                .claim("userId", ((CustomUserDetails) authentication.getPrincipal()).getId())
                .claim("scope", scope)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();

        logger.debug("Access token generado para usuario: {}", username);
        return token;
    }
}