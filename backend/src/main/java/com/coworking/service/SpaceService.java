package com.coworking.service;

import com.coworking.model.*;
import com.coworking.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final SpaceItemRepository spaceItemRepository;
    private final ReservationRepository reservationRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${upload.dir}")
    private String uploadDir;

    // ========== PUBLIC ENDPOINTS ==========

    public List<Map<String, Object>> getTopSpaces() {
        List<Space> spaces = spaceRepository.findTopSpaces();
        return spaces.stream().limit(6).map(this::buildSpaceSummaryMap).collect(Collectors.toList());
    }

    public List<String> getAllCities() {
        return spaceRepository.findDistinctCities();
    }

    public Map<String, Object> getSpaceSummary() {
        long total = spaceRepository.countActive();
        return Map.of("totalSpaces", total);
    }

    public List<Map<String, Object>> searchSpaces(String name, String citiesParam, String type, String capacity) {
        List<String> cities = null;
        if (citiesParam != null && !citiesParam.isEmpty()) {
            cities = Arrays.asList(citiesParam.split(","));
        }

        List<Space> spaces = spaceRepository.searchSpaces(
            (name != null && name.isEmpty()) ? null : name,
            cities
        );

        // Filter by type if specified
        if (type != null && !type.isEmpty()) {
            final String typeFilter = type;
            spaces = spaces.stream().filter(s -> {
                if ("open".equals(typeFilter)) return true;
                if ("office".equals(typeFilter)) {
                    return s.getItems().stream().anyMatch(i -> "OFFICE".equals(i.getType()));
                }
                if ("conference".equals(typeFilter)) {
                    return s.getItems().stream().anyMatch(i -> "CONFERENCE".equals(i.getType()));
                }
                return true;
            }).collect(Collectors.toList());
        }

        // Filter by capacity (min desk count for offices)
        if (capacity != null && !capacity.isEmpty() && "office".equals(type)) {
            try {
                int minCapacity = Integer.parseInt(capacity);
                spaces = spaces.stream().filter(s ->
                    s.getItems().stream()
                        .filter(i -> "OFFICE".equals(i.getType()))
                        .anyMatch(i -> i.getDeskCount() != null && i.getDeskCount() >= minCapacity)
                ).collect(Collectors.toList());
            } catch (NumberFormatException ignored) {}
        }

        return spaces.stream().map(this::buildSpaceSummaryMap).collect(Collectors.toList());
    }

    public Map<String, Object> getSpaceDetails(Long id) {
        Space space = spaceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Space not found"));
        return buildSpaceDetailMap(space);
    }

    public Map<String, Object> getAvailability(Long spaceId, String dateStr, String type, String startTimeStr, String endTimeStr) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found"));

        LocalDate date = LocalDate.parse(dateStr);
        LocalDateTime start = LocalDateTime.of(date, java.time.LocalTime.parse(startTimeStr));
        LocalDateTime end = LocalDateTime.of(date, java.time.LocalTime.parse(endTimeStr));

        List<Reservation> conflicts = reservationRepository.findConflictingReservations(space, type, start, end);

        Map<String, Object> result = new LinkedHashMap<>();

        if ("open".equals(type)) {
            int deskCount = space.getOpenSpaceDeskCount() != null ? space.getOpenSpaceDeskCount() : 5;
            long booked = conflicts.stream()
                .filter(r -> "open".equals(r.getType()))
                .count();
            result.put("available", booked < deskCount);
            result.put("deskCount", deskCount);
            result.put("bookedCount", booked);
        } else if ("office".equals(type)) {
            List<SpaceItem> offices = spaceItemRepository.findBySpaceAndType(space, "OFFICE");
            Set<String> bookedOfficeNames = conflicts.stream()
                .filter(r -> "office".equals(r.getType()))
                .map(Reservation::getItemName)
                .collect(Collectors.toSet());

            List<Map<String, Object>> officeList = offices.stream().map(o -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", o.getName());
                m.put("deskCount", o.getDeskCount());
                m.put("available", !bookedOfficeNames.contains(o.getName()));
                return m;
            }).collect(Collectors.toList());

            result.put("offices", officeList);
        } else if ("conference".equals(type)) {
            List<SpaceItem> rooms = spaceItemRepository.findBySpaceAndType(space, "CONFERENCE");
            Set<String> bookedRoomNames = conflicts.stream()
                .filter(r -> "conference".equals(r.getType()))
                .map(Reservation::getItemName)
                .collect(Collectors.toSet());

            List<Map<String, Object>> roomList = rooms.stream().map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", r.getName());
                m.put("equipment", r.getEquipment());
                m.put("available", !bookedRoomNames.contains(r.getName()));
                return m;
            }).collect(Collectors.toList());

            result.put("conferenceRooms", roomList);
        }

        return result;
    }

    public List<Map<String, Object>> getCalendarEvents(Long spaceId, String type, String itemId, String startStr, String endStr) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found"));

        LocalDateTime start = LocalDateTime.parse(startStr.substring(0, 19));
        LocalDateTime end = LocalDateTime.parse(endStr.substring(0, 19));

        List<Reservation> reservations = reservationRepository.findConflictingReservations(space, type, start, end);

        if (itemId != null && !itemId.isEmpty()) {
            reservations = reservations.stream()
                .filter(r -> itemId.equals(r.getItemName()))
                .collect(Collectors.toList());
        }

        return reservations.stream().map(r -> {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("id", r.getId());
            event.put("title", r.getType() + (r.getItemName() != null ? " - " + r.getItemName() : ""));
            event.put("start", r.getStartDateTime().toString());
            event.put("end", r.getEndDateTime().toString());
            event.put("color", "CONFIRMED".equals(r.getStatus()) ? "#16a34a" : "#0ea5e9");
            return event;
        }).collect(Collectors.toList());
    }

    // ========== MEMBER ENDPOINTS ==========

    @Transactional
    public Map<String, Object> createReservation(String username, Long spaceId, String startDateTimeStr,
                                                   String endDateTimeStr, String type, String itemId) {
        User member = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found"));

        if (!"ACTIVE".equals(space.getStatus())) {
            throw new RuntimeException("Space is not active");
        }

        LocalDateTime start = LocalDateTime.parse(startDateTimeStr);
        LocalDateTime end = LocalDateTime.parse(endDateTimeStr);

        if (!start.isBefore(end)) {
            throw new RuntimeException("Start time must be before end time");
        }

        // Check conflicts
        List<Reservation> conflicts = reservationRepository.findConflictingReservations(space, type, start, end);

        if ("open".equals(type)) {
            int deskCount = space.getOpenSpaceDeskCount() != null ? space.getOpenSpaceDeskCount() : 5;
            if (conflicts.size() >= deskCount) {
                throw new RuntimeException("No open desks available for this time slot");
            }
        } else {
            boolean conflictExists = conflicts.stream()
                .anyMatch(r -> itemId != null && itemId.equals(r.getItemName()));
            if (conflictExists) {
                throw new RuntimeException("This " + type + " is already reserved for this time slot");
            }
        }

        Reservation reservation = new Reservation();
        reservation.setSpace(space);
        reservation.setMember(member);
        reservation.setStartDateTime(start);
        reservation.setEndDateTime(end);
        reservation.setType(type);
        reservation.setItemName(itemId);
        reservation.setStatus("PENDING");
        reservation.setCreatedAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        return Map.of("message", "Reservation created successfully", "id", reservation.getId());
    }

    @Transactional
    public Map<String, Object> addReview(String username, Long spaceId, String comment, Boolean isLike) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found"));

        Feedback feedback = new Feedback();
        feedback.setSpace(space);
        feedback.setUser(user);
        feedback.setComment(comment);
        feedback.setIsLike(isLike);
        feedback.setCreatedAt(LocalDateTime.now());
        feedbackRepository.save(feedback);

        // Return updated space details
        space = spaceRepository.findById(spaceId).get();
        return buildSpaceDetailMap(space);
    }

    // ========== MANAGER ENDPOINTS ==========

    public List<Map<String, Object>> getManagerSpaces(String username) {
        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Space> spaces = spaceRepository.findByManager(manager);
        return spaces.stream().map(this::buildManagerSpaceMap).collect(Collectors.toList());
    }

    public Map<String, Object> getManagerSpace(String username, Long spaceId) {
        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found"));

        if (!space.getManager().getId().equals(manager.getId())) {
            throw new RuntimeException("Not authorized to view this space");
        }

        return buildManagerSpaceMap(space);
    }

    public List<Map<String, Object>> getManagerReservations(String username) {
        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Reservation> reservations = reservationRepository.findByManagerAndNotCancelled(manager);
        return reservations.stream()
            .sorted(Comparator.comparing(Reservation::getStartDateTime).reversed())
            .map(r -> {
                Map<String, Object> m = buildReservationMap(r);
                m.put("memberUsername", r.getMember() != null ? r.getMember().getUsername() : null);
                m.put("memberName", r.getMember() != null ?
                    r.getMember().getFirstName() + " " + r.getMember().getLastName() : null);
                return m;
            }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> confirmReservation(String username, Long reservationId) {
        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        if (!r.getSpace().getManager().getId().equals(manager.getId())) {
            throw new RuntimeException("Not authorized");
        }

        r.setStatus("CONFIRMED");
        reservationRepository.save(r);
        return Map.of("message", "Reservation confirmed");
    }

    @Transactional
    public Map<String, Object> checkoutReservation(String username, Long reservationId) {
        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        if (!r.getSpace().getManager().getId().equals(manager.getId())) {
            throw new RuntimeException("Not authorized");
        }

        // Can only checkout 10+ minutes after start
        if (LocalDateTime.now().isBefore(r.getStartDateTime().plusMinutes(10))) {
            throw new RuntimeException("Cannot checkout before 10 minutes after start time");
        }

        r.setStatus("COMPLETED");
        reservationRepository.save(r);
        return Map.of("message", "Checkout successful");
    }

    @Transactional
    public Map<String, Object> rescheduleReservation(String username, Long reservationId, String newStartStr, String newEndStr) {
        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        if (!r.getSpace().getManager().getId().equals(manager.getId())) {
            throw new RuntimeException("Not authorized");
        }

        if ("open".equals(r.getType())) {
            throw new RuntimeException("Cannot reschedule open-space reservations");
        }

        LocalDateTime newStart = LocalDateTime.parse(newStartStr.substring(0, 19));
        LocalDateTime newEnd = LocalDateTime.parse(newEndStr.substring(0, 19));

        r.setStartDateTime(newStart);
        r.setEndDateTime(newEnd);
        reservationRepository.save(r);
        return Map.of("message", "Reservation rescheduled");
    }

    @Transactional
    public Map<String, Object> createSpace(String username, Map<String, String> fields,
                                            MultipartFile mainImage, MultipartFile[] galleryImages) throws IOException {
        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Space space = new Space();
        fillSpaceFromFields(space, fields);
        space.setManager(manager);
        space.setStatus("PENDING");

        if (mainImage != null && !mainImage.isEmpty()) {
            String filename = saveFile(mainImage, "spaces");
            space.setMainImage("spaces/" + filename);
        }

        spaceRepository.save(space);

        // Save gallery images
        if (galleryImages != null) {
            int order = 0;
            for (MultipartFile img : galleryImages) {
                if (img != null && !img.isEmpty()) {
                    String filename = saveFile(img, "spaces");
                    SpaceImage si = new SpaceImage();
                    si.setSpace(space);
                    si.setImagePath("spaces/" + filename);
                    si.setDisplayOrder(order++);
                    space.getGalleryImages().add(si);
                }
            }
            spaceRepository.save(space);
        }

        // Save items (offices and conference rooms)
        saveSpaceItems(space, fields);

        return Map.of("message", "Space created", "id", space.getId());
    }

    @Transactional
    public Map<String, Object> updateSpace(String username, Long spaceId, Map<String, String> fields,
                                            MultipartFile mainImage, MultipartFile[] galleryImages) throws IOException {
        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found"));

        if (!space.getManager().getId().equals(manager.getId())) {
            throw new RuntimeException("Not authorized");
        }

        fillSpaceFromFields(space, fields);

        if (mainImage != null && !mainImage.isEmpty()) {
            String filename = saveFile(mainImage, "spaces");
            space.setMainImage("spaces/" + filename);
        }

        if (galleryImages != null && galleryImages.length > 0) {
            space.getGalleryImages().clear();
            int order = 0;
            for (MultipartFile img : galleryImages) {
                if (img != null && !img.isEmpty()) {
                    String filename = saveFile(img, "spaces");
                    SpaceImage si = new SpaceImage();
                    si.setSpace(space);
                    si.setImagePath("spaces/" + filename);
                    si.setDisplayOrder(order++);
                    space.getGalleryImages().add(si);
                }
            }
        }

        // Update items
        space.getItems().clear();
        spaceRepository.save(space);
        saveSpaceItems(space, fields);

        return Map.of("message", "Space updated");
    }

    @Transactional
    public Map<String, Object> deleteSpace(String username, Long spaceId) {
        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found"));

        if (!space.getManager().getId().equals(manager.getId())) {
            throw new RuntimeException("Not authorized");
        }

        spaceRepository.delete(space);
        return Map.of("message", "Space deleted");
    }

    @Transactional
    public Map<String, Object> importSpaces(String username, MultipartFile file) throws IOException {
        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String[] lines = content.split("\n");
        int imported = 0;

        for (int i = 1; i < lines.length; i++) { // Skip header
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split(",");
            if (parts.length < 4) continue;

            try {
                Space space = new Space();
                space.setName(parts[0].trim());
                space.setCity(parts[1].trim());
                space.setAddress(parts[2].trim());
                if (parts.length > 3 && !parts[3].trim().isEmpty()) {
                    space.setPricePerHour(new BigDecimal(parts[3].trim()));
                }
                if (parts.length > 4 && !parts[4].trim().isEmpty()) {
                    space.setDescription(parts[4].trim());
                }
                space.setManager(manager);
                space.setStatus("PENDING");
                space.setOpenSpaceDeskCount(5);
                space.setNoShowLimit(3);
                spaceRepository.save(space);
                imported++;
            } catch (Exception e) {
                // Skip invalid rows
            }
        }

        return Map.of("message", "Imported " + imported + " spaces");
    }

    public byte[] generateReport(String username, String month) throws IOException {
        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Space> spaces = spaceRepository.findByManager(manager);

        YearMonth ym = YearMonth.parse(month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);

        List<Reservation> reservations = reservationRepository.findForReport(spaces, start, end);

        // Generate PDF using PDFBox
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage();
        doc.addPage(page);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
            cs.beginText();
            cs.newLineAtOffset(50, 750);
            cs.showText("Capacity Report - " + month);
            cs.endText();

            cs.setFont(PDType1Font.HELVETICA, 10);
            cs.beginText();
            cs.newLineAtOffset(50, 720);
            cs.showText("Manager: " + manager.getFirstName() + " " + manager.getLastName());
            cs.newLineAtOffset(0, -15);
            cs.showText("Total Reservations: " + reservations.size());
            cs.newLineAtOffset(0, -25);
            cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
            cs.showText(String.format("%-30s %-15s %-15s %-10s", "Space", "Type", "Item", "Status"));
            cs.newLineAtOffset(0, -15);
            cs.setFont(PDType1Font.HELVETICA, 10);

            int y = 0;
            for (Reservation r : reservations) {
                if (y > 50) {
                    cs.endText();
                    page = new PDPage();
                    doc.addPage(page);
                    break;
                }
                String row = String.format("%-30s %-15s %-15s %-10s",
                    truncate(r.getSpace().getName(), 28),
                    r.getType(),
                    r.getItemName() != null ? truncate(r.getItemName(), 13) : "-",
                    r.getStatus()
                );
                cs.showText(row);
                cs.newLineAtOffset(0, -13);
                y++;
            }
            cs.endText();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        doc.close();
        return baos.toByteArray();
    }

    public List<Map<String, Object>> getManagerCalendarEvents(String username, String spaceIdParam,
            String elementType, String elementName, String startStr, String endStr) {
        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Space> spaces;
        if (spaceIdParam != null && !spaceIdParam.isEmpty() && !spaceIdParam.equals("all")) {
            Long spaceId = Long.parseLong(spaceIdParam);
            Space space = spaceRepository.findById(spaceId)
                    .orElseThrow(() -> new RuntimeException("Space not found"));
            spaces = List.of(space);
        } else {
            spaces = spaceRepository.findByManager(manager);
        }

        if (spaces.isEmpty()) return Collections.emptyList();

        LocalDateTime start = LocalDateTime.parse(startStr.substring(0, 19));
        LocalDateTime end = LocalDateTime.parse(endStr.substring(0, 19));

        List<Reservation> reservations = reservationRepository.findManagerCalendarReservations(spaces, start, end);

        // Filter by element type
        if (elementType != null && !elementType.isEmpty() && !elementType.equals("all")) {
            reservations = reservations.stream()
                .filter(r -> elementType.equals(r.getType()))
                .collect(Collectors.toList());
        }

        // Filter by element name
        if (elementName != null && !elementName.isEmpty()) {
            final String name = elementName;
            reservations = reservations.stream()
                .filter(r -> name.equals(r.getItemName()))
                .collect(Collectors.toList());
        }

        return reservations.stream().map(r -> {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("id", r.getId());
            event.put("title", r.getSpace().getName() + " - " + r.getType() +
                (r.getItemName() != null ? " (" + r.getItemName() + ")" : ""));
            event.put("start", r.getStartDateTime().toString());
            event.put("end", r.getEndDateTime().toString());
            event.put("color", "CONFIRMED".equals(r.getStatus()) ? "#16a34a" :
                "COMPLETED".equals(r.getStatus()) ? "#6b7280" : "#0ea5e9");
            Map<String, Object> ext = new LinkedHashMap<>();
            ext.put("status", r.getStatus());
            ext.put("spaceId", r.getSpace().getId());
            ext.put("spaceName", r.getSpace().getName());
            ext.put("type", r.getType());
            ext.put("itemName", r.getItemName());
            ext.put("reservationId", r.getId());
            event.put("extendedProps", ext);
            return event;
        }).collect(Collectors.toList());
    }

    // ========== HELPER METHODS ==========

    private void fillSpaceFromFields(Space space, Map<String, String> fields) {
        if (fields.containsKey("name")) space.setName(fields.get("name"));
        if (fields.containsKey("city")) space.setCity(fields.get("city"));
        if (fields.containsKey("address")) space.setAddress(fields.get("address"));
        if (fields.containsKey("description")) space.setDescription(fields.get("description"));
        if (fields.containsKey("pricePerHour")) {
            try { space.setPricePerHour(new BigDecimal(fields.get("pricePerHour"))); } catch (Exception ignored) {}
        }
        if (fields.containsKey("openSpaceDeskCount")) {
            try { space.setOpenSpaceDeskCount(Integer.parseInt(fields.get("openSpaceDeskCount"))); } catch (Exception ignored) {}
        }
        if (fields.containsKey("noShowLimit")) {
            try { space.setNoShowLimit(Integer.parseInt(fields.get("noShowLimit"))); } catch (Exception ignored) {}
        }
        if (fields.containsKey("latitude") && !fields.get("latitude").isEmpty()) {
            try { space.setLatitude(Double.parseDouble(fields.get("latitude"))); } catch (Exception ignored) {}
        }
        if (fields.containsKey("longitude") && !fields.get("longitude").isEmpty()) {
            try { space.setLongitude(Double.parseDouble(fields.get("longitude"))); } catch (Exception ignored) {}
        }
    }

    @SuppressWarnings("unchecked")
    private void saveSpaceItems(Space space, Map<String, String> fields) {
        // Offices
        if (fields.containsKey("offices")) {
            try {
                List<Map<String, Object>> offices = objectMapper.readValue(
                    fields.get("offices"), new TypeReference<>() {});
                for (Map<String, Object> o : offices) {
                    SpaceItem item = new SpaceItem();
                    item.setSpace(space);
                    item.setType("OFFICE");
                    item.setName((String) o.get("name"));
                    Object deskCount = o.get("deskCount");
                    if (deskCount != null) {
                        item.setDeskCount(deskCount instanceof Integer ? (Integer) deskCount :
                            Integer.parseInt(deskCount.toString()));
                    }
                    spaceItemRepository.save(item);
                }
            } catch (Exception e) {
                // Skip invalid JSON
            }
        }

        // Conference rooms
        if (fields.containsKey("conferenceRooms")) {
            try {
                List<Map<String, Object>> rooms = objectMapper.readValue(
                    fields.get("conferenceRooms"), new TypeReference<>() {});
                for (Map<String, Object> r : rooms) {
                    SpaceItem item = new SpaceItem();
                    item.setSpace(space);
                    item.setType("CONFERENCE");
                    item.setName((String) r.get("name"));
                    item.setEquipment((String) r.get("equipment"));
                    spaceItemRepository.save(item);
                }
            } catch (Exception e) {
                // Skip invalid JSON
            }
        }
    }

    public Map<String, Object> buildSpaceSummaryMap(Space space) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", space.getId());
        map.put("name", space.getName());
        map.put("city", space.getCity());
        map.put("address", space.getAddress());
        map.put("pricePerHour", space.getPricePerHour());
        map.put("mainImage", space.getMainImage());
        map.put("status", space.getStatus());

        long likes = feedbackRepository.countLikes(space);
        long dislikes = feedbackRepository.countDislikes(space);
        map.put("likes", likes);
        map.put("dislikes", dislikes);

        return map;
    }

    public Map<String, Object> buildSpaceDetailMap(Space space) {
        Map<String, Object> map = buildSpaceSummaryMap(space);
        map.put("description", space.getDescription());
        map.put("latitude", space.getLatitude());
        map.put("longitude", space.getLongitude());
        map.put("openSpaceDeskCount", space.getOpenSpaceDeskCount());
        map.put("noShowLimit", space.getNoShowLimit());

        // Gallery images
        List<String> gallery = space.getGalleryImages().stream()
            .sorted(Comparator.comparing(SpaceImage::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(SpaceImage::getImagePath)
            .collect(Collectors.toList());
        map.put("galleryImages", gallery);

        // Items
        List<Map<String, Object>> offices = space.getItems().stream()
            .filter(i -> "OFFICE".equals(i.getType()))
            .map(i -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", i.getId());
                m.put("name", i.getName());
                m.put("deskCount", i.getDeskCount());
                return m;
            }).collect(Collectors.toList());

        List<Map<String, Object>> conferenceRooms = space.getItems().stream()
            .filter(i -> "CONFERENCE".equals(i.getType()))
            .map(i -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", i.getId());
                m.put("name", i.getName());
                m.put("equipment", i.getEquipment());
                return m;
            }).collect(Collectors.toList());

        map.put("offices", offices);
        map.put("conferenceRooms", conferenceRooms);

        // Comments (feedback with non-null comments)
        List<Map<String, Object>> comments = space.getFeedbacks().stream()
            .filter(f -> f.getComment() != null && !f.getComment().isEmpty())
            .sorted(Comparator.comparing(Feedback::getCreatedAt).reversed())
            .limit(10)
            .map(f -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", f.getId());
                m.put("userId", f.getUser() != null ? f.getUser().getId() : null);
                m.put("username", f.getUser() != null ? f.getUser().getUsername() : null);
                m.put("comment", f.getComment());
                m.put("createdAt", f.getCreatedAt());
                return m;
            }).collect(Collectors.toList());

        map.put("comments", comments);

        // Manager info
        if (space.getManager() != null) {
            map.put("managerId", space.getManager().getId());
            map.put("managerName", space.getManager().getFirstName() + " " + space.getManager().getLastName());
        }

        return map;
    }

    public Map<String, Object> buildManagerSpaceMap(Space space) {
        Map<String, Object> map = buildSpaceDetailMap(space);
        return map;
    }

    private Map<String, Object> buildReservationMap(Reservation r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("spaceId", r.getSpace() != null ? r.getSpace().getId() : null);
        map.put("spaceName", r.getSpace() != null ? r.getSpace().getName() : null);
        map.put("startDateTime", r.getStartDateTime());
        map.put("endDateTime", r.getEndDateTime());
        map.put("type", r.getType());
        map.put("itemName", r.getItemName());
        map.put("status", r.getStatus());
        return map;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }
}
