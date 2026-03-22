package com.coworking.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "space_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private Space space;

    private String name;

    // "OFFICE" or "CONFERENCE"
    private String type;

    // For offices: desk count
    private Integer deskCount;

    // For conference rooms: equipment description
    private String equipment;
}
