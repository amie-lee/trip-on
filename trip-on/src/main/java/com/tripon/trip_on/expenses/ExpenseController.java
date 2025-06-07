package com.tripon.trip_on.expenses;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.tripon.trip_on.expenses.dto.ExpenseForm;
import com.tripon.trip_on.expenses.dto.ExpenseRequestDto;
import com.tripon.trip_on.expenses.dto.ExpenseRowDto;
import com.tripon.trip_on.trips.Trip;
import com.tripon.trip_on.trips.TripRepository;
import com.tripon.trip_on.user.User;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/trips/{tripId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final TripRepository tripRepository;
    private final ExpenseService expenseService;

    @GetMapping
    public String showExpenses(@PathVariable Long tripId, Model model) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));

        List<ExpenseRequestDto> expenses = expenseService.findAllByTripId(tripId);
        int totalAmount = expenses.stream().mapToInt(ExpenseRequestDto::getAmount).sum(); // ✅ 합계 계산

        ExpenseForm form = new ExpenseForm();
        form.setExpenseRows(List.of(new ExpenseRowDto()));

        Map<Long, String> userMap = trip.getTripMembers().stream()
            .collect(Collectors.toMap(User::getId, User::getUsername));

        model.addAttribute("expenses", expenses);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("trip", trip);
        model.addAttribute("expenseForm", form);
        model.addAttribute("users", trip.getTripMembers());
        model.addAttribute("userMap", userMap);
        return "expenses/expense-list";
    }

    @PostMapping
    public String saveExpenses(@PathVariable Long tripId,
                               @ModelAttribute ExpenseForm expenseForm,
                               BindingResult result,
                               HttpSession session,
                               Model model) {

        if (result.hasErrors()) {
            model.addAttribute("errors", result.getAllErrors());
            return "expenses/expense-list";
        }

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        expenseService.saveExpenses(tripId, userId, expenseForm);
        return "redirect:/trips/" + tripId + "/expenses";
    }
}