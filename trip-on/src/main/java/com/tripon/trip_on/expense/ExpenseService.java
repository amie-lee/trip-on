package com.tripon.trip_on.expense;

import org.springframework.stereotype.Service;

import com.tripon.trip_on.expense.dto.ExpenseRequestDto;
import com.tripon.trip_on.expense.dto.ExpenseResponseDto;
import com.tripon.trip_on.expense.repository.ExpenseRepository;
import com.tripon.trip_on.plan.Trip;
import com.tripon.trip_on.plan.TripRepository;
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
}
