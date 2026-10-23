package com.example.project.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity class representing a Seniority Level.
 * This class maps to the "seniority_level" table in the database
 * and contains fields for storing details about seniority levels.
 **/

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "seniority_level")
public class SeniorityLevel {

    @Id
    @Column(name = "id_seniority_level")
    private String idSeniorityLevel;

    @Column(unique = true, nullable = false)
    private String name;
}