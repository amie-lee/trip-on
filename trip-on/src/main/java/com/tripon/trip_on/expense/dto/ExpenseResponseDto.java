package com.tripon.trip_on.expense.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExpenseResponseDto {
    private Long expenseId;
    private String message;
}
