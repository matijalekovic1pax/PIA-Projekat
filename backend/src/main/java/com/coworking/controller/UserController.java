package com.coworking.controller;

import com.coworking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Map<String, Object> result = userService.getProfile(userDetails.getUsername());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Map<String, String> fields,
            @RequestParam(required = false) MultipartFile profilePicture) {
        try {
            Map<String, Object> result = userService.updateProfile(userDetails.getUsername(), fields, profilePicture);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "File upload failed"));
        }
    }

    @PutMapping("/reservations/{id}/cancel")
    public ResponseEntity<?> cancelReservation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        try {
            Map<String, Object> result = userService.cancelReservation(userDetails.getUsername(), id);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
