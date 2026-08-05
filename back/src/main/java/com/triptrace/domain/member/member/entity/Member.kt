package com.triptrace.domain.member.member.entity

import com.triptrace.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.ColumnDefault
import java.time.LocalDateTime

@Entity
// 같은 소셜 제공자에서 같은 계정으로 두 번 가입되지 않도록 (provider, provider_id) 조합을 유일하게 잡는다.
// LOCAL 회원은 provider_id가 null이고, DB의 UNIQUE는 null끼리 충돌시키지 않으므로 여러 건이 공존할 수 있다.
@Table(
    name = "member",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_member_provider_provider_id",
            columnNames = ["provider", "provider_id"]
        )
    ]
)
class Member : BaseEntity {

    @Column(nullable = false, unique = true)
    var email: String? = null
        protected set

    @Column(length = 50, nullable = false, unique = true)
    var username: String? = null
        protected set

    // 소셜 로그인 회원은 비밀번호가 없으므로 null을 허용한다. (LOCAL 회원은 항상 채워진다)
    @Column(length = 255)
    var passwordHash: String? = null
        protected set

    @Column(length = 500)
    var profileImageUrl: String? = null
        protected set

    @Column(length = 100)
    var intro: String? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    var status: MemberStatus = MemberStatus.ACTIVE
        protected set

    // 가입 경로. 기존 LOCAL 회원가입 생성자는 이 값을 건드리지 않고 기본값 LOCAL을 그대로 쓴다.
    // ddl-auto=update로 이미 데이터가 있는 테이블에 NOT NULL 컬럼을 추가해야 하므로 DB 기본값도 함께 지정한다.
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @ColumnDefault("'LOCAL'")
    var provider: LoginType = LoginType.LOCAL
        protected set

    // 소셜 제공자가 발급한 고유 식별자. LOCAL 회원은 null이다.
    @Column(length = 255)
    var providerId: String? = null
        protected set

    var deletedAt: LocalDateTime? = null
        protected set

    // JPA와 기존 Java 호출부가 함께 쓰던 기본 생성자. 가시성을 좁히면 호출부가 깨지므로 public을 유지한다.
    constructor()

    constructor(
        email: String?,
        username: String?,
        passwordHash: String?,
        profileImageUrl: String?,
        status: MemberStatus
    ) {
        this.email = email
        this.username = username
        this.passwordHash = passwordHash
        this.profileImageUrl = profileImageUrl
        this.status = status
    }

    // 온보딩 완료: 임시로 발급했던 닉네임을 사용자가 정한 값으로 바꾸고 정상 회원으로 전환한다.
    fun completeProfile(username: String?) {
        this.username = username
        this.status = MemberStatus.ACTIVE
    }

    // 부분 수정: null 인 값은 "변경하지 않음"으로 보고, 넘어온 값만 반영한다.
    fun modifyInfo(username: String?, intro: String?, profileImageUrl: String?) {
        if (username != null) {
            this.username = username
        }
        if (intro != null) {
            this.intro = intro
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl
        }
    }

    companion object {
        /**
         * 소셜 로그인으로 처음 들어온 회원을 만든다.
         * 비밀번호가 없으므로 passwordHash는 항상 null이고, 온보딩 전이라 status는 PENDING_PROFILE이다.
         * username은 호출하는 쪽에서 중복되지 않는 임시값을 만들어 넘겨준다.
         */
        @JvmStatic
        fun ofOAuth(
            email: String?,
            provider: LoginType,
            providerId: String?,
            username: String?,
            profileImageUrl: String?
        ): Member {
            val member = Member()
            member.email = email
            member.username = username
            member.passwordHash = null
            member.profileImageUrl = profileImageUrl
            member.status = MemberStatus.PENDING_PROFILE
            member.provider = provider
            member.providerId = providerId

            return member
        }
    }
}
