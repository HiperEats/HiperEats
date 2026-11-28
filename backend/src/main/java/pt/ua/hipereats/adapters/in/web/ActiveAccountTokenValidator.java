package pt.ua.hipereats.adapters.in.web;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import pt.ua.hipereats.application.port.in.AccountStatusQuery;

/**
 * Refuses the token of an account that was deactivated after logging in (HE-36), so a person who
 * leaves loses access at once instead of keeping it until the token expires.
 */
@Component
public class ActiveAccountTokenValidator implements OAuth2TokenValidator<Jwt> {

    private final AccountStatusQuery accountStatus;

    public ActiveAccountTokenValidator(AccountStatusQuery accountStatus) {
        this.accountStatus = accountStatus;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (accountStatus.isActive(token.getSubject())) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "The account is no longer active", null));
    }
}
