package com.clement.dexwin.domain.security;

import com.clement.dexwin.domain.models.User;
import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;


@Component
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final JwtEncoder jwtEncoder;

    @Override
    public String generateToken(User user) {

        String scope = user.getRoles().name();

        JwtClaimsSet claim = getClaims(user, scope);

        return this.jwtEncoder.encode(JwtEncoderParameters.from(claim)).getTokenValue();
    }


    private static JwtClaimsSet getClaims(User user, String scope) {
        Instant now = Instant.now();

        return JwtClaimsSet.builder().issuer("self")
                .issuedAt(now).expiresAt(now.plus(1, ChronoUnit.HOURS))
                .subject(getSubject(user)).claim("scope",  scope).build();
    }

    private static String getSubject(User user) {
        return user.getEmail();
    }
}