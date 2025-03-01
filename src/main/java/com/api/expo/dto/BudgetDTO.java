// BudgetDTO.java
package com.api.expo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BudgetDTO {
    private String id;
    private String categoryId; // null ou vide pour budget global
    private String yearMonth; // format "yyyy-MM"
    private BigDecimal amount;
    private Integer alertThresholdPercentage = 80;
    private String notes;
}