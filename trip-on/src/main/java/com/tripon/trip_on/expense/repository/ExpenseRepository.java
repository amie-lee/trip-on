package com.tripon.trip_on.expense.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripon.trip_on.expense.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findAllByTripId(Long tripId);

    List<Expense> findByPayerId(Long userId);
}