package com.project.kiro.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "categories",
       uniqueConstraints = @UniqueConstraint(columnNames = "name_lower"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    // Stored lowercase for case-insensitive uniqueness enforcement
    @Column(name = "name_lower", nullable = false, length = 100)
    private String nameLower;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Expense> expenses;
}
