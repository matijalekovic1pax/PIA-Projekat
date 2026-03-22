package com.coworking.service;

import com.coworking.model.Reservation;
import com.coworking.model.User;
import com.coworking.repository.ReservationRepository;
import com.coworking.repository.UserRepository;
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
public class UserService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${upload.dir}")
    private String uploadDir;

    public Map<String, Object> getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Reservation> reservations = reservationRepository.findByMemberOrderByStartDateTimeDesc(user);

        List<Map<String, Object>> reservationList = reservations.stream()
                .map(this::buildReservationMap)
                .toList();

        return Map.of(
            "user", buildUserMap(user),
            "reservations", reservationList
        );
    }

    @Transactional
    public Map<String, Object> updateProfile(String username, Map<String, String> fields, MultipartFile profilePicture) throws IOException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (fields.containsKey("firstName")) user.setFirstName(fields.get("firstName"));
        if (fields.containsKey("lastName")) user.setLastName(fields.get("lastName"));
        if (fields.containsKey("email")) user.setEmail(fields.get("email"));
        if (fields.containsKey("phone")) user.setPhone(fields.get("phone"));

        if ("manager".equals(user.getType())) {
            if (fields.containsKey("companyName")) user.setCompanyName(fields.get("companyName"));
            if (fields.containsKey("companyAddress")) user.setCompanyAddress(fields.get("companyAddress"));
            if (fields.containsKey("companyRegNumber")) user.setCompanyRegNumber(fields.get("companyRegNumber"));
            if (fields.containsKey("companyTaxId")) user.setCompanyTaxId(fields.get("companyTaxId"));
        }

        // Password change
        String oldPassword = fields.get("oldPassword");
        String newPassword = fields.get("newPassword");
        if (oldPassword != null && !oldPassword.isEmpty() && newPassword != null && !newPassword.isEmpty()) {
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new RuntimeException("Old password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        if (profilePicture != null && !profilePicture.isEmpty()) {
            String filename = saveFile(profilePicture, "profiles");
            user.setProfilePicture("profiles/" + filename);
        }

        userRepository.save(user);
        return buildUserMap(user);
    }

    @Transactional
    public Map<String, Object> cancelReservation(String username, Long reservationId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        if (!reservation.getMember().getId().equals(user.getId())) {
            throw new RuntimeException("Not your reservation");
        }

        // Must be 12 hours before start
        if (LocalDateTime.now().isAfter(reservation.getStartDateTime().minusHours(12))) {
            throw new RuntimeException("Cannot cancel less than 12 hours before start");
        }

        reservation.setStatus("CANCELLED");
        reservationRepository.save(reservation);
        return Map.of("message", "Reservation cancelled");
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
        map.put("companyName", user.getCompanyName());
        map.put("companyAddress", user.getCompanyAddress());
        map.put("companyRegNumber", user.getCompanyRegNumber());
        map.put("companyTaxId", user.getCompanyTaxId());
        return map;
    }

    public Map<String, Object> buildReservationMap(Reservation r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("spaceId", r.getSpace() != null ? r.getSpace().getId() : null);
        map.put("spaceName", r.getSpace() != null ? r.getSpace().getName() : null);
        map.put("spaceCity", r.getSpace() != null ? r.getSpace().getCity() : null);
        map.put("startDateTime", r.getStartDateTime());
        map.put("endDateTime", r.getEndDateTime());
        map.put("type", r.getType());
        map.put("itemName", r.getItemName());
        map.put("status", r.getStatus());
        map.put("createdAt", r.getCreatedAt());
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
