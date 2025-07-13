package com.expenzo.expenzobackend.service;

import com.expenzo.expenzobackend.dto.auth.*;
import com.expenzo.expenzobackend.model.User;
import com.expenzo.expenzobackend.repository.UserRepository;
import com.expenzo.expenzobackend.security.JwtUtils;
import com.expenzo.expenzobackend.security.UserDetailsImpl;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import com.nimbusds.jose.proc.SecurityContext;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.text.ParseException;
import java.util.Collections;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService{

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public JwtResponse login(LoginRequest loginRequest) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        String token = jwtUtils.generateJwtToken(auth);

        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        return JwtResponse.builder()
                .token(token)
                .username(userDetails.getUsername())
                .id(userDetails.getId())
                .build();

    }

    @Override
    public UserDto register(RegisterRequest req) {
        if (req.getProvider() == AuthProvider.EMAIL) {

            if (userRepository.existsByUsername(req.getEmail())) {
                throw new IllegalArgumentException("Username already in use");
            }
            User user = new User();
            user.setUsername(req.getEmail());
            user.setPassword(passwordEncoder.encode(req.getPassword()));
            user.setProvider(AuthProvider.EMAIL);
            User saved = userRepository.save(user);
            return new UserDto(saved.getId(), saved.getUsername());
        }

        String email = verifyOauthToken(req.getOauthToken(), req.getProvider());
        Optional<User> existing = userRepository.findByUsername(email);
        User user = existing.orElseGet(() -> {
            User u = new User();
            u.setUsername(email);
            u.setProvider(req.getProvider());
            return userRepository.save(u);
        });
        return new UserDto(user.getId(), user.getUsername());
    }

    private String verifyOauthToken(String oauthToken, AuthProvider provider) {
        switch (provider) {
            case GOOGLE:
                return verifyGoogleToken(oauthToken);
            case APPLE:
                return verifyAppleToken(oauthToken);
            default:
                throw new IllegalArgumentException("Unsupported auth provider: " + provider);
        }
    }

    private String verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList("YOUR_GOOGLE_CLIENT_ID"))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new RuntimeException("Invalid Google ID token");
            }
            GoogleIdToken.Payload payload = idToken.getPayload();

            return payload.getEmail();
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify Google ID token: " + e.getMessage(), e);
        }
    }

    private String verifyAppleToken(String jwtString) {
        try {

            URL jwkUrl = new URL("https://appleid.apple.com/auth/keys");
            JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(jwkUrl);


            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            JWSAlgorithm expectedAlg = JWSAlgorithm.RS256;
            JWSKeySelector<SecurityContext> keySelector =
                    new JWSVerificationKeySelector<>(expectedAlg, keySource);
            jwtProcessor.setJWSKeySelector(keySelector);


            SecurityContext ctx = null;
            JWTClaimsSet claims = jwtProcessor.process(jwtString, ctx);


            String email = claims.getStringClaim("email");
            if (email == null || email.isEmpty()) {
                throw new RuntimeException("Apple token does not contain email");
            }
            return email;
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify Apple ID token: " + e.getMessage(), e);
        }
    }
}
