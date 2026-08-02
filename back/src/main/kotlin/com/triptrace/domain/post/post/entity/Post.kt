package com.triptrace.domain.post.post.entity

import com.triptrace.domain.trip.trip.entity.Trip
import com.triptrace.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import java.time.LocalDate

@Entity
class Post() : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    final lateinit var trip: Trip
        private set

    @Column(nullable = false)
    final lateinit var date: LocalDate
        private set

    @Column(length = 100, nullable = false)
    final lateinit var title: String
        private set

    @Lob
    final var memo: String? = null
        private set

    constructor(trip: Trip, date: LocalDate, title: String, memo: String?) : this() {
        this.trip = trip
        this.date = date
        this.title = title
        this.memo = memo
    }

    fun modify(
        date: LocalDate,
        title: String,
        memo: String?,
    ) {
        this.date = date
        this.title = title
        this.memo = memo
    }
}
