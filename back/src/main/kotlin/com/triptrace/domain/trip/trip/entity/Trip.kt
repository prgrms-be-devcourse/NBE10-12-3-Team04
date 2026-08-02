package com.triptrace.domain.trip.trip.entity

import com.triptrace.domain.image.image.entity.Image
import com.triptrace.domain.member.member.entity.Member
import com.triptrace.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalDateTime

@Entity
class Trip() : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    final lateinit var owner: Member
        private set

    @Column(nullable = false)
    final lateinit var title: String
        private set

    final var country: String? = null
        private set
    final var city: String? = null
        private set
    final var startDate: LocalDateTime? = null
        private set
    final var endDate: LocalDateTime? = null
        private set

    @Column(nullable = false)
    final var visibility: Boolean = false
        private set

    @Column(nullable = false)
    final var likeCount: Long = 0
        private set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "representative_image_id")
    final var representativeImage: Image? = null
        private set

    constructor(
        owner: Member?,
        title: String,
        country: String?,
        city: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        visibility: Boolean,
    ) : this() {
        if (owner != null) this.owner = owner
        this.title = title
        this.country = country
        this.city = city
        this.startDate = startDate
        this.endDate = endDate
        this.visibility = visibility
    }

    fun increaseLikeCount() {
        likeCount++
    }

    fun decreaseLikeCount() {
        likeCount--
    }

    fun isVisibility(): Boolean = visibility

    fun modify(
        title: String,
        country: String?,
        city: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        visibility: Boolean,
    ) {
        this.title = title
        this.country = country
        this.city = city
        this.startDate = startDate
        this.endDate = endDate
        this.visibility = visibility
    }

    fun changeRepresentativeImage(representativeImage: Image?) {
        this.representativeImage = representativeImage
    }

    fun changeAutoRecordDefaults(
        country: String?,
        city: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
    ) {
        this.country = country
        this.city = city
        this.startDate = startDate
        this.endDate = endDate
    }

    fun changeDateRange(
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
    ) {
        this.startDate = startDate
        this.endDate = endDate
    }
}
