package com.uss.security.jwt;

import com.uss.service.UserDetailsImpl;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

//    token comes here with bearer , in this method we are extracting only token
    public String getJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

//    this method use to generate token
    public String generateToken(UserDetailsImpl userDetails){
        String username =userDetails.getUsername();
        String roles = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.joining(","));
        return Jwts.builder()
                .subject(username)
                .claim("userId", userDetails.getId())
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs)) // 10 hours
                .signWith(key())
                .compact();
    }
    private Key key(){
        return
                Keys.hmacShaKeyFor
                        (Decoders.BASE64.decode(jwtSecret));
    }
    public String getUsernameFromJwtToken(String token){
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build().parseSignedClaims(token)
                .getPayload().getSubject();
    }

    public Long getUserIdFromJwtToken(String token) {
        Object userId = Jwts.parser()
                .verifyWith((SecretKey) key())
                .build().parseSignedClaims(token)
                .getPayload().get("userId");
        return userId instanceof Number number ? number.longValue() : null;
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith((SecretKey) key())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
