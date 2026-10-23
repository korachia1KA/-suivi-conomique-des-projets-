package com.example.project.service;

import com.example.project.DTO.notification.NotificationDto;
import com.example.project.exception.BusinessException;
import com.example.project.model.Notification;
import com.example.project.model.User;
import com.example.project.repository.NotificationRepository;
import com.example.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final BigDecimal CRITICAL_THRESHOLD = new BigDecimal("15");

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public void notifyProjectCreated(String idProject, String projectName, String idBusinessUnit) {
        String title = "Nouveau projet : " + projectName;
        String body  = "Un nouveau projet a été créé dans votre périmètre.";
        String link  = "/projects/" + idProject;

        broadcastToRecipients(
                idBusinessUnit,
                "PROJECT_CREATED",
                title,
                body,
                link
        );
    }

    @Override
    @Transactional
    public void notifyProjectCritical(String idProject,
                                      String projectName,
                                      String idBusinessUnit,
                                      BigDecimal marginPct) {

        BigDecimal m = marginPct == null
                ? BigDecimal.ZERO
                : marginPct.setScale(2, RoundingMode.HALF_UP);

        String title = "Projet critique : " + projectName;
        String body  = "La marge est tombée à " + m.toPlainString() + " % (seuil : "
                + CRITICAL_THRESHOLD.toPlainString() + " %).";
        String link  = "/projects/" + idProject;

        broadcastToRecipients(
                idBusinessUnit,
                "PROJECT_CRITICAL",
                title,
                body,
                link
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> listForCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User me = userRepository.findByEmail(email);
        if (me == null) throw new BusinessException("USER_NOT_FOUND");

        return notificationRepository
                .findTop50ByRecipient_IdUserOrderByCreatedAtDesc(me.getIdUser())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCountForCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User me = userRepository.findByEmail(email);
        if (me == null) throw new BusinessException("USER_NOT_FOUND");
        return notificationRepository.countByRecipient_IdUserAndReadFalse(me.getIdUser());
    }

    @Override
    @Transactional
    public void markOneRead(String id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User me = userRepository.findByEmail(email);
        if (me == null) throw new BusinessException("USER_NOT_FOUND");
        notificationRepository.markOneRead(id, me.getIdUser());
    }

    @Override
    @Transactional
    public void markAllRead() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User me = userRepository.findByEmail(email);
        if (me == null) throw new BusinessException("USER_NOT_FOUND");
        notificationRepository.markAllRead(me.getIdUser());
    }

    // ------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------

    private void broadcastToRecipients(String idBusinessUnit,
                                       String kind,
                                       String title,
                                       String body,
                                       String link) {

        List<User> recipients = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null)
                .filter(u -> {
                    String role = u.getRole().getName();
                    boolean isAdmin = "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
                    if (isAdmin) return true;
                    boolean isManager = "MANAGER".equalsIgnoreCase(role) || "ROLE_MANAGER".equalsIgnoreCase(role);
                    return isManager
                            && u.getBusinessUnit() != null
                            && idBusinessUnit != null
                            && idBusinessUnit.equals(u.getBusinessUnit().getIdBusinessUnit());
                })
                .toList();

        for (User recipient : recipients) {
            Notification n = new Notification();
            n.setIdNotification(UUID.randomUUID().toString());
            n.setRecipient(recipient);
            n.setKind(kind);
            n.setTitle(title);
            n.setBody(body);
            n.setLink(link);
            n.setRead(false);
            n.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(n);

            messagingTemplate.convertAndSendToUser(
                    recipient.getEmail(),
                    "/queue/notifications",
                    toDto(n)
            );
        }
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getIdNotification(),
                n.getKind(),
                n.getTitle(),
                n.getBody(),
                n.getLink(),
                n.getRead(),
                n.getCreatedAt()
        );
    }
}