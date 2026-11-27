package pt.ua.hipereats.adapters.out.security;

import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import pt.ua.hipereats.application.port.out.IssuedToken;
import pt.ua.hipereats.application.port.out.TokenIssuer;
import pt.ua.hipereats.domain.account.UserAccount;

/**
 * Issues a signed JWT: the subject is the email, the claim {@code role} the role. The backend
 * reads the role from the token in every request, so it does not look the account up each time.
 */
@Component
public class JwtTokenIssuer implements TokenIssuer {

    static final String ISSUER = "hipereats";

    private final JwtEncoder encoder;
    private final SecurityProperties properties;
    private final Clock clock;

    public JwtTokenIssuer(JwtEncoder encoder, SecurityProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public IssuedToken issue(UserAccount account) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.tokenValidity());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(account.email())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("role", account.role().name())
                .claim("accountId", account.id())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }
}
