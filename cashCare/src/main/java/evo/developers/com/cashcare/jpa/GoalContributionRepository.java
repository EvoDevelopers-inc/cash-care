package evo.developers.com.cashcare.jpa;

import evo.developers.com.cashcare.entity.GoalContributionEntity;
import evo.developers.com.cashcare.entity.GoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GoalContributionRepository extends JpaRepository<GoalContributionEntity, Long> {

    List<GoalContributionEntity> findAllByGoalOrderByCreatedAtDesc(GoalEntity goal);

    Optional<GoalContributionEntity> findTopByGoalAndCreatedAtBetweenOrderByCreatedAtDesc(
            GoalEntity goal, Instant from, Instant to
    );

    void deleteByGoal(GoalEntity goal);
}
