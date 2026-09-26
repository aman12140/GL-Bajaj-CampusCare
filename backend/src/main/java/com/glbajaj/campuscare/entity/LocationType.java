package com.glbajaj.campuscare.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Admin-configurable location type (Academic Block, Hostel, Library ...). First step of the location picker. */
@Entity
@Table(name = "location_types")
@Getter @Setter @NoArgsConstructor
public class LocationType {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private RecordStatus status = RecordStatus.ACTIVE;
}
