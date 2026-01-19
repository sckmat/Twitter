package com.ziminpro.ums.auth;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${security.jwt.secret:}")
    private String secret;

    @Value("${security.jwt.issuer:ums}")
    private String issuer;

    @Value("${security.jwt.ttl-seconds:86400}")
    private long ttlSeconds;

    private Algorithm algorithm;
    private JWTVerifier verifier;

    @PostConstruct
    public void init() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("security.jwt.secret is required");
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).withIssuer(issuer).build();
    }

    public String issueToken(UUID userId, String email,  List<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);

        List<String> safeRoles = roles == null ? List.of() : roles;

        return JWT.create()
                .withIssuer(issuer)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(exp))
                .withSubject(userId.toString())
                .withClaim("email", email)
                .withClaim("roles", safeRoles)
                .sign(algorithm);
    }

    public List<String> getRoles(String token) {
        DecodedJWT jwt = verifier.verify(token);
        List<String> roles = jwt.getClaim("roles").asList(String.class);
        return roles == null ? List.of() : roles;
    }

    public UUID verifyAndGetUserId(String token) {
        DecodedJWT jwt = verifier.verify(token);
        return UUID.fromString(jwt.getSubject());
    }

    public String getEmail(String token) {
        DecodedJWT jwt = verifier.verify(token);
        return jwt.getClaim("email").asString();
    }
}