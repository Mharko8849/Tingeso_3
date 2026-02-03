package com.example.demo.ConfigTest;

import com.example.demo.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SecurityConfigTest {

    @Test
    public void testCorsConfigurationSource() {
        SecurityConfig securityConfig = new SecurityConfig();
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertNotNull(source);
    }

    @Test
    public void testJwtAuthConverter() {
        // Access the private method via reflection or just test the logic if extracted.
        // Since it's private and used inside filterChain, we can try to invoke it via reflection 
        // or just trust integration tests. 
        // However, to increase coverage of the private method lines, we can use reflection.
        
        SecurityConfig securityConfig = new SecurityConfig();
        
        try {
            java.lang.reflect.Method method = SecurityConfig.class.getDeclaredMethod("jwtAuthConverter");
            method.setAccessible(true);
            JwtAuthenticationConverter converter = (JwtAuthenticationConverter) method.invoke(securityConfig);
            assertNotNull(converter);

            // Test the converter logic
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "test-user");
            Map<String, Object> realmAccess = new HashMap<>();
            realmAccess.put("roles", List.of("ADMIN", "USER"));
            claims.put("realm_access", realmAccess);
            
            Map<String, Object> headers = new HashMap<>();
            headers.put("alg", "RS256");
            
            Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
            
            AbstractAuthenticationToken token = converter.convert(jwt);
            assertNotNull(token);
            Collection<GrantedAuthority> authorities = token.getAuthorities();
            assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
            assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));

        } catch (Exception e) {
            e.printStackTrace();
            fail("Reflection failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testJwtAuthConverter_NoRoles() throws Exception {
        SecurityConfig securityConfig = new SecurityConfig();
        
        java.lang.reflect.Method method = SecurityConfig.class.getDeclaredMethod("jwtAuthConverter");
        method.setAccessible(true);
        JwtAuthenticationConverter converter = (JwtAuthenticationConverter) method.invoke(securityConfig);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "test-user");
        // No realm_access
        
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
        
        AbstractAuthenticationToken token = converter.convert(jwt);
        assertNotNull(token);
        assertTrue(token.getAuthorities().isEmpty());
    }
}
