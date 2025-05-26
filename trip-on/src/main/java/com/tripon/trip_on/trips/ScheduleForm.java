package com.tripon.trip_on.trips;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class ScheduleForm {
    private List<ScheduleUpdateDto> scheduleDtos = new ArrayList<>();

    public List<ScheduleUpdateDto> getScheduleDtos() {
        return scheduleDtos;
    }

    public void setScheduleDtos(List<ScheduleUpdateDto> scheduleDtos) {
        this.scheduleDtos = scheduleDtos;
    }
}