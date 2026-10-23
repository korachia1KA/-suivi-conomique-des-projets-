package com.example.project.repository;

import com.example.project.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findTop50ByRecipient_IdUserOrderByCreatedAtDesc(String idUser);

    long countByRecipient_IdUserAndReadFalse(String idUser);

    @Modifying
    @Transactional
    @Query("""
        UPDATE Notification n
        SET n.read = true
        WHERE n.recipient.idUser = :idUser
          AND n.read = false
    """)
    void markAllRead(@Param("idUser") String idUser);

    @Modifying
    @Transactional
    @Query("""
        UPDATE Notification n
        SET n.read = true
        WHERE n.idNotification = :id
          AND n.recipient.idUser = :idUser
    """)
    void markOneRead(@Param("id") String id, @Param("idUser") String idUser);
}