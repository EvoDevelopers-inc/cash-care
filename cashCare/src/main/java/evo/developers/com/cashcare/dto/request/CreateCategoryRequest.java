package evo.developers.com.cashcare.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCategoryRequest {

    @NotNull
    private Long monthlyFinancesId;

    @NotBlank
    private String nameCategory;

    private boolean required;
}
