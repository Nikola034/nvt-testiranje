package nvt.backend.services.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import nvt.backend.dto.user.auth.*;
import nvt.backend.exceptions.UserAuthenticationException;
import nvt.backend.model.auth.Role;
import nvt.backend.model.auth.Token;
import nvt.backend.model.user.Customer;
import nvt.backend.model.user.User;
import nvt.backend.repositories.auth.TokenRepository;
import nvt.backend.repositories.user.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenRepository tokenRepository;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private String generateSecurePassword() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public RegisterResponseDTO register(RegisterRequestDTO request) {

        if(repository.findByUsername(request.getUsername()).isPresent()) {
            return new RegisterResponseDTO(-1, "User Already Exists", null,null, null, null, null, null);
        }

        Customer user = new Customer();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setSurname(request.getSurname());
        user.setRole(Role.C);
        user.setAuthorities("CUSTOMER");


        long activationTokenExpire = 24 * 60 * 60 * 1000;
        String activationToken = jwtService.generateActivationToken(user, activationTokenExpire);
        user.setActivationToken(activationToken);
        user.setTokenExpiration(new Date(System.currentTimeMillis() + activationTokenExpire));
        user.setActive(false);

        user = repository.save(user);

        sendActivationEmail(user.getUsername(), activationToken);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        saveUserToken(accessToken, refreshToken, user);

        return new RegisterResponseDTO(user.getId(), "User Created Successfully", user.getUsername(), user.getName(), user.getSurname(),request.getOrganization(), accessToken, refreshToken);

    }

    private void sendActivationEmail(String email, String token) {
        String activationLink = "http://localhost:8080/api/v1/auth/activate?token=" + token;
        emailService.sendMail(
                "system@securely.com",
                email,
                "Activate Your Account",
                "Click the link to activate your account: \n\n" + "<a href='" + activationLink + "'> Activate" + "</a>" +
                        "\n\nThe link will expire in 24 hours."
        );
    }

    public LoginResponseDTO authenticate(LoginRequestDTO request) {
        // Lookup user by email
        User user = repository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UserAuthenticationException(
                        "Invalid email or password",
                        UserAuthenticationException.ErrorType.USER_NOT_FOUND
                ));


        // Authenticate user credentials
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
            ));
        } catch (Exception e) {
            throw new UserAuthenticationException(
                    "Invalid email or password",
                    UserAuthenticationException.ErrorType.INVALID_CREDENTIALS
            );
        }

        if(!user.isActive()){
            throw new UserAuthenticationException(

                    "Your account is not active\n",
                    UserAuthenticationException.ErrorType.ACCOUNT_NOT_VERIFIED
            );
        }

        // Generate tokens for the user
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Revoke any previous tokens
        revokeAllTokenByUser(user);

        // Save new tokens
        saveUserToken(accessToken, refreshToken, user);

        return new LoginResponseDTO(accessToken, refreshToken);
    }



    private void revokeAllTokenByUser(User user) {
        List<Token> validTokens = tokenRepository.findAllAccessTokensByUser(user.getId());
        if(validTokens.isEmpty()) {
            return;
        }  

        validTokens.forEach(t-> {
            t.setLoggedOut(true);
        });

        tokenRepository.saveAll(validTokens);
    }
    private void saveUserToken(String accessToken, String refreshToken, User user) {
        Token token = new Token();
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setLoggedOut(false);
        token.setUser(user);
        tokenRepository.save(token);
    }

    public String activate(String activationToken) {
        // Extract the username from the token
        String username = jwtService.extractUsername(activationToken);

        // Validate the token and retrieve the user
        User user = repository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));

        // Check if the token matches and has not expired
        if (!user.getActivationToken().equals(activationToken) ||
                user.getTokenExpiration().before(new Date())) {
            throw new RuntimeException("Invalid or expired activation token");
        }

        // Update user status to verified
        user.setActive(true);
        user.setActivationToken(null); // Clear the activation token
        user.setTokenExpiration(null); // Clear the token expiration
        repository.save(user);

        return "Account verified successfully!";
    }

    public ResponseEntity<AuthenticationResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthenticationResponse(null, null, "Missing or invalid Authorization header"));
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtService.extractUsername(token);

            User user = repository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (jwtService.isValidRefreshToken(token, user)) {
                String accessToken = jwtService.generateAccessToken(user);
                String refreshToken = jwtService.generateRefreshToken(user);

                revokeAllTokenByUser(user);
                saveUserToken(accessToken, refreshToken, user);

                return ResponseEntity.ok(new AuthenticationResponse(accessToken, refreshToken, "Tokens refreshed successfully"));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthenticationResponse(null, null, "Invalid refresh token"));

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthenticationResponse(null, null, "Refresh token expired"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthenticationResponse(null, null, "Token refresh failed"));
        }
    }

}


