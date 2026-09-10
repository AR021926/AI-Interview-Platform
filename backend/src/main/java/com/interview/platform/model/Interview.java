package com.interview.platform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "interviews")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String status;

    @Column
    private Integer finalScore;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Interview() {
    }

    public Interview(String title, String role, String status, User user) {
        this.title = title;
        this.role = role;
        this.status = status;
        this.user = user;
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(Integer finalScore) {
        this.finalScore = finalScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}