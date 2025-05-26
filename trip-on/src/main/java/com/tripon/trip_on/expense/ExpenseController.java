package com.tripon.trip_on.expense;


import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tripon.trip_on.expense.dto.ExpenseRequestDto;
import com.tripon.trip_on.expense.dto.ExpenseResponseDto;
import com.tripon.trip_on.plan.Trip;
import com.tripon.trip_on.plan.TripRepository;
import com.tripon.trip_on.user.User;
import com.tripon.trip_on.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/trips/{tripId}")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    /**
     * 지출 등록 화면 (HTML 반환)
     */
    @GetMapping("/expenses")
    public String showExpensePage(@PathVariable Long tripId, Model model) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new IllegalArgumentException("해당 여행이 없습니다."));

        List<User> users = userRepository.findUsersByTripId(tripId); // User 엔티티 그대로 넘김

        model.addAttribute("tripTitle", trip.getTitle());
        model.addAttribute("users", users); // 💡 여기서 User 직접 넘김

        return "expense-test";
    }

    /**
     * 지출 등록 API (JSON POST)
     */
    @PostMapping("/api/expenses")
    @ResponseBody
    public ExpenseResponseDto createExpense(@PathVariable Long tripId,
                                            @RequestBody ExpenseRequestDto dto) {
        return expenseService.createExpense(tripId, dto);
    }
}
