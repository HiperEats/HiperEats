package pt.ua.hipereats.adapters.out.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** The encoder that signs the tokens and the decoder that checks them, with the same secret. */
public final class JwtKeys {

    private JwtKeys() {
    }

    public static NimbusJwtEncoder encoder(SecurityProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key(properties)));
    }

    /** Checks the signature and the expiry of every token. */
    public static NimbusJwtDecoder decoder(SecurityProperties properties) {
        return NimbusJwtDecoder.withSecretKey(key(properties)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private static SecretKey key(SecurityProperties properties) {
        return new SecretKeySpec(properties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
