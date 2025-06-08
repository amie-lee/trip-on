package com.tripon.trip_on.split;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.tripon.trip_on.expenses.Expense;
import com.tripon.trip_on.expenses.ExpenseParticipant;
import com.tripon.trip_on.expenses.repository.ExpenseRepository;
import com.tripon.trip_on.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SplitService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public List<SplitResultDto> calculateSettlement(Long tripId) {
        // 1. 모든 지출 불러오기
        List<Expense> expenses = expenseRepository.findAllByTripId(tripId);

        // 2. Map<UserId, paid> & Map<UserId, shouldPay> 계산
        Map<Long, Long> paidMap = new HashMap<>();  // userId -> total paid
        Map<Long, Long> owedMap = new HashMap<>();  // userId -> total owed
        Map<Long, String> usernameMap = new HashMap<>(); // userId -> username

        for (Expense expense : expenses) {
            Long payerId = expense.getPayer().getId();
            Long amount = (long) expense.getAmount();
            List<ExpenseParticipant> participants = expense.getParticipants();
            int numParticipants = participants.size();

            paidMap.put(payerId, paidMap.getOrDefault(payerId, 0L) + amount);
            usernameMap.put(payerId, expense.getPayer().getUsername());

            long share = amount / numParticipants;
            for (ExpenseParticipant ep : participants) {
                Long uid = ep.getUser().getId();
                owedMap.put(uid, owedMap.getOrDefault(uid, 0L) + share);
                usernameMap.put(uid, ep.getUser().getUsername());
            }
        }

        Map<Long, Long> netMap = new HashMap<>();
        for (Long userId : usernameMap.keySet()) {
            long paid = paidMap.getOrDefault(userId, 0L);
            long owed = owedMap.getOrDefault(userId, 0L);
            netMap.put(userId, paid - owed);
        }

        // split users into creditors and debtors
        PriorityQueue<UserBalance> creditors = new PriorityQueue<>((a, b) -> Long.compare(b.amount, a.amount));
        PriorityQueue<UserBalance> debtors = new PriorityQueue<>((a, b) -> Long.compare(a.amount, b.amount));

        for (Map.Entry<Long, Long> entry : netMap.entrySet()) {
            Long userId = entry.getKey();
            long balance = entry.getValue();
            if (balance > 0) {
                creditors.offer(new UserBalance(userId, balance));
            } else if (balance < 0) {
                debtors.offer(new UserBalance(userId, balance));
            }
        }

        List<SplitResultDto> results = new ArrayList<>();

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            UserBalance creditor = creditors.poll();
            UserBalance debtor = debtors.poll();

            long transfer = Math.min(creditor.amount, -debtor.amount);
            results.add(new SplitResultDto(
                    usernameMap.get(debtor.userId),
                    usernameMap.get(creditor.userId),
                    (int) transfer
            ));

            long creditorRemains = creditor.amount - transfer;
            long debtorRemains = debtor.amount + transfer;

            if (creditorRemains > 0) {
                creditors.offer(new UserBalance(creditor.userId, creditorRemains));
            }
            if (debtorRemains < 0) {
                debtors.offer(new UserBalance(debtor.userId, debtorRemains));
            }
        }

        return results;
    }

    private static class UserBalance {
        Long userId;
        long amount;

        UserBalance(Long userId, long amount) {
            this.userId = userId;
            this.amount = amount;
        }
    }
}
