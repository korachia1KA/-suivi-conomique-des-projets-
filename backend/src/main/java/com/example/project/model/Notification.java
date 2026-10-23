package com.example.project.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Persisted in-app notification.
 * One row per recipient so we can mark read/unread per user.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification", indexes = {
        @Index(name = "idx_notif_user_read", columnList = "id_user, read_flag"),
        @Index(name = "idx_notif_created", columnList = "created_at")
})
public class Notification {

    @Id
    @Column(name = "id_notification", nullable = false)
    private String idNotification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_user", nullable = false)
    private User recipient;

    @Column(name = "kind", nullable = false, length = 64)
    private String kind;            // PROJECT_CREATED, PROJECT_CRITICAL, ...

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "body", length = 1000)
    private String body;

    @Column(name = "link", length = 500)
    private String link;            // ex: /projects/{idProject}

    @Column(name = "read_flag", nullable = false)
    private Boolean read = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}