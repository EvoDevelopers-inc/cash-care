package evo.developers.com.cashcare.jpa;

import evo.developers.com.cashcare.entity.CategoryEntity;
import evo.developers.com.cashcare.entity.ExpenseEntity;
import evo.developers.com.cashcare.entity.MonthlyFinances;
import evo.developers.com.cashcare.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<ExpenseEntity, Long> {

    List<ExpenseEntity> findAllByMonthlyFinances(MonthlyFinances monthlyFinances);

    List<ExpenseEntity> findAllByCategory(CategoryEntity category);

    Optional<ExpenseEntity> findByIdAndMonthlyFinances_User(Long id, UserEntity user);
}
