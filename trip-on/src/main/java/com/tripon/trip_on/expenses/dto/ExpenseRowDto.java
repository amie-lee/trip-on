package com.tripon.trip_on.expenses.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExpenseRowDto {
    private Long payerId;
    private List<Long> participantIds;
    private int amount;
    private String category;
    private String description;
    private Long ExpenseId;
}