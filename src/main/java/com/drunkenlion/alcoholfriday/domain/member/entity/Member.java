package com.drunkenlion.alcoholfriday.domain.member.entity;

import com.drunkenlion.alcoholfriday.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {
    @Comment("회원 메일")
    @Column(unique = true, length = 50)
    private String email;

    @Comment("회원 가입 소셜 정보")
    @Column(length = 20)
    private String provider;

    @Comment("회원 본명")
    @Column(length = 50)
    private String name;

    @Comment("회원 별명")
    @Column(length = 50)
    private String nickname;

    @Comment("회원 권한")
    @Column(length = 50)
    private String role;

    @Comment("회원 연락처")
    private Long phone;

    @Comment("성인인증 날짜")
    private LocalDate certifyAt;

    @Comment("이용 약관 동의")
    private Boolean agreedToServiceUse;

    @Comment("개인정보 수집 이용 안내 동의")
    private Boolean agreedToServicePolicy;

    @Comment("개인정보 활용 동의")
    private Boolean agreedToServicePolicyUse;
}
