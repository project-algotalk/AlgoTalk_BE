package com.algotalk.userservice.service.impl;

import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.service.IJwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService implements IJwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    @Value("${jwt.access.token.expiration}")
    private long accessTokenExpiration; // 15분
    @Value("${jwt.refresh.token.expiration}")
    private long refreshTokenExpiration; // 7일

    @Override
    public String generateAccessToken(UserInfoCommand pCommand) throws Exception {
        log.info("{}.generateAccessToken Start!", this.getClass().getName());

        Instant now = Instant.now();
        long expirationSeconds = accessTokenExpiration / 1000; // ms -> 초 변환
        Instant expiry = now.plusSeconds(expirationSeconds);

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("algotalk") // 토큰 발급자
                .issuedAt(now) // 발급 시간
                .expiresAt(expiry) // 만료 시간
                .subject(String.valueOf(pCommand.getUserId())) // 사용자 ID를 subject로 설정
                .claim("loginId", pCommand.getLoginId()) // 추가 클레임
                .claim("nickname", pCommand.getNickname()) // 추가 클레임
                .build();

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        log.info("{}.generateAccessToken End!", this.getClass().getName());
        return accessToken;
    }

    @Override
    public String generateRefreshToken(UserInfoCommand pCommand) throws Exception {
        log.info("{}.generateRefreshToken Start!", this.getClass().getName());

        Instant now = Instant.now();
        long expirationSeconds = refreshTokenExpiration / 1000; // ms -> 초 변환
        Instant expiry = now.plusSeconds(expirationSeconds);

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("algotalk") // 토큰 발급자
                .issuedAt(now) // 발급 시간
                .expiresAt(expiry) // 만료 시간
                .subject(String.valueOf(pCommand.getUserId())) // 사용자 ID를 subject로 설정
                .claim("userId", pCommand.getUserId()) // 토큰 권한 정보
                .build();

        String refreshToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        log.info("{}.generateRefreshToken End!", this.getClass().getName());
        return refreshToken;
    }

    @Override
    public Long getUserIdFromToken(String token) throws Exception {
        return Long.valueOf(
                jwtDecoder.decode(token)
                        .getSubject()
        );
    }
}
