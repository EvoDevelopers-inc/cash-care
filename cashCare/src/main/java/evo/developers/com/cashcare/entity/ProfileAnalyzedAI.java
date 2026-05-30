package evo.developers.com.cashcare.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "analyzed_profiles")
@Data
public class ProfileAnalyzedAI {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


}
