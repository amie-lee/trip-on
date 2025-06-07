package com.tripon.trip_on.expenses;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripon.trip_on.expenses.dto.ExpenseForm;
import com.tripon.trip_on.expenses.dto.ExpenseRequestDto;
import com.tripon.trip_on.expenses.dto.ExpenseRowDto;
import com.tripon.trip_on.expenses.repository.ExpenseRepository;
import com.tripon.trip_on.split.SplitResultDto;
import com.tripon.trip_on.split.SplitService;
import com.tripon.trip_on.trips.Trip;
import com.tripon.trip_on.trips.TripMember;
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
    private final ExpenseRepository expenseRepository;
    private final SplitService splitService;

    @GetMapping
    public String showExpenses(@PathVariable Long tripId, Model model) throws JsonProcessingException {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));

        List<ExpenseRequestDto> expenses = expenseService.findAllByTripId(tripId);
        int totalAmount = expenses.stream().mapToInt(ExpenseRequestDto::getAmount).sum();

        ExpenseForm form = new ExpenseForm();
        //form.setExpenseRows(List.of(new ExpenseRowDto()));
        form.setTripId(trip.getId());

        Map<Long, String> userMap = trip.getTripMembers().stream()
            .map(TripMember::getUser)
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(User::getId, User::getUsername));

        List<User> users = trip.getTripMembers().stream()
            .map(TripMember::getUser)
            .filter(Objects::nonNull)
            .toList();

        ObjectMapper objectMapper = new ObjectMapper();
        String usersJson = objectMapper.writeValueAsString(users);

        Map<String, Integer> categoryTotals = expenses.stream()
            .collect(Collectors.groupingBy(
                ExpenseRequestDto::getCategory,
                Collectors.summingInt(ExpenseRequestDto::getAmount)
            ));
        String categoryTotalsJson = new ObjectMapper().writeValueAsString(categoryTotals);

        List<SplitResultDto> splitResults = splitService.calculateSettlement(tripId);

        model.addAttribute("categoryTotals", categoryTotals);
        model.addAttribute("categoryTotalsJson", categoryTotalsJson);
        model.addAttribute("splitResults", splitResults);

        model.addAttribute("expenses", expenses);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("trip", trip);
        model.addAttribute("expenseForm", form);
        model.addAttribute("users", users);
        model.addAttribute("userMap", userMap);
        model.addAttribute("usersJson", usersJson);
        return "expenses/expense-list";
    }

    @PostMapping
    public String saveExpenses(@PathVariable Long tripId,
                               @ModelAttribute ExpenseForm expenseForm,
                               @RequestParam(value = "toDeleteIds", required = false) List<Long> toDeleteIds,
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

        if (toDeleteIds != null) {
            for (Long id : toDeleteIds) {
                System.out.println("삭제할 ID: " + id);
                expenseRepository.deleteById(id);
            }
        }

        expenseService.saveExpenses(expenseForm, toDeleteIds);
        return "redirect:/trips/" + tripId + "/expenses";
    }
}