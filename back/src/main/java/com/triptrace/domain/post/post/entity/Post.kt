package com.triptrace.domain.post.post.entity

import com.triptrace.domain.trip.trip.entity.Trip
import com.triptrace.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
class Post(
    trip: Trip,
    date: LocalDate,
    title: String,
    memo: String?
) : BaseEntity() {
    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "trip_id", nullable = false)
    var trip: Trip = trip
        protected set

    @field:Column(nullable = false)
    var date: LocalDate = date
        protected set

    @field:Column(length = 100, nullable = false)
    var title: String = title
        protected set

    @field:Lob // TEXT로 매핑
    var memo: String? = memo
        protected set

    fun modify(date: LocalDate, title: String, memo: String?) {
        this.date = date
        this.title = title
        this.memo = memo
    }
}
