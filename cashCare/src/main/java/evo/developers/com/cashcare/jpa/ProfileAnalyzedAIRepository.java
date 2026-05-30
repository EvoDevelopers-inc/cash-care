package evo.developers.com.cashcare.jpa;

import evo.developers.com.cashcare.entity.ProfileAnalyzedAIEntity;
import evo.developers.com.cashcare.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileAnalyzedAIRepository extends JpaRepository<ProfileAnalyzedAIEntity, Long> {

    Optional<ProfileAnalyzedAIEntity> findByUser(UserEntity user);
}
