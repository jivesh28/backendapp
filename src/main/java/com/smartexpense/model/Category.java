package com.smartexpense.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // Comma-separated keywords e.g. "swiggy,zomato,dominos"
    @Column(columnDefinition = "TEXT")
    private String keywords;

    // SYSTEM = seeded, LLM = AI-discovered
    @Column(name = "created_by")
    private String createdBy;
}
