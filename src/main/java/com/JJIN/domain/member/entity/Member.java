package com.JJIN.domain.member.entity;

import com.JJIN.domain.member.entity.enums.Role;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Table(name = "member")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column
    private String nickname;

    @Column
    private String password;

    @Column
    private String socialId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public static Member create(
        final String email,
        final String nickname,
        final String password,
        final Role role
    ) {
        return Member.builder()
            .email(email)
            .nickname(nickname)
            .password(password)
            .role(role)
            .build();
    }

    public static Member createSocialMember(
        final String email,
        final String nickname,
        final String socialId,
        final Role role
    ) {
        return Member.builder()
            .email(email)
            .nickname(nickname)
            .socialId(socialId)
            .role(role)
            .build();
    }

    public void changeRole(final Role role) {
        this.role = role;
    }
}
