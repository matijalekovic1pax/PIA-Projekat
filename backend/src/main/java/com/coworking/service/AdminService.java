package com.coworking.service;

import com.coworking.model.Space;
import com.coworking.model.User;
import com.coworking.repository.FeedbackRepository;
import com.coworking.repository.ReservationRepository;
import com.coworking.repository.SpaceRepository;
import com.coworking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final FeedbackRepository feedbackRepository;
    private final ReservationRepository reservationRepository;

    public List<Map<String, Object>> getUsers(String status) {
        List<User> users;
        if (status != null && !status.isEmpty()) {
            users = userRepository.findByStatus(status.toUpperCase());
        } else {
            // Return all non-admin users
            users = userRepository.findAll().stream()
                .filter(u -> !"admin".equals(u.getType()))
                .collect(Collectors.toList());
        }
        return users.stream().map(this::buildUserMap).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> updateUser(Long userId, Map<String, Object> fields) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (fields.containsKey("firstName")) user.setFirstName((String) fields.get("firstName"));
        if (fields.containsKey("lastName")) user.setLastName((String) fields.get("lastName"));
        if (fields.containsKey("email")) user.setEmail((String) fields.get("email"));
        if (fields.containsKey("phone")) user.setPhone((String) fields.get("phone"));
        if (fields.containsKey("status")) user.setStatus((String) fields.get("status"));
        if (fields.containsKey("companyName")) user.setCompanyName((String) fields.get("companyName"));
        if (fields.containsKey("companyAddress")) user.setCompanyAddress((String) fields.get("companyAddress"));
        if (fields.containsKey("companyRegNumber")) user.setCompanyRegNumber((String) fields.get("companyRegNumber"));
        if (fields.containsKey("companyTaxId")) user.setCompanyTaxId((String) fields.get("companyTaxId"));

        userRepository.save(user);
        return buildUserMap(user);
    }

    @Transactional
    public Map<String, Object> updateUserStatus(Long userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(status.toUpperCase());
        userRepository.save(user);
        return Map.of("message", "User status updated to " + status);
    }

    @Transactional
    public Map<String, Object> deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
        return Map.of("message", "User deleted");
    }

    public List<Map<String, Object>> getSpaces(String status) {
        List<Space> spaces;
        if (status != null && !status.isEmpty()) {
            spaces = spaceRepository.findByStatus(status.toUpperCase());
        } else {
            spaces = spaceRepository.findAll();
        }
        return spaces.stream().map(this::buildAdminSpaceMap).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> updateSpaceStatus(Long spaceId, String status) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found"));
        space.setStatus(status.toUpperCase());
        spaceRepository.save(space);
        return Map.of("message", "Space status updated to " + status);
    }

    public Map<String, Object> getStats() {
        // Popularity: likes per space
        List<Object[]> popularityData = feedbackRepository.findPopularityStats();
        List<Map<String, Object>> popularity = popularityData.stream()
            .map(row -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("spaceName", row[0]);
                m.put("likes", row[1]);
                return m;
            }).collect(Collectors.toList());

        // Revenue: price * hours per space (based on completed/confirmed reservations)
        List<Space> allSpaces = spaceRepository.findAll();
        List<Map<String, Object>> revenue = allSpaces.stream().map(space -> {
            List<com.coworking.model.Reservation> reservations = reservationRepository.findBySpace(space)
                .stream()
                .filter(r -> "COMPLETED".equals(r.getStatus()) || "CONFIRMED".equals(r.getStatus()))
                .collect(Collectors.toList());

            double totalRevenue = reservations.stream().mapToDouble(r -> {
                if (space.getPricePerHour() == null) return 0;
                long minutes = java.time.Duration.between(r.getStartDateTime(), r.getEndDateTime()).toMinutes();
                double hours = minutes / 60.0;
                return space.getPricePerHour().doubleValue() * hours;
            }).sum();

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("spaceName", space.getName());
            m.put("revenue", Math.round(totalRevenue * 100.0) / 100.0);
            return m;
        }).filter(m -> (double) m.get("revenue") > 0).collect(Collectors.toList());

        return Map.of(
            "popularity", popularity,
            "revenue", revenue
        );
    }

    private Map<String, Object> buildUserMap(User user) {
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

    private Map<String, Object> buildAdminSpaceMap(Space space) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", space.getId());
        map.put("name", space.getName());
        map.put("city", space.getCity());
        map.put("address", space.getAddress());
        map.put("status", space.getStatus());
        map.put("pricePerHour", space.getPricePerHour());
        map.put("mainImage", space.getMainImage());
        if (space.getManager() != null) {
            map.put("managerName", space.getManager().getFirstName() + " " + space.getManager().getLastName());
            map.put("managerId", space.getManager().getId());
        }
        return map;
    }
}
