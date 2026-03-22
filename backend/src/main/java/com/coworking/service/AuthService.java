package com.coworking.service;

import com.coworking.model.PasswordResetToken;
import com.coworking.model.User;
import com.coworking.repository.PasswordResetTokenRepository;
import com.coworking.repository.UserRepository;
import com.coworking.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${upload.dir}")
    private String uploadDir;

    @Transactional
    public Map<String, Object> register(
            String username, String password, String firstName, String lastName,
            String email, String phone, String type,
            String companyName, String companyAddress, String companyRegNumber, String companyTaxId,
            MultipartFile profilePicture) throws IOException {

        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setType(type != null ? type : "member");
        user.setStatus("PENDING");

        if ("manager".equals(type)) {
            user.setCompanyName(companyName);
            user.setCompanyAddress(companyAddress);
            user.setCompanyRegNumber(companyRegNumber);
            user.setCompanyTaxId(companyTaxId);
        }

        if (profilePicture != null && !profilePicture.isEmpty()) {
            String filename = saveFile(profilePicture, "profiles");
            user.setProfilePicture("profiles/" + filename);
        }

        userRepository.save(user);
        return Map.of("message", "Registration successful. Awaiting admin approval.");
    }

    public Map<String, Object> login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("Account is not active. Status: " + user.getStatus());
        }

        String token = jwtUtil.generateToken(user.getUsername());
        return Map.of(
            "token", token,
            "user", buildUserMap(user)
        );
    }

    @Transactional
    public Map<String, Object> forgotPassword(String usernameOrEmail) {
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();
        // Delete any existing tokens
        tokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(token);
        resetToken.setExpiryDate(LocalDateTime.now().plusHours(24));
        tokenRepository.save(resetToken);

        String resetLink = "http://localhost:4200/reset-password/" + token;
        return Map.of("resetLink", resetLink, "message", "Password reset link generated");
    }

    @Transactional
    public Map<String, Object> resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new RuntimeException("Token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.delete(resetToken);

        return Map.of("message", "Password reset successful");
    }

    public Map<String, Object> buildUserMap(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("firstName", user.getFirstName());
        map.put("lastName", user.getLastName());
        map.put("phone", user.getPhone());
        map.put("type", user.getType());
        map.put("status", user.getStatus());
        map.put("profilePicture", user.getProfilePicture());
        if ("manager".equals(user.getType())) {
            map.put("companyName", user.getCompanyName());
            map.put("companyAddress", user.getCompanyAddress());
            map.put("companyRegNumber", user.getCompanyRegNumber());
            map.put("companyTaxId", user.getCompanyTaxId());
        }
        return map;
    }

    private String saveFile(MultipartFile file, String subfolder) throws IOException {
        Path dir = Paths.get(uploadDir, subfolder);
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Files.write(dir.resolve(filename), file.getBytes());
        return filename;
    }
}
