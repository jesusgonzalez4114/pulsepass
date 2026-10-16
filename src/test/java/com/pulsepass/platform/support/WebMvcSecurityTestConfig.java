package com.pulsepass.platform.support;

import com.pulsepass.platform.security.CustomUserDetailsService;
import com.pulsepass.platform.security.JwtProperties;
import com.pulsepass.platform.security.JwtService;
import com.pulsepass.platform.security.RestAccessDeniedHandler;
import com.pulsepass.platform.security.RestAuthenticationEntryPoint;
import com.pulsepass.platform.security.SecurityConfig;

import org.mockito.Mockito;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Para @WebMvcTest: monta la seguridad REAL (cadena de filtros, JWT, 401/403) en lugar de desactivarla.
 * Solo se sustituye por un mock el acceso a la base de datos (CustomUserDetailsService).
 * Los tests que necesiten identidades usan @WithMockUser o un token emitido con el JwtService real.
 */
@TestConfiguration
@EnableConfigurationProperties(JwtProperties.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, JwtService.class})
public class WebMvcSecurityTestConfig {

    @Bean
    CustomUserDetailsService customUserDetailsService() {
        return Mockito.mock(CustomUserDetailsService.class);
    }
}
