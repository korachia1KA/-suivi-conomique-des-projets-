package com.example.project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Embeddable class representing the composite primary key for the UserProjectMonthly entity.
 * This class contains fields that uniquely identify a user's monthly record for a specific project.
 * Implements Serializable to ensure the key can be serialized.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class UserProjectMonthlyId implements Serializable {

    @Column(name = "id_user")
    private String idUser;

    @Column(name = "id_project")
    private String idProject;

    @Column(name = "date")
    private LocalDate date;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserProjectMonthlyId that)) return false;
        return Objects.equals(idUser, that.idUser)
                && Objects.equals(idProject, that.idProject)
                && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUser, idProject, date);
    }
}