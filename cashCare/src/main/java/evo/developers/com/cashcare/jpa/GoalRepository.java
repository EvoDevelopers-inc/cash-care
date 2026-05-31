package evo.developers.com.cashcare.jpa;

import evo.developers.com.cashcare.entity.GoalEntity;
import evo.developers.com.cashcare.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<GoalEntity, Long> {

    List<GoalEntity> findAllByUserOrderByCreatedAtAsc(UserEntity user);

    Optional<GoalEntity> findByIdAndUser(Long id, UserEntity user);

    long countByUser(UserEntity user);
}
