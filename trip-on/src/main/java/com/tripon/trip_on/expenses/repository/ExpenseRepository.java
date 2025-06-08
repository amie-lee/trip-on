package com.tripon.trip_on.expenses.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripon.trip_on.expenses.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findAllByTripId(Long tripId);

    List<Expense> findByPayerId(Long userId);

    void deleteByTripId(Long tripId);

    List<Expense> findByTripId(Long tripId);
}