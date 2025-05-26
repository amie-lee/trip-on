package com.tripon.trip_on.expense.dto;

import java.util.List;

import lombok.Data;

@Data
public class ExpenseRequestDto {
    private Long payerId;
    private int amount;
    private String category;
    private String description;
    private List<Long> participantIds;
}
