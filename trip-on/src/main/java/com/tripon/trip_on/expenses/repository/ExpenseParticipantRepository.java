package com.tripon.trip_on.expenses.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tripon.trip_on.expenses.ExpenseParticipant;

@Repository
public interface ExpenseParticipantRepository extends JpaRepository<ExpenseParticipant, Long> {
    List<ExpenseParticipant> findAllByExpenseId(Long expenseId);

    List<ExpenseParticipant> findAllByUserId(Long userId);

    void deleteByExpenseIdIn(List<Long> expenseIds);

}
