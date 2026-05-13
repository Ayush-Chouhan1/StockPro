package com.stockpro.authservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.stockpro.authservice.dto.ChangePasswordDTO;
import com.stockpro.authservice.dto.ForgotPasswordRequestDTO;
import com.stockpro.authservice.dto.LoginResponseDTO;
import com.stockpro.authservice.dto.RegisterResponseDTO;
import com.stockpro.authservice.dto.ResetPasswordRequestDTO;
import com.stockpro.authservice.dto.UpdateProfileDTO;
import com.stockpro.authservice.dto.UserRequestDTO;
import com.stockpro.authservice.dto.UserResponseDTO;
import com.stockpro.authservice.entity.User;
import com.stockpro.authservice.entity.UserRole;
import com.stockpro.authservice.exception.UserAlreadyExistsException;
import com.stockpro.authservice.repository.UserRepository;
import com.stockpro.authservice.security.JwtUtil;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private AuditService auditService;

    @Value("${google.client-id:}")
    private String googleClientId;

    @Value("${app.reset-password.frontend-url:http://localhost:3000/reset-password}")
    private String resetPasswordFrontendUrl;

    @Value("${app.reset-password.token-expiration-minutes:15}")
    private long resetPasswordTokenExpirationMinutes;

    @Value("${spring.mail.username:no-reply@stockpro.local}")
    private String mailFrom;

    public RegisterResponseDTO register(UserRequestDTO dto) {
        log.info("Registering user: {}", dto.getEmail());
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + dto.getEmail());
        }
        if (dto.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Admin accounts must be created by an Admin user");
        }
        User savedUser = saveNewUser(dto);
        log.info("User registered successfully: {}", dto.getEmail());
        audit("SELF_REGISTER", dto.getEmail(), dto.getEmail(),
                "USER", savedUser.getId().toString(), "Public registration");

        return new RegisterResponseDTO(savedUser.getId(), "User registered successfully");
    }

    public RegisterResponseDTO createInternalAccount(UserRequestDTO dto, String actorEmail) {
        log.info("Admin {} creating internal account: {}", actorEmail, dto.getEmail());
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + dto.getEmail());
        }
        User savedUser = saveNewUser(dto);
        audit("CREATE_INTERNAL_ACCOUNT", actorEmail, dto.getEmail(),
                "USER", savedUser.getId().toString(),
                "Created role " + savedUser.getRole());
        return new RegisterResponseDTO(savedUser.getId(), "Internal account created successfully");
    }

    public LoginResponseDTO login(String email, String password) {
        log.info("Login attempt: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Account is deactivated");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtUtil.generateToken(email, user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(email);

        log.info("Login successful: {}", email);
        audit("LOGIN", email, email, "USER",
                user.getId().toString(), "Email/password login");
        return new LoginResponseDTO(accessToken, refreshToken);
    }

    public LoginResponseDTO googleLogin(String idToken) {
        Map<String, Object> profile = verifyGoogleToken(idToken);
        String email = stringValue(profile.get("email"));
        boolean emailVerified = Boolean.parseBoolean(stringValue(profile.get("email_verified")));
        if (email == null || !emailVerified) {
            throw new RuntimeException("Google account email is not verified");
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User created = new User();
            created.setName(defaultString(stringValue(profile.get("name")), email));
            created.setEmail(email);
            created.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            created.setRole(UserRole.WAREHOUSE_STAFF);
            created.setDepartment("Google Workspace");
            return userRepository.save(created);
        });

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Account is deactivated");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        audit("GOOGLE_LOGIN", email, email, "USER",
                user.getId().toString(), "Google ID token login");
        return new LoginResponseDTO(
                jwtUtil.generateToken(email, user.getRole().name()),
                jwtUtil.generateRefreshToken(email));
    }

    public LoginResponseDTO refresh(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }
        String type = jwtUtil.extractType(refreshToken);
        if (!"REFRESH".equals(type)) {
            throw new RuntimeException("Not a refresh token");
        }
        String email = jwtUtil.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));


        jwtUtil.blacklistToken(refreshToken);

        String newAccessToken = jwtUtil.generateToken(email, user.getRole().name());
        String newRefreshToken = jwtUtil.generateRefreshToken(email);

        log.info("Token refreshed for: {}", email);
        return new LoginResponseDTO(newAccessToken, newRefreshToken);
    }

    public void logout(String token) {
        jwtUtil.blacklistToken(token);
        log.info("Token blacklisted - user logged out");
    }

    public UserResponseDTO getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToDTO(user);
    }

    public UserResponseDTO updateProfile(String email, UpdateProfileDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setDepartment(dto.getDepartment());
        userRepository.save(user);
        log.info("Profile updated for: {}", email);
        audit("UPDATE_PROFILE", email, email, "USER",
                user.getId().toString(), "Updated profile/contact information");
        return mapToDTO(user);
    }

    public void changePassword(String email, ChangePasswordDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for: {}", email);
        audit("CHANGE_PASSWORD", email, email, "USER",
                user.getId().toString(), "Password changed");
    }

    public void requestPasswordReset(ForgotPasswordRequestDTO dto) {
        userRepository.findByEmail(dto.getEmail()).ifPresent(user -> {
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                log.info("Password reset requested for inactive account: {}", dto.getEmail());
                return;
            }

            String token = UUID.randomUUID().toString();
            user.setResetPasswordToken(token);
            user.setResetPasswordTokenExpiresAt(
                    LocalDateTime.now().plusMinutes(resetPasswordTokenExpirationMinutes));
            userRepository.save(user);

            sendPasswordResetEmail(user, token);
            audit("REQUEST_PASSWORD_RESET", user.getEmail(), user.getEmail(),
                    "USER", user.getId().toString(), "Password reset email requested");
        });
    }

    public void resetPassword(ResetPasswordRequestDTO dto) {
        User user = userRepository.findByResetPasswordToken(dto.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        LocalDateTime expiresAt = user.getResetPasswordTokenExpiresAt();
        if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            clearPasswordResetToken(user);
            userRepository.save(user);
            throw new RuntimeException("Invalid or expired reset token");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        clearPasswordResetToken(user);
        userRepository.save(user);
        audit("RESET_PASSWORD", user.getEmail(), user.getEmail(),
                "USER", user.getId().toString(), "Password reset via email token");
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream().map(this::mapToDTO).toList();
    }

    public void deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setIsActive(false);
        userRepository.save(user);
        log.info("User deactivated: {}", id);
        audit("DEACTIVATE_USER", "ADMIN", user.getEmail(), "USER",
                user.getId().toString(), "Account deactivated");
    }

    private void audit(String action, String actorEmail, String targetEmail,
            String targetType, String targetId, String details) {
        if (auditService != null) {
            auditService.record(action, actorEmail, targetEmail, targetType,
                    targetId, details);
        }
    }

    private User saveNewUser(UserRequestDTO dto) {
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole());
        user.setDepartment(dto.getDepartment());
        return userRepository.save(user);
    }

    private void sendPasswordResetEmail(User user, String token) {
        if (mailSender == null) {
            log.warn("SMTP mail sender is not configured; password reset email not sent for {}", user.getEmail());
            return;
        }

        String resetLink = resetPasswordFrontendUrl + "?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("StockPro password reset");
        message.setText("""
                Hello %s,

                We received a request to reset your StockPro password.

                Reset your password using this link:
                %s

                This link expires in %d minutes. If you did not request this, ignore this email.

                StockPro Team
                """.formatted(user.getName(), resetLink, resetPasswordTokenExpirationMinutes));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.error("Failed to send password reset email to {}: {}", user.getEmail(), ex.getMessage());
            throw new RuntimeException("Unable to send password reset email");
        }
    }

    private void clearPasswordResetToken(User user) {
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiresAt(null);
    }

    private Map<String, Object> verifyGoogleToken(String idToken) {
        String url = UriComponentsBuilder
                .fromUriString("https://oauth2.googleapis.com/tokeninfo")
                .queryParam("id_token", idToken)
                .build()
                .toUriString();
        @SuppressWarnings("unchecked")
        Map<String, Object> response = new RestTemplate().getForObject(url, Map.class);
        if (response == null) {
            throw new RuntimeException("Unable to verify Google token");
        }
        String audience = stringValue(response.get("aud"));
        if (googleClientId != null && !googleClientId.isBlank()
                && !googleClientId.equals(audience)) {
            throw new RuntimeException("Google token audience mismatch");
        }
        return response;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private UserResponseDTO mapToDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setUserId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        dto.setDepartment(user.getDepartment());
        dto.setIsActive(user.getIsActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastLoginAt(user.getLastLoginAt());
        return dto;
    }
}
