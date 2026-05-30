package evo.developers.com.cashcare.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_category", nullable = false)
    private String nameCategory;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    @Column(name = "planned_amount", precision = 19, scale = 2)
    private BigDecimal plannedAmount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monthly_finances_id", nullable = false)
    private MonthlyFinances monthlyFinances;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseEntity> expenses = new ArrayList<>();

    public void addExpense(ExpenseEntity expense) {
        expenses.add(expense);
        expense.setCategory(this);
        expense.setMonthlyFinances(monthlyFinances);
    }
}
