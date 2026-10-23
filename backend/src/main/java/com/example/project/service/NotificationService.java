package com.example.project.service;

import com.example.project.DTO.notification.NotificationDto;

import java.math.BigDecimal;
import java.util.List;

public interface NotificationService {

    /** Persist + broadcast to all ADMINs and managers of the given BU. */
    void notifyProjectCreated(String idProject, String projectName, String idBusinessUnit);

    /** Persist + broadcast a critical alert when a project's margin drops below the threshold. */
    void notifyProjectCritical(String idProject,
                               String projectName,
                               String idBusinessUnit,
                               BigDecimal marginPct);

    List<NotificationDto> listForCurrentUser();

    long unreadCountForCurrentUser();

    void markOneRead(String id);

    void markAllRead();
}