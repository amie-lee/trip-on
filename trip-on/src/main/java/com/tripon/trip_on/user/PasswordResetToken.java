package com.tripon.trip_on.user;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class PasswordResetToken {
  @Id @GeneratedValue
  private Long id;

  @Column(nullable=false, unique=true)
  private String token;

  @OneToOne
  @JoinColumn(name="user_id", nullable=false)
  private User user;

  @Column(nullable=false)
  private LocalDateTime expiryDate;

  // 생성 시간으로부터 1시간 후 만료
  public static PasswordResetToken create(User user) {
    PasswordResetToken t = new PasswordResetToken();
    t.user = user;
    t.token = UUID.randomUUID().toString();
    t.expiryDate = LocalDateTime.now().plusHours(1);
    return t;
  }

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiryDate);
  }
}
