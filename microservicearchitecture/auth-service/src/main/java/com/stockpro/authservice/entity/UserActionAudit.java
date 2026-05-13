package com.stockpro.authservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_action_audit")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auditId;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(nullable = false, length = 160)
    private String actorEmail;

    @Column(length = 160)
    private String targetEmail;

    @Column(length = 80)
    private String targetType;

    @Column(length = 80)
    private String targetId;

    @Lob
    private String details;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
