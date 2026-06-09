package com.algotalk.userservice.service.impl;

import com.algotalk.userservice.dto.auth.RefreshTokenIssue;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.service.IJwtTokenService;
import com.algotalk.userservice.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService implements IJwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    @Value("${jwt.access.token.expiration}")
    private long accessTokenExpiration; // ms
    @Value("${jwt.refresh.token.expiration}")
    private long refreshTokenExpiration; // ms

    @Value("${jwt.refresh.session.absolute-expiration:2592000000}")
    private long absoluteSessionExpiration; // ms, 기본 30일

    @Override
    public String generateAccessToken(UserInfoCommand pCommand, String sessionId) throws Exception {
        log.info("{}.generateAccessToken Start!", this.getClass().getName());

        Instant now = Instant.now();
        long expirationSeconds = accessTokenExpiration / 1000; // ms -> 초 변환
        Instant expiry = now.plusSeconds(expirationSeconds);

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("algotalk") // 토큰 발급자
                .issuedAt(now) // 발급 시간
                .expiresAt(expiry) // 만료 시간
                .subject(String.valueOf(pCommand.getUserId())) // 사용자 ID subject 설정
                .claim("loginId", CmmUtil.nvl(pCommand.getLoginId())) // 추가 클레임
                .claim("nickname", CmmUtil.nvl(pCommand.getNickname())) // 추가 클레임
                .claim("roles", List.of(pCommand.getRole())) // 토큰 권한 정보
                .claim("sessionId", sessionId) // Redis 로그인 세션과 Access Token 연결
                .build();

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        log.info("{}.generateAccessToken End!", this.getClass().getName());
        return accessToken;
    }

    @Override
    public RefreshTokenIssue issueRefreshToken(UserInfoCommand pCommand) throws Exception {
        log.info("{}.issueRefreshToken Start!", this.getClass().getName());

        Instant now = Instant.now();
        String sessionId = UUID.randomUUID().toString();
        Instant absoluteExpiresAt = now.plusMillis(absoluteSessionExpiration).truncatedTo(ChronoUnit.SECONDS);
        RefreshTokenIssue refreshTokenIssue = generateRefreshToken(pCommand, sessionId, absoluteExpiresAt, now);

        log.info("{}.issueRefreshToken End!", this.getClass().getName());
        return refreshTokenIssue;
    }

    @Override
    public RefreshTokenIssue rotateRefreshToken(
            UserInfoCommand pCommand,
            String sessionId,
            Instant absoluteExpiresAt
    ) throws Exception {
        log.info("{}.rotateRefreshToken Start!", this.getClass().getName());

        RefreshTokenIssue refreshTokenIssue =
                generateRefreshToken(pCommand, sessionId, absoluteExpiresAt, Instant.now());

        log.info("{}.rotateRefreshToken End!", this.getClass().getName());
        return refreshTokenIssue;
    }

    private RefreshTokenIssue generateRefreshToken(
            UserInfoCommand pCommand,
            String sessionId,
            Instant absoluteExpiresAt,
            Instant now
    ) {
        log.info("{}.generateRefreshToken Start!", this.getClass().getName());

        if (!absoluteExpiresAt.isAfter(now)) {
            throw new IllegalStateException("Refresh Token 세션의 절대 만료 시간이 지났습니다.");
        }

        Instant slidingExpiresAt = now.plusMillis(refreshTokenExpiration);
        Instant expiresAt = slidingExpiresAt.isBefore(absoluteExpiresAt)
                ? slidingExpiresAt
                : absoluteExpiresAt;

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("algotalk")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(String.valueOf(pCommand.getUserId()))
                .claim("userId", pCommand.getUserId())
                .claim("sessionId", sessionId)
                .claim("sessionExpiresAt", absoluteExpiresAt.getEpochSecond())
                .id(UUID.randomUUID().toString())
                .build();

        String refreshToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        log.info("{}.generateRefreshToken End!", this.getClass().getName());
        return new RefreshTokenIssue(refreshToken, sessionId, expiresAt, absoluteExpiresAt);
    }

    @Override
    public Long getUserIdFromToken(String token) throws Exception {
        log.info("{}.getUserIdFromToken Start!", this.getClass().getName());

        Long userId = Long.valueOf(jwtDecoder.decode(token).getSubject());

        log.info("{}.getUserIdFromToken End!", this.getClass().getName());
        return userId;
    }

    @Override
    public String getSessionIdFromToken(String token) throws Exception {
        log.info("{}.getSessionIdFromToken Start!", this.getClass().getName());

        String sessionId = jwtDecoder.decode(token).getClaimAsString("sessionId");

        log.info("{}.getSessionIdFromToken End!", this.getClass().getName());
        return sessionId;
    }

    @Override
    public Instant getSessionExpiresAtFromToken(String token) throws Exception {
        log.info("{}.getSessionExpiresAtFromToken Start!", this.getClass().getName());

        Object claim = jwtDecoder.decode(token).getClaim("sessionExpiresAt");
        Instant sessionExpiresAt = claim instanceof Number number
                ? Instant.ofEpochSecond(number.longValue())
                : Instant.ofEpochSecond(Long.parseLong(String.valueOf(claim)));

        log.info("{}.getSessionExpiresAtFromToken End!", this.getClass().getName());
        return sessionExpiresAt;
    }
}
