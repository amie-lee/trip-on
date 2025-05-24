package com.tripon.trip_on.trips;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter // ?
@Entity
public class TripTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    private String tagName;
}