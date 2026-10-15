package com.pulsepass.platform.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Lee el encabezado Authorization: Bearer, valida el JWT y construye el contexto de seguridad
 * con un principal cargado de la base de datos (estado y roles vigentes).
 *
 * Nunca devuelve errores por sí mismo: si el token no es válido simplemente NO autentica,
 * y es la cadena (reglas de rutas + AuthenticationEntryPoint) quien decide 401 o acceso público.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (isAlreadyAuthenticated()) {
            chain.doFilter(request, response);
            return;
        }

        List<String> authorizationHeaders = Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));

        if (authorizationHeaders.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        if (authorizationHeaders.size() > 1) {
            log.debug("Several Authorization headers received: none is trusted");
            chain.doFilter(request, response);
            return;
        }

        String header = authorizationHeaders.get(0);
        if (!header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();

        try {
            Long userId = jwtService.extractUserId(token);
            SecurityPrincipal principal = userDetailsService.loadByUserId(userId);

            if (principal.isEnabled()) {
                UsernamePasswordAuthenticationToken authentication =
                        UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } else {
                log.debug("Token belongs to a disabled user");
            }
        } catch (JwtException | IllegalArgumentException | AuthenticationException ex) {
            // Solo el tipo de error: nunca el token ni el mensaje completo.
            log.debug("Bearer token rejected: {}", ex.getClass().getSimpleName());
        }

        chain.doFilter(request, response);
    }

    private boolean isAlreadyAuthenticated() {
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        return existing != null
                && existing.isAuthenticated()
                && !(existing instanceof AnonymousAuthenticationToken);
    }
}
