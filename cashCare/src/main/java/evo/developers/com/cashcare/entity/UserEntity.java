package evo.developers.com.cashcare.entity;

import evo.developers.com.cashcare.model.CitySize;
import evo.developers.com.cashcare.model.EmploymentType;
import evo.developers.com.cashcare.model.FinancialGoal;
import evo.developers.com.cashcare.model.Gender;
import evo.developers.com.cashcare.model.HousingStatus;
import evo.developers.com.cashcare.model.MaritalStatus;
import evo.developers.com.cashcare.model.SpendingStyle;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String hashPassword;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private int age;

    @Column(nullable = false)
    private Gender gender;

    @Column(name = "is_init", nullable = false, columnDefinition = "boolean default false")
    private boolean isInit = false;

    /* ───────────── Анкета (опросник для AI) ───────────── */

    @Column(name = "survey_completed", nullable = false, columnDefinition = "boolean default false")
    private boolean surveyCompleted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status", length = 32)
    private MaritalStatus maritalStatus;

    @Column(name = "children_count")
    private Integer childrenCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 32)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "housing_status", length = 32)
    private HousingStatus housingStatus;

    @Column(name = "has_debts")
    private Boolean hasDebts;

    @Enumerated(EnumType.STRING)
    @Column(name = "financial_goal", length = 32)
    private FinancialGoal financialGoal;

    @Enumerated(EnumType.STRING)
    @Column(name = "city_size", length = 32)
    private CitySize citySize;

    @Enumerated(EnumType.STRING)
    @Column(name = "spending_style", length = 32)
    private SpendingStyle spendingStyle;
}
