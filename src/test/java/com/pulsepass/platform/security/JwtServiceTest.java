package com.pulsepass.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class JwtServiceTest {

    private static final String ISSUER = "pulsepass-api";
    private static final String SECRET = Base64.getEncoder().encodeToString(
            "clave-ficticia-solo-para-pruebas-01-64-bytes-0123456789abcdefghij".getBytes());
    private static final String OTHER_SECRET = Base64.getEncoder().encodeToString(
            "otra-clave-ficticia-solo-para-pruebas-02-64-bytes-0123456789abcd".getBytes());

    private JwtService service;

    @BeforeEach
    void setUp() {
        service = new JwtService(new JwtProperties(ISSUER, SECRET, Duration.ofMinutes(15)));
    }

    private SecretKey key(String base64) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64));
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key(SECRET)).build().parseSignedClaims(token).getPayload();
    }

    // TEST-JWT-001
    @Test
    void shouldIssueTokenWithMandatoryClaimsAndFifteenMinutesTtl() {
        String token = service.generateToken(42L);

        Claims claims = parse(token);

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getIssuedAt()).isNotNull();
        long lifetime = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(lifetime).isEqualTo(900_000L);
        assertThat(service.getAccessTokenTtlSeconds()).isEqualTo(900L);
    }

    @Test
    void shouldNotPutRolesOrPersonalDataInThePayload() {
        Claims claims = parse(service.generateToken(1L));

        assertThat(claims.keySet()).containsExactlyInAnyOrder("sub", "iss", "iat", "exp", "jti");
    }

    @Test
    void shouldIssueDifferentJtiEachTime() {
        assertThat(parse(service.generateToken(1L)).getId())
                .isNotEqualTo(parse(service.generateToken(1L)).getId());
    }

    // TEST-JWT-002
    @Test
    void shouldExtractUserIdFromValidToken() {
        assertThat(service.extractUserId(service.generateToken(42L))).isEqualTo(42L);
    }

    // TEST-JWT-003
    @Test
    void shouldRejectExpiredToken() {
        Clock past = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
        JwtService oldService = new JwtService(new JwtProperties(ISSUER, SECRET, Duration.ofMinutes(15)), past);

        String expired = oldService.generateToken(42L);

        assertThatThrownBy(() -> service.extractUserId(expired)).isInstanceOf(ExpiredJwtException.class);
    }

    // TEST-JWT-004
    @Test
    void shouldRejectTokenWithWrongIssuer() {
        JwtService other = new JwtService(new JwtProperties("otro-emisor", SECRET, Duration.ofMinutes(15)));

        assertThatThrownBy(() -> service.extractUserId(other.generateToken(42L)))
                .isInstanceOf(JwtException.class);
    }

    // TEST-JWT-005
    @Test
    void shouldRejectTokenSignedWithAnotherKey() {
        JwtService other = new JwtService(new JwtProperties(ISSUER, OTHER_SECRET, Duration.ofMinutes(15)));

        assertThatThrownBy(() -> service.extractUserId(other.generateToken(42L)))
                .isInstanceOf(JwtException.class);
    }

    // TEST-JWT-006
    @Test
    void shouldRejectTamperedToken() {
        String token = service.generateToken(42L);
        String[] parts = token.split("\\.");
        // Se altera la firma cambiando su primer carácter por otro distinto.
        char first = parts[2].charAt(0);
        String tamperedSignature = (first == 'A' ? 'B' : 'A') + parts[2].substring(1);
        String tampered = parts[0] + "." + parts[1] + "." + tamperedSignature;

        assertThatThrownBy(() -> service.extractUserId(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhosePayloadWasReplaced() {
        String token = service.generateToken(42L);
        String otherPayload = service.generateToken(1L).split("\\.")[1];
        String[] parts = token.split("\\.");

        assertThatThrownBy(() -> service.extractUserId(parts[0] + "." + otherPayload + "." + parts[2]))
                .isInstanceOf(JwtException.class);
    }

    // TEST-JWT-007
    @Test
    void shouldRejectEmptyOrMalformedTokens() {
        assertThatThrownBy(() -> service.extractUserId(null)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.extractUserId("")).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.extractUserId("   ")).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.extractUserId("esto.no.es-un-jwt")).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.extractUserId("basura")).isInstanceOf(JwtException.class);
    }

    // TEST-JWT-008
    @Test
    void shouldRejectUnsignedTokenWithAlgNone() {
        String unsigned = Jwts.builder()
                .subject("42").issuer(ISSUER).id(UUID.randomUUID().toString())
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .compact();

        assertThatThrownBy(() -> service.extractUserId(unsigned)).isInstanceOf(JwtException.class);
    }

    // TEST-JWT-009
    @Test
    void shouldRejectTokenSignedWithUnexpectedAlgorithm() {
        // Misma clave, pero HS512
        SecretKey sameKey = key(SECRET);
        String hs512 = Jwts.builder()
                .subject("42").issuer(ISSUER).id(UUID.randomUUID().toString())
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(sameKey, Jwts.SIG.HS512)
                .compact();

        assertThatThrownBy(() -> service.extractUserId(hs512)).isInstanceOf(JwtException.class);
    }

    // TEST-JWT-010
    @Test
    void shouldRejectTokenWithoutMandatoryClaims() {
        SecretKey sameKey = key(SECRET);
        Date now = new Date();
        Date exp = new Date(now.getTime() + 60_000);

        String noSubject = Jwts.builder().issuer(ISSUER).id("x").issuedAt(now).expiration(exp)
                .signWith(sameKey, Jwts.SIG.HS256).compact();
        String noExpiration = Jwts.builder().subject("42").issuer(ISSUER).id("x").issuedAt(now)
                .signWith(sameKey, Jwts.SIG.HS256).compact();
        String noJti = Jwts.builder().subject("42").issuer(ISSUER).issuedAt(now).expiration(exp)
                .signWith(sameKey, Jwts.SIG.HS256).compact();
        String nonNumericSubject = Jwts.builder().subject("andrea").issuer(ISSUER).id("x")
                .issuedAt(now).expiration(exp).signWith(sameKey, Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> service.extractUserId(noSubject)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.extractUserId(noExpiration)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.extractUserId(noJti)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.extractUserId(nonNumericSubject)).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenIssuedFarInTheFuture() {
        Clock future = Clock.fixed(Instant.now().plus(Duration.ofHours(1)), ZoneOffset.UTC);
        JwtService futureService = new JwtService(new JwtProperties(ISSUER, SECRET, Duration.ofHours(3)), future);

        assertThatThrownBy(() -> service.extractUserId(futureService.generateToken(42L)))
                .isInstanceOf(JwtException.class);
    }

    // TEST-JWT-011
    @Test
    void shouldFailToStartWhenSecretIsMissing() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties(ISSUER, null, Duration.ofMinutes(15))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService(new JwtProperties(ISSUER, "  ", Duration.ofMinutes(15))))
                .isInstanceOf(IllegalStateException.class);
    }

    // TEST-JWT-012
    @Test
    void shouldFailToStartWhenSecretIsTooShort() {
        String weak = Base64.getEncoder().encodeToString("corta".getBytes());

        assertThatThrownBy(() -> new JwtService(new JwtProperties(ISSUER, weak, Duration.ofMinutes(15))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too short");
    }

    @Test
    void shouldFailToStartWhenSecretIsNotBase64AndNeverEchoItInTheMessage() {
        String notBase64 = "esto no es base64 !!! con espacios";

        assertThatThrownBy(() -> new JwtService(new JwtProperties(ISSUER, notBase64, Duration.ofMinutes(15))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining(notBase64);
    }

    @Test
    void propertiesToStringShouldMaskTheSecret() {
        assertThat(new JwtProperties(ISSUER, SECRET, Duration.ofMinutes(15)).toString())
                .doesNotContain(SECRET);
    }
}
