package com.coworking.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private User member;

    // For office/conference: the name of the item
    private String itemName;

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    // "open", "office", "conference"
    private String type;

    // "PENDING", "CONFIRMED", "COMPLETED", "CANCELLED"
    @Column(nullable = false)
    private String status = "PENDING";

    private LocalDateTime createdAt = LocalDateTime.now();
}
