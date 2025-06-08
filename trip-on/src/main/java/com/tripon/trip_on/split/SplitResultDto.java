package com.tripon.trip_on.split;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SplitResultDto {
    private String fromUser;
    private String toUser;
    private int amount;
}
