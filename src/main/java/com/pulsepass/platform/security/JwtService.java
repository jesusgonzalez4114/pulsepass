package com.pulsepass.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;


@Service
public class JwtService {

    private static final int MIN_KEY_BYTES = 32;
    private static final String EXPECTED_ALGORITHM = "HS256";
    private static final long ALLOWED_CLOCK_SKEW_SECONDS = 60;

    private final SecretKey key;
    private final String issuer;
    private final Duration accessTokenTtl;
    private final Clock clock;
    private final JwtParser parser;

    @Autowired
    public JwtService(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    /** Constructor */
    public JwtService(JwtProperties properties, Clock clock) {
        if (properties.issuer() == null || properties.issuer().isBlank()) {
            throw new IllegalStateException("pulsepass.security.jwt.issuer is required");
        }
        if (properties.accessTokenTtl() == null
                || properties.accessTokenTtl().isZero()
                || properties.accessTokenTtl().isNegative()) {
            throw new IllegalStateException("pulsepass.security.jwt.access-token-ttl must be positive");
        }
        this.key = buildKey(properties.secretBase64());
        this.issuer = properties.issuer();
        this.accessTokenTtl = properties.accessTokenTtl();
        this.clock = clock;
        this.parser = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build();
    }

    private static SecretKey buildKey(String secretBase64) {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret is missing: define the PULSEPASS_JWT_SECRET_BASE64 environment variable");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretBase64.trim());
        } catch (RuntimeException ex) {
            // No se encadena la causa: podría contener fragmentos del secreto.
            throw new IllegalStateException("JWT secret is not valid Base64");
        }
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "JWT secret is too short: it must decode to at least " + MIN_KEY_BYTES + " bytes");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /** Emite un token de acceso. El payload solo lleva el ID del usuario y metadatos, nunca roles ni datos personales. */
    public String generateToken(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Valida firma, emisor, expiración, algoritmo y claims obligatorios, y devuelve el ID de usuario (sub).
     *
     * @throws JwtException si el token no es aceptable por cualquier motivo
     */
    public Long extractUserId(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtException("Token is empty");
        }

        Jws<Claims> jws = parser.parseSignedClaims(token);

        if (!EXPECTED_ALGORITHM.equals(jws.getHeader().getAlgorithm())) {
            throw new JwtException("Unexpected signing algorithm");
        }

        Claims claims = jws.getPayload();
        String subject = claims.getSubject();

        if (claims.getExpiration() == null
                || claims.getIssuedAt() == null
                || claims.getId() == null
                || subject == null
                || subject.isBlank()) {
            throw new JwtException("Missing mandatory claims");
        }

        Instant latestAcceptableIssueTime = clock.instant().plusSeconds(ALLOWED_CLOCK_SKEW_SECONDS);
        if (claims.getIssuedAt().toInstant().isAfter(latestAcceptableIssueTime)) {
            throw new JwtException("Token issued in the future");
        }

        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException ex) {
            throw new JwtException("Invalid subject");
        }
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }
}
