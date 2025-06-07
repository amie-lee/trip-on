package com.tripon.trip_on.expenses;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public ExpenseResponseDto createExpense(ExpenseRequestDto dto) {
        Trip trip = tripRepository.findById(dto.getTripId())
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

    public void saveExpenseList(List<ExpenseRequestDto> dtoList) {
        for (ExpenseRequestDto dto : dtoList) {
            if (dto.getExpenseId() != null) {
                updateExpense(dto);  // update 분기
            } else {
                createExpense(dto);  // insert 분기
            }
        }
    }

    public void saveExpenses(ExpenseForm expenseForm, List<Long> toDeleteIds) {
        List<ExpenseRequestDto> dtoList = expenseForm.toRequestDtoList();
        if (toDeleteIds != null && !toDeleteIds.isEmpty()) {
            dtoList = dtoList.stream()
                    .filter(dto -> dto.getExpenseId() == null || !toDeleteIds.contains(dto.getExpenseId()))
                    .toList();
        }
        saveExpenseList(dtoList);
    }

    @Transactional
    public void updateExpense(ExpenseRequestDto dto) {
        // 1. 기존 expense 찾기
        Expense expense = expenseRepository.findById(dto.getExpenseId())
                .orElseThrow(() -> new IllegalArgumentException("Expense not found with ID: " + dto.getExpenseId()));

        // 2. 연관된 trip, payer 찾기
        Trip trip = tripRepository.findById(dto.getTripId())
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));

        User payer = userRepository.findById(dto.getPayerId())
                .orElseThrow(() -> new IllegalArgumentException("Payer not found"));

        // 3. 값 업데이트
        expense.setTrip(trip);
        expense.setPayer(payer);
        expense.setAmount(dto.getAmount());
        expense.setCategory(dto.getCategory());
        expense.setDescription(dto.getDescription());

        // 4. 기존 참여자 제거 및 새로 설정
        expense.getParticipants().clear();

        List<ExpenseParticipant> newParticipants = dto.getParticipantIds().stream()
            .map(userId -> {
                User participant = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Participant not found"));
                return ExpenseParticipant.builder()
                        .expense(expense)
                        .user(participant)
                        .build();
            }).toList();

        expense.getParticipants().addAll(newParticipants);

        // 5. 저장 (생략 가능 – @Transactional 이 처리)
        expenseRepository.save(expense);
    }
    
    public List<ExpenseRequestDto> findAllByTripId(Long tripId) {
        List<Expense> expenses = expenseRepository.findAllByTripId(tripId);
        return expenses.stream().map(expense -> {
            ExpenseRequestDto dto = new ExpenseRequestDto();
            dto.setTripId(tripId);
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
            dto.setExpenseId(expense.getId());
            return dto;
        }).toList();
    }
}
