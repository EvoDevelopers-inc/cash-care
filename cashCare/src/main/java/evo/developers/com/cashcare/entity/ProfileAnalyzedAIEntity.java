package evo.developers.com.cashcare.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "analyzed_profiles")
@Getter
@Setter
public class ProfileAnalyzedAIEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(name = "personality_type")
    private String personalityType;

    @Column(name = "reasoning", columnDefinition = "TEXT")
    private String reasoning;

    @Column(name = "period_start")
    private String periodStart;

    @Column(name = "period_end")
    private String periodEnd;

    @Column(name = "currency")
    private String currency;

    @Column(name = "total_income", precision = 19, scale = 2)
    private BigDecimal totalIncome;

    @Column(name = "total_expense", precision = 19, scale = 2)
    private BigDecimal totalExpense;

    @Column(name = "raw_json", columnDefinition = "TEXT", nullable = false)
    private String rawJson;

    @Column(name = "recommended_free_pocket_pct")
    private Double recommendedFreePocketPct;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
