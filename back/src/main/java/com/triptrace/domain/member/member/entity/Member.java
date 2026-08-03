package com.triptrace.domain.member.member.entity;

import com.triptrace.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.ColumnDefault;
import java.time.LocalDateTime;

@Entity
// 같은 소셜 제공자에서 같은 계정으로 두 번 가입되지 않도록 (provider, provider_id) 조합을 유일하게 잡는다.
// LOCAL 회원은 provider_id가 null이고, DB의 UNIQUE는 null끼리 충돌시키지 않으므로 여러 건이 공존할 수 있다.
@Table(
    name = "member",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_member_provider_provider_id",
        columnNames = {"provider", "provider_id"}
    )
)
public class Member extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 50, nullable = false, unique = true)
    private String username;

    // 소셜 로그인 회원은 비밀번호가 없으므로 null을 허용한다. (LOCAL 회원은 항상 채워진다)
    @Column(length = 255)
    private String passwordHash;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(length = 100)
    private String intro;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private MemberStatus status = MemberStatus.ACTIVE;

    // 가입 경로. 기존 LOCAL 회원가입 생성자는 이 값을 건드리지 않고 기본값 LOCAL을 그대로 쓴다.
    // ddl-auto=update로 이미 데이터가 있는 테이블에 NOT NULL 컬럼을 추가해야 하므로 DB 기본값도 함께 지정한다.
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @ColumnDefault("'LOCAL'")
    private LoginType provider = LoginType.LOCAL;

    // 소셜 제공자가 발급한 고유 식별자. LOCAL 회원은 null이다.
    @Column(length = 255)
    private String providerId;

    private LocalDateTime deletedAt;

    public Member(
        String email,
        String username,
        String passwordHash,
        String profileImageUrl,
        MemberStatus status
    ) {
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.profileImageUrl = profileImageUrl;
        this.status = status;
    }

    // 부분 수정: null 인 값은 "변경하지 않음"으로 보고, 넘어온 값만 반영한다.
    public void modifyInfo(String username, String intro, String profileImageUrl) {
        if (username != null) {
            this.username = username;
        }
        if (intro != null) {
            this.intro = intro;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    public String getEmail() {
        return this.email;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPasswordHash() {
        return this.passwordHash;
    }

    public String getProfileImageUrl() {
        return this.profileImageUrl;
    }

    public String getIntro() {
        return this.intro;
    }

    public MemberStatus getStatus() {
        return this.status;
    }

    public LoginType getProvider() {
        return this.provider;
    }

    public String getProviderId() {
        return this.providerId;
    }

    public LocalDateTime getDeletedAt() {
        return this.deletedAt;
    }

    public Member() {
    }
}
