package com.tripon.trip_on.expenses.dto;

import java.util.List;

import lombok.Data;

@Data
public class ExpenseRequestDto {
    private Long tripId;
    private Long expenseId;
    private Long payerId;
    private String payerName;
    private int amount;
    private String category;
    private String description;
    private List<Long> participantIds;
}
