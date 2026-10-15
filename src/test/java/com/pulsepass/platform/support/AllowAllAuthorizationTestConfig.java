package com.pulsepass.platform.support;

import com.pulsepass.platform.service.AuthorizationService;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Para las pruebas de regresión funcional de los controladores: las políticas de propiedad responden
 * "permitido" y así esas pruebas siguen verificando solo códigos HTTP y DTOs.
 * Las políticas reales se prueban en AuthorizationServiceImplTest y SecurityHttpMatrixTest.
 */
@TestConfiguration
public class AllowAllAuthorizationTestConfig {

    @Bean("authorizationService")
    AuthorizationService authorizationService() {
        return Mockito.mock(AuthorizationService.class,
                invocation -> invocation.getMethod().getReturnType() == boolean.class ? Boolean.TRUE : null);
    }
}
