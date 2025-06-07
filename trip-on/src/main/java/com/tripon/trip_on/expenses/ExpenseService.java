package com.tripon.trip_on.expenses;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tripon.trip_on.expenses.dto.ExpenseForm;
import com.tripon.trip_on.expenses.dto.ExpenseRequestDto;
import com.tripon.trip_on.expenses.dto.ExpenseResponseDto;
import com.tripon.trip_on.expenses.repository.ExpenseRepository;
import com.tripon.trip_on.trips.Trip;
import com.tripon.trip_on.trips.TripRepository;
import com.tripon.trip_on.user.User;
import com.tripon.trip_on.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;

    public ExpenseResponseDto createExpense(Long tripId, ExpenseRequestDto dto) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));

        User payer = userRepository.findById(dto.getPayerId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Expense expense = Expense.builder()
                .trip(trip)
                .payer(payer)
                .amount(dto.getAmount())
                .category(dto.getCategory())
                .description(dto.getDescription())
                .participants(new ArrayList<>())
                .build();

        for (Long userId : dto.getParticipantIds()) {
            User participant = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            ExpenseParticipant ep = ExpenseParticipant.builder()
                    .expense(expense)
                    .user(participant)
                    .build();
            expense.getParticipants().add(ep);
        }

        Expense saved = expenseRepository.save(expense);
        return ExpenseResponseDto.builder()
                .expenseId(saved.getId())
                .message("지출 등록 완료")
                .build();
    }

    public void saveExpenseList(Long tripId, List<ExpenseRequestDto> dtoList) {
        for (ExpenseRequestDto dto : dtoList) {
                createExpense(tripId, dto); // 기존 메서드 재사용
        }
    }

    public void saveExpenses(Long tripId, Long userId, ExpenseForm expenseForm) {
        List<ExpenseRequestDto> dtoList = expenseForm.toRequestDtoList();
        saveExpenseList(tripId, dtoList);
    }
    public List<ExpenseRequestDto> findAllByTripId(Long tripId) {
        List<Expense> expenses = expenseRepository.findAllByTripId(tripId);
        return expenses.stream().map(expense -> {
            ExpenseRequestDto dto = new ExpenseRequestDto();
            dto.setPayerId(expense.getPayer().getId());
            dto.setPayerName(expense.getPayer().getUsername());
            dto.setAmount(expense.getAmount());
            dto.setCategory(expense.getCategory());
            dto.setDescription(expense.getDescription());
            dto.setParticipantIds(
                expense.getParticipants() != null
                    ? expense.getParticipants().stream().map(p -> p.getUser().getId()).toList()
                    : List.of()
            );
            return dto;
        }).toList();
    }
}
