package evo.developers.com.cashcare.entity;

import evo.developers.com.cashcare.model.GoalCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "goals")
@Getter
@Setter
public class GoalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "title", nullable = false, length = 80)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private GoalCategory category;

    @Column(name = "custom_emoji", length = 8)
    private String customEmoji;

    @Column(name = "target_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal targetAmount;

    @Column(name = "saved_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal savedAmount = BigDecimal.ZERO;

    @Column(name = "target_date")
    private Instant targetDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (savedAmount == null) savedAmount = BigDecimal.ZERO;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
