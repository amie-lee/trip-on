package com.tripon.trip_on.trips;

import com.tripon.trip_on.user.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "trip_member")  // ✅ 실제 테이블 이름과 일치시켜야 함
public class TripMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Trip trip;

    @ManyToOne
    private User user;
}
