package com.example.project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity class representing a Status.
 * This class maps to the "status" table in the database and
 * contains fields for storing details about statuses.
 */
@Entity
@Table(name = "status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Status {

    @Id
    @Column(name = "id_status")
    private String idStatus;

    @Column(unique = true, nullable = false)
    private String name;
}