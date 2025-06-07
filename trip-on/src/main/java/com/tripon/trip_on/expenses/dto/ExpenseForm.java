package com.tripon.trip_on.expenses.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExpenseForm {
    private List<ExpenseRowDto> expenseRows = new ArrayList<>();

    // 변환 메서드: 저장을 위해 ExpenseRequestDto 리스트로 변환
    public List<ExpenseRequestDto> toRequestDtoList() {
        List<ExpenseRequestDto> result = new ArrayList<>();
        for (ExpenseRowDto row : expenseRows) {
            ExpenseRequestDto dto = new ExpenseRequestDto();
            dto.setPayerId(row.getPayerId());
            dto.setParticipantIds(row.getParticipantIds());
            dto.setAmount(row.getAmount());
            dto.setCategory(row.getCategory());
            dto.setDescription(row.getDescription());
            result.add(dto);
        }
        return result;
    }
}
