package com.glbajaj.campuscare.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Hierarchical campus location: BUILDING -> FLOOR -> ROOM (floors and rooms are optional).
 * Example: Block A (BUILDING) -> 2nd Floor (FLOOR) -> Demo Lab A (ROOM).
 * Children inherit the "type" (location type name) of their building.
 */
@Entity
@Table(name = "locations", indexes = @Index(name = "idx_locations_parent", columnList = "parent_id"))
@Getter @Setter @NoArgsConstructor
public class Location {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    /** Location type name, e.g. "Academic Block". */
    @Column(nullable = false, length = 80)
    private String type;

    @Enumerated(EnumType.STRING) @Column(name = "location_level", nullable = false, length = 20)
    private LocationLevel level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Location parent;

    /** Floor label such as "2nd Floor" (set for FLOOR nodes and for rooms on that floor). */
    @Column(name = "floor_label", length = 40)
    private String floor;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private RecordStatus status = RecordStatus.ACTIVE;
}
