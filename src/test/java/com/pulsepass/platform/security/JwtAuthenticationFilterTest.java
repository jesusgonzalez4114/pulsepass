package com.pulsepass.platform.security;

import com.pulsepass.platform.domain.Role;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private CustomUserDetailsService userDetailsService;
    private FilterChain chain;
    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtService = mock(JwtService.class);
        userDetailsService = mock(CustomUserDetailsService.class);
        chain = mock(FilterChain.class);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        request = new MockHttpServletRequest("GET", "/api/auth/me");
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private SecurityPrincipal principal(boolean active, Role... roles) {
        return new SecurityPrincipal(7L, "andrea", "andrea@mail.com", null, active, List.of(roles));
    }

    // TEST-FLT-001
    @Test
    void shouldContinueWithoutAuthenticationWhenThereIsNoAuthorizationHeader() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    // TEST-FLT-002
    @Test
    void shouldAuthenticateWithValidBearerToken() throws Exception {
        request.addHeader("Authorization", "Bearer token-valido");
        when(jwtService.extractUserId("token-valido")).thenReturn(7L);
        when(userDetailsService.loadByUserId(7L)).thenReturn(principal(true, Role.USER, Role.STAFF));

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(((SecurityPrincipal) authentication.getPrincipal()).getUserId()).isEqualTo(7L);
        assertThat(authentication.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_STAFF");
        verify(chain).doFilter(request, response);
    }

    // TEST-FLT-003
    @Test
    void shouldNotAuthenticateWithInvalidToken() throws Exception {
        request.addHeader("Authorization", "Bearer token-roto");
        when(jwtService.extractUserId("token-roto")).thenThrow(new JwtException("invalid"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void shouldNotAuthenticateWhenTokenIsBlank() throws Exception {
        request.addHeader("Authorization", "Bearer    ");
        when(jwtService.extractUserId("")).thenThrow(new JwtException("empty"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    // TEST-FLT-004
    @Test
    void shouldIgnoreRequestWithSeveralAuthorizationHeaders() throws Exception {
        request.addHeader("Authorization", "Bearer token-a");
        request.addHeader("Authorization", "Bearer token-b");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void shouldIgnoreNonBearerSchemes() throws Exception {
        request.addHeader("Authorization", "Basic YW5kcmVhOnNlY3JldA==");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void shouldNeverReadTokenFromQueryString() throws Exception {
        request.setParameter("access_token", "token-valido");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, userDetailsService);
    }

    // TEST-FLT-005
    @Test
    void shouldNotAuthenticateDisabledUser() throws Exception {
        request.addHeader("Authorization", "Bearer token-valido");
        when(jwtService.extractUserId("token-valido")).thenReturn(7L);
        when(userDetailsService.loadByUserId(7L)).thenReturn(principal(false, Role.USER));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWhenUserNoLongerExists() throws Exception {
        request.addHeader("Authorization", "Bearer token-valido");
        when(jwtService.extractUserId("token-valido")).thenReturn(99L);
        when(userDetailsService.loadByUserId(99L)).thenThrow(new UsernameNotFoundException("gone"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    // TEST-FLT-006
    @Test
    void shouldNotOverwriteAnAlreadyEstablishedAuthentication() throws Exception {
        Authentication existing = UsernamePasswordAuthenticationToken.authenticated(
                "otro", null, AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(existing);
        request.addHeader("Authorization", "Bearer token-valido");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        verify(jwtService, never()).extractUserId(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldReplaceAnonymousAuthentication() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        request.addHeader("Authorization", "Bearer token-valido");
        when(jwtService.extractUserId("token-valido")).thenReturn(7L);
        when(userDetailsService.loadByUserId(7L)).thenReturn(principal(true, Role.USER));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isInstanceOf(SecurityPrincipal.class);
    }
}
