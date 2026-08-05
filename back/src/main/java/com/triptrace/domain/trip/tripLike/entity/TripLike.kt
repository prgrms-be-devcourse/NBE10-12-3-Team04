package com.triptrace.domain.trip.tripLike.entity

import com.triptrace.domain.member.member.entity.Member
import com.triptrace.domain.trip.trip.entity.Trip
import com.triptrace.global.jpa.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

// 복합 유니크 설정. 같은 사용자가 같은 여행기에 좋아요를 중복x
@Entity
@Table(
    name = "trip_like",
    uniqueConstraints = [UniqueConstraint(columnNames = ["member_id", "trip_id"])]
)
class TripLike : BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    lateinit var member: Member
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    lateinit var trip: Trip
        protected set

    constructor(member: Member, trip: Trip) {
        this.member = member
        this.trip = trip
    }
}
