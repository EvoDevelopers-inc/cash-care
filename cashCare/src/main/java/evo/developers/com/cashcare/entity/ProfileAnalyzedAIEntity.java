package evo.developers.com.cashcare.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "analyzed_profiles")
@Data
public class ProfileAnalyzedAIEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



}
