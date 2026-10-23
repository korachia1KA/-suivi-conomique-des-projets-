package com.example.project.DTO.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
    private String id;
    private String kind;
    private String title;
    private String body;
    private String link;
    private Boolean read;
    private LocalDateTime createdAt;
}