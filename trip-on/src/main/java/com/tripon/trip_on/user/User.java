package com.tripon.trip_on.user;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name="users")
public class User {
    @Id 
    @GeneratedValue(strategy=GenerationType.IDENTITY) // DB에서 알아서 값 자동 생성 (1,2,3...)
    private long id;

    @Column(length=50)
    private String username;

    @Column(nullable=false, length=50) //nullable=false -> 필수값
    private String password;

    @Column(unique=true) // unique=true -> 한 DB에 하나만 있어야 함
    private String email;
}
