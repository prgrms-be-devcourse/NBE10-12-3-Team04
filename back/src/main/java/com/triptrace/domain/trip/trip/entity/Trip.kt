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
class Trip : BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    lateinit var owner: Member
        protected set

    @Column(nullable = false)
    lateinit var title: String
        protected set

    var country: String? = null
        protected set

    var city: String? = null
        protected set

    var startDate: LocalDateTime? = null
        protected set

    var endDate: LocalDateTime? = null
        protected set

    @Column(nullable = false)
    private var visibility: Boolean = false

    @Column(nullable = false)
    var likeCount: Long = 0L
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "representative_image_id")
    var representativeImage: Image? = null
        protected set

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
        visibility: Boolean
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
        endDate: LocalDateTime?
    ) {
        this.country = country
        this.city = city
        this.startDate = startDate
        this.endDate = endDate
    }

    fun changeDateRange(startDate: LocalDateTime?, endDate: LocalDateTime?) {
        this.startDate = startDate
        this.endDate = endDate
    }

    constructor(
        owner: Member,
        title: String,
        country: String?,
        city: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        visibility: Boolean
    ) {
        this.owner = owner
        this.title = title
        this.country = country
        this.city = city
        this.startDate = startDate
        this.endDate = endDate
        this.visibility = visibility
    }

    constructor()
}
