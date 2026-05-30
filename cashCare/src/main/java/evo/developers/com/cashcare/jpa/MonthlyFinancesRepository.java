package evo.developers.com.cashcare.jpa;

import evo.developers.com.cashcare.entity.MonthlyFinances;
import evo.developers.com.cashcare.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonthlyFinancesRepository extends JpaRepository<MonthlyFinances, Long> {

    Optional<MonthlyFinances> findByUserAndYearAndMonth(UserEntity user, int year, int month);

    Optional<MonthlyFinances> findByIdAndUser(Long id, UserEntity user);

    List<MonthlyFinances> findAllByUserOrderByYearDescMonthDesc(UserEntity user);

    boolean existsByUserAndYearAndMonth(UserEntity user, int year, int month);
}
