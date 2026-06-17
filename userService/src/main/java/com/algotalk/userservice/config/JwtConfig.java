package com.algotalk.userservice.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * JWT 토큰 발급/검증/권한 변환에 필요한 Bean 을 등록하는 설정 클래스
 * 서버가 보유한 secret key 하나로 서명과 검증을 모두 수행
 *
 * 전체 흐름:
 * 1. 로그인 성공
 *    AuthService -> JwtEncoder -> AT(Access Token) 발급 -> 클라이언트 반환
 * 2. 이후 API 요청 (Authorization: Bearer {AT})
 *    Spring Security -> JwtDecoder -> AT 서명/만료 검증
 *    -> JwtAuthenticationConverter -> roles 클레임을 ROLE_USER 등 권한으로 변환
 *    -> SecurityContext 저장 -> API 접근 허용
 */
@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    // application-local.yml의 jwt.secret.key 값을 주입받음
    // 반드시 Base64로 인코딩된 문자열이어야 함
    @Value("${jwt.secret.key}")
    private String secretBase64;

    /**
     * JWT 서명/검증에 사용할 비밀 키(SecretKey) 생성
     *
     * - yml에 저장된 Base64 문자열을 디코딩해 실제 바이트 배열로 변환
     * - SecretKey 객체로 만들어 Spring 컨테이너에 등록
     */
    @Bean
    public SecretKey jwtSecretKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretBase64);
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    /**
     * JWT 발급기 (JwtEncoder)
     * 로그인 성공 시 Access Token(AT)을 만들 때 사용
     *
     * 동작 방식:
     *   1. 서비스에서 userId, roles 등 클레임(payload)을 담은 JwtClaimsSet 생성
     *   2. JwtEncoder.encode(JwtClaimsSet) 호출
     *   3. 내부적으로 jwtSecretKey()로 서명 -> 완성된 JWT 문자열 반환
     */
    @Bean
    public JwtEncoder jwtEncoder(SecretKey key) {
        // ImmutableSecret: 키가 바뀌지 않는 단순 대칭키 소스
        JWKSource<SecurityContext> jwkSource = new ImmutableSecret<>(key);
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * JWT 검증기 (JwtDecoder)
     *
     * - 클라이언트가 API 요청 시 보내는 Access Token을 검증할 때 사용
     * - SecurityConfig에서 oauth2ResourceServer().jwt() 설정을 하면
     *   Spring Security가 이 Bean을 자동으로 찾아서 모든 요청마다 토큰 검증
     *
     * 검증 항목:
     *   - 서명이 올바른지 (위조 여부)
     *   - 만료 시간(exp)이 지나지 않았는지
     *   - 발급 대상(iss) 등 표준 클레임
     */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey key) {
        return NimbusJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256) // 서명 알고리즘은 발급 시와 동일해야 함
                .build();
    }

    /**
     * JWT 권한 변환기 (JwtAuthenticationConverter)
     *
     * - JwtDecoder가 토큰 검증을 마친 뒤, 토큰 안의 권한 정보를
     *   Spring Security가 이해하는 형태로 바꿔주는 역할
     *
     * 변환 흐름:
     *   AT payload 안의 "roles": ["ROLE_USER"]
     *       ->  setAuthoritiesClaimName("roles") 로 roles 필드를 읽음
     *       ->  setAuthorityPrefix("ROLE_") 로 Spring Security 권한 prefix를 붙임
     *   Spring Security 권한 객체 -> GrantedAuthority("ROLE_USER")
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter delegate = new JwtGrantedAuthoritiesConverter();
        // JWT roles 클레임은 "USER"처럼 ROLE_ prefix 없이 저장하고,
        // Spring Security 권한으로 변환하는 이 지점에서만 ROLE_ prefix를 붙인다.
        delegate.setAuthorityPrefix("ROLE_");           // "USER" -> "ROLE_USER"
        delegate.setAuthoritiesClaimName("roles");      // payload에서 읽어올 필드명

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(delegate);
        return converter;
    }
}