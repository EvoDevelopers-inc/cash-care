package evo.developers.com.cashcare.jpa;

import evo.developers.com.cashcare.entity.CreditEntity;
import evo.developers.com.cashcare.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditRepository extends JpaRepository<CreditEntity, Long> {

    List<CreditEntity> findAllByUserOrderByIdAsc(UserEntity user);

    Optional<CreditEntity> findByIdAndUser(Long id, UserEntity user);

    void deleteByUser(UserEntity user);
}
