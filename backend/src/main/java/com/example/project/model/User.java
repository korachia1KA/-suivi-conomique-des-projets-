package com.example.project.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

/**
 * Entity class representing a User.
 * This class maps to the "user" table in the database and
 * contains fields for storing user details
 **/

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(name = "id_user")
    private String idUser;

    @Column(name = "email")
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "fiscal_code")
    private String fiscalCode;

    @Column(name = "id_card_number")
    private String idCardNumber;

    @Column(name = "birth_day")
    private LocalDate birthDay;

    @Column(name = "birth_place")
    private String birthPlace;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "iban")
    private String iban;

    @Column(name = "iban_holder")
    private String ibanHolder;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name="daily_working_hours", precision = 4, scale = 2)
    private BigDecimal dailyWorkingHours;

    @Column(name = "hourly_cost", precision = 10, scale = 2)
    private BigDecimal hourlyCost;

    @Column(name = "enabled")
    private Boolean enabled;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_role", referencedColumnName = "id_role")
    private Role role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_seniority_level", referencedColumnName = "id_seniority_level")
    private SeniorityLevel seniorityLevel;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_business_unit", referencedColumnName = "id_business_unit")
    private BusinessUnit businessUnit;
}