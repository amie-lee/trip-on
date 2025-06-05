package com.tripon.trip_on.user;


import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name="users") //sql 테이블 이름 설정 (여기서 해도 되고 sql에서 해도 되는 듯)
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

    @Column(name = "profile_image", nullable = false, length = 512)
    private String profileImage = "/images/tripon_profile_demo.png";

    @Column(name = "reset_token", length=64)
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

}
