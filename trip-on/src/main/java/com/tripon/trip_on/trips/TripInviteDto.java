package com.tripon.trip_on.trips;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TripInviteDto {
    @NotBlank
    @Email
    private String email;
}
