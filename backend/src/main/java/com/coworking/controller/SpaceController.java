package com.coworking.controller;

import com.coworking.dto.ReservationRequest;
import com.coworking.dto.RescheduleRequest;
import com.coworking.dto.ReviewRequest;
import com.coworking.service.SpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;

    // ===== PUBLIC ENDPOINTS =====

    @GetMapping("/top")
    public ResponseEntity<List<Map<String, Object>>> getTopSpaces() {
        return ResponseEntity.ok(spaceService.getTopSpaces());
    }

    @GetMapping("/cities")
    public ResponseEntity<List<String>> getAllCities() {
        return ResponseEntity.ok(spaceService.getAllCities());
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSpaceSummary() {
        return ResponseEntity.ok(spaceService.getSpaceSummary());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchSpaces(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String cities,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String capacity) {
        return ResponseEntity.ok(spaceService.searchSpaces(name, cities, type, capacity));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSpaceDetails(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(spaceService.getSpaceDetails(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<?> getAvailability(
            @PathVariable Long id,
            @RequestParam String date,
            @RequestParam String type,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        try {
            return ResponseEntity.ok(spaceService.getAvailability(id, date, type, startTime, endTime));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/calendar")
    public ResponseEntity<?> getCalendarEvents(
            @PathVariable Long id,
            @RequestParam String type,
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) String itemId) {
        try {
            return ResponseEntity.ok(spaceService.getCalendarEvents(id, type, itemId, start, end));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ===== MEMBER ENDPOINTS =====

    @PostMapping("/reserve")
    public ResponseEntity<?> createReservation(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ReservationRequest request) {
        try {
            Map<String, Object> result = spaceService.createReservation(
                userDetails.getUsername(),
                request.getSpaceId(),
                request.getStartDateTime(),
                request.getEndDateTime(),
                request.getType(),
                request.getItemId()
            );
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/review")
    public ResponseEntity<?> addReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ReviewRequest request) {
        try {
            Map<String, Object> result = spaceService.addReview(
                userDetails.getUsername(),
                request.getSpaceId(),
                request.getComment(),
                request.getIsLike()
            );
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ===== MANAGER ENDPOINTS =====

    @GetMapping("/manager/my-spaces")
    public ResponseEntity<?> getManagerSpaces(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            return ResponseEntity.ok(spaceService.getManagerSpaces(userDetails.getUsername()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/manager/spaces/{id}")
    public ResponseEntity<?> getManagerSpace(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(spaceService.getManagerSpace(userDetails.getUsername(), id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/manager/reservations")
    public ResponseEntity<?> getManagerReservations(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            return ResponseEntity.ok(spaceService.getManagerReservations(userDetails.getUsername()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/manager/reservations/{id}/confirm")
    public ResponseEntity<?> confirmReservation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(spaceService.confirmReservation(userDetails.getUsername(), id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/manager/reservations/{id}/checkout")
    public ResponseEntity<?> checkoutReservation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(spaceService.checkoutReservation(userDetails.getUsername(), id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/manager/reservations/{id}/reschedule")
    public ResponseEntity<?> rescheduleReservation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody RescheduleRequest request) {
        try {
            return ResponseEntity.ok(spaceService.rescheduleReservation(
                userDetails.getUsername(), id, request.getStartDateTime(), request.getEndDateTime()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/manager/spaces")
    public ResponseEntity<?> createSpace(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Map<String, String> fields,
            @RequestParam(required = false) MultipartFile mainImage,
            @RequestParam(required = false) MultipartFile[] galleryImages) {
        try {
            Map<String, Object> result = spaceService.createSpace(
                userDetails.getUsername(), fields, mainImage, galleryImages);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "File upload failed"));
        }
    }

    @PutMapping("/manager/spaces/{id}")
    public ResponseEntity<?> updateSpace(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam Map<String, String> fields,
            @RequestParam(required = false) MultipartFile mainImage,
            @RequestParam(required = false) MultipartFile[] galleryImages) {
        try {
            Map<String, Object> result = spaceService.updateSpace(
                userDetails.getUsername(), id, fields, mainImage, galleryImages);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "File upload failed"));
        }
    }

    @DeleteMapping("/manager/spaces/{id}")
    public ResponseEntity<?> deleteSpace(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(spaceService.deleteSpace(userDetails.getUsername(), id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/manager/import")
    public ResponseEntity<?> importSpaces(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(spaceService.importSpaces(userDetails.getUsername(), file));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Import failed"));
        }
    }

    @GetMapping("/manager/report")
    public ResponseEntity<?> downloadReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String month) {
        try {
            byte[] pdfBytes = spaceService.generateReport(userDetails.getUsername(), month);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report_" + month + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Report generation failed"));
        }
    }

    @GetMapping("/manager/calendar")
    public ResponseEntity<?> getManagerCalendarEvents(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String spaceId,
            @RequestParam(required = false) String elementType,
            @RequestParam(required = false) String elementName,
            @RequestParam String start,
            @RequestParam String end) {
        try {
            return ResponseEntity.ok(spaceService.getManagerCalendarEvents(
                userDetails.getUsername(), spaceId, elementType, elementName, start, end));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
