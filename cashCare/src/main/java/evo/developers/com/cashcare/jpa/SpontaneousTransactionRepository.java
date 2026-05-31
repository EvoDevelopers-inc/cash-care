package evo.developers.com.cashcare.jpa;

import evo.developers.com.cashcare.entity.MonthlyFinances;
import evo.developers.com.cashcare.entity.SpontaneousTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SpontaneousTransactionRepository extends JpaRepository<SpontaneousTransactionEntity, Long> {

    List<SpontaneousTransactionEntity> findAllByMonthlyFinancesOrderByCreatedAtDesc(MonthlyFinances monthlyFinances);

    List<SpontaneousTransactionEntity> findAllByMonthlyFinancesAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            MonthlyFinances monthlyFinances, Instant fromInclusive, Instant toExclusive
    );

    Optional<SpontaneousTransactionEntity> findByIdAndMonthlyFinances(Long id, MonthlyFinances monthlyFinances);
}
