package com.example.momobe.security.infrastructure;


import com.example.momobe.common.exception.enums.ErrorCode;
import com.example.momobe.security.exception.InvalidJwtTokenException;
import com.example.momobe.security.domain.JwtTokenUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

import static com.example.momobe.security.enums.SecurityConstants.*;

@Component
public final class JwtTokenUtilImpl implements JwtTokenUtil {
    private final byte[] accessByteKey;
    private final byte[] refreshByteKey;

    public JwtTokenUtilImpl(@Value("${jwt.secretKey}") String accessByteKey, @Value("${jwt.refreshKey}") String refreshByteKey) {
        this.accessByteKey = accessByteKey.getBytes(StandardCharsets.UTF_8);
        this.refreshByteKey = refreshByteKey.getBytes(StandardCharsets.UTF_8);
    }

    public String createAccessToken(String email, Long id, List<String> roles, String nickname) {
        return createToken(email, id, roles, ACCESS_TOKEN_EXPIRE_COUNT, accessByteKey, nickname);
    }

    public String createRefreshToken(String email, Long id, List<String> roles, String nickname) {
        return createToken(email, id, roles, REFRESH_TOKEN_EXPIRE_COUNT, refreshByteKey, nickname);
    }

    public Claims parseAccessToken(String token) {
        return parseToken(accessByteKey, token);
    }

    public Claims parseRefreshToken(String token) {
        return parseToken(refreshByteKey, token);
    }

    String createToken(String email, Long id, List<String> roles, Long expire, byte[] secret, String nickname) {
        Claims claims = Jwts
                .claims()
                .setSubject(email);

        claims.put(ID, id);
        claims.put(ROLES, roles);
        claims.put(NICKNAME, nickname);

        String token = Jwts
                .builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + expire))
                .signWith(getSigningKey(secret))
                .compact();

        return JWT_PREFIX + token;
    }

    private Claims parseToken(byte[] secretByteKey, String token) {
        if (token.startsWith(JWT_PREFIX)) {
            token = token.split(" ")[1];
        }

        JwtParser parser = Jwts
                .parserBuilder()
                .setSigningKey(getSigningKey(secretByteKey))
                .build();

        Claims claims;

        try {
            claims = parser.parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException expiredJwtException) {
            throw new InvalidJwtTokenException(ErrorCode.EXPIRED_EXCEPTION);
        } catch (UnsupportedJwtException unsupportedJwtException) {
            throw new InvalidJwtTokenException(ErrorCode.UNSUPPORTED_EXCEPTION);
        } catch (MalformedJwtException malformedJwtException) {
            throw new InvalidJwtTokenException(ErrorCode.MALFORMED_EXCEPTION);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new InvalidJwtTokenException(ErrorCode.ILLEGAL_ARGUMENTS_EXCEPTION);
        } catch (SignatureException signatureException) {
            throw new InvalidJwtTokenException(ErrorCode.SIGNATURE_EXCEPTION);
        }

        if (!StringUtils.hasText(claims.getSubject()) || claims.get(ID)==null || claims.get(ROLES)==null) {
            throw new InvalidJwtTokenException(ErrorCode.MALFORMED_EXCEPTION);
        }

        return claims;
    }

    private Key getSigningKey(byte[] secret) {
        return Keys.hmacShaKeyFor(secret);
    }
}

