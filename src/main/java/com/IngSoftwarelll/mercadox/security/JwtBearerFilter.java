package com.IngSoftwarelll.mercadox.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/**
 * Filtro JWT puro con Spring Security (sin ninguna dependencia de OAuth2).
 * Usa JJWT (la librería estándar independiente) para validar el token.
 */
public class JwtBearerFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtBearerFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final String secretKey;

    public JwtBearerFilter(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromHeader(request);

        if (token != null) {
            try {
                // Validación completa: firma, expiración, formato, etc.
                SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                if (!isAccessToken(claims)) {
                    logger.warn("JWT no es un access token (type != access)");
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                Collection<GrantedAuthority> authorities = extractAuthorities(claims);

                // Autenticación estándar de Spring Security (sin OAuth2)
                String username = claims.getSubject();
                Long userId = claims.get("userId", Long.class);

                // Reconstruyes tu CustomUserDetails
                CustomUserDetails userDetails = new CustomUserDetails(
                        userId,
                        username,
                        "",
                        authorities
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Autenticación JWT exitosa para usuario: {}", username);

            } catch (JwtException e) {
                logger.warn("JWT inválido: {}");
                SecurityContextHolder.clearContext();
            } catch (Exception e) { 
                logger.error("Error inesperado al procesar JWT", e);
                SecurityContextHolder.clearContext();
            }
        } else {
            logger.debug("No se encontró token Bearer en el header Authorization");
        }

        // Siempre continuamos con la cadena de filtros
        filterChain.doFilter(request, response);
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
            return StringUtils.hasText(token) ? token : null;
        }
        return null;
    }

    private Collection<GrantedAuthority> extractAuthorities(Claims claims) {
        String scopes = claims.get("scope", String.class);
        if (StringUtils.hasText(scopes)) {
            return AuthorityUtils.createAuthorityList(scopes.split("\\s+"));
        }
        return Collections.emptyList();
    }

    private boolean isAccessToken(Claims claims) {
        String type = claims.get("type", String.class);
        return "access".equals(type);
    }
}