package evo.developers.com.cashcare.jpa;

import evo.developers.com.cashcare.entity.CategoryEntity;
import evo.developers.com.cashcare.entity.MonthlyFinances;
import evo.developers.com.cashcare.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    List<CategoryEntity> findAllByMonthlyFinances(MonthlyFinances monthlyFinances);

    Optional<CategoryEntity> findByIdAndMonthlyFinances_User(Long id, UserEntity user);
}
