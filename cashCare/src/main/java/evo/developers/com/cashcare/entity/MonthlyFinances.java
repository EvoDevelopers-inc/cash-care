package evo.developers.com.cashcare.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "monthly_finances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "year", "month"})
)
@Getter
@Setter
public class MonthlyFinances {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal salary = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String others;

    @OneToMany(mappedBy = "monthlyFinances", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CategoryEntity> categories = new ArrayList<>();

    @OneToMany(mappedBy = "monthlyFinances", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseEntity> expenses = new ArrayList<>();

    public void addCategory(CategoryEntity category) {
        categories.add(category);
        category.setMonthlyFinances(this);
    }

    public void addExpense(ExpenseEntity expense) {
        expenses.add(expense);
        expense.setMonthlyFinances(this);
    }
}
