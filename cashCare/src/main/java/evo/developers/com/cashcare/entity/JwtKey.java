package evo.developers.com.cashcare.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "jwt_keys")
public class JwtKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
}
