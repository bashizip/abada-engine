package com.abada.engine.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

public class MockJwtGenerator {

    // Use the same secret as your spring configuration
    private static final String SECRET = "this-is-a-long-and-secure-jwt-secret-key-256-bit";
    // match with app.yaml
    private static final Key KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public static String generateToken(String username, List<String> groups) {
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + (1000 * 60 * 60); // 1 hour validity

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(nowMillis))
                .setExpiration(new Date(expMillis))
                .claim("groups", groups)
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static void main(String[] args) {
        String jwt = generateToken("john", List.of("admin", "finance"));
        System.out.println("Generated JWT:\n" + jwt);
    }
}
