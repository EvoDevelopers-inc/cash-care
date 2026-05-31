package evo.developers.com.cashcare.entity;

import evo.developers.com.cashcare.model.SpontaneousTransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "spontaneous_transactions")
@Getter
@Setter
public class SpontaneousTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monthly_finances_id", nullable = false)
    private MonthlyFinances monthlyFinances;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private SpontaneousTransactionType type;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "note", length = 200)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
