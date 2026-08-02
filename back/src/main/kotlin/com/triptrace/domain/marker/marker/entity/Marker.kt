package com.triptrace.domain.marker.marker.entity

import com.triptrace.domain.image.image.entity.Image
import com.triptrace.domain.post.post.entity.Post
import com.triptrace.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
class Marker() : BaseEntity() {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, unique = true)
    final lateinit var post: Post
        private set

    @Column(precision = 10, scale = 7)
    final var centerLat: BigDecimal? = null
        private set

    @Column(precision = 10, scale = 7)
    final var centerLng: BigDecimal? = null
        private set

    @Column(length = 100)
    final var placeName: String? = null
        private set
    final var visitedAt: LocalDateTime? = null
        private set

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    final lateinit var source: MarkerSource
        private set

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "representative_image_id")
    final var representativeImage: Image? = null
        private set

    @JvmOverloads
    constructor(
        post: Post,
        centerLat: BigDecimal?,
        centerLng: BigDecimal?,
        placeName: String?,
        visitedAt: LocalDateTime?,
        source: MarkerSource,
        representativeImage: Image? = null,
    ) : this() {
        this.post = post
        this.centerLat = centerLat
        this.centerLng = centerLng
        this.placeName = placeName
        this.visitedAt = visitedAt
        this.source = source
        this.representativeImage = representativeImage
    }

    fun modify(
        centerLat: BigDecimal?,
        centerLng: BigDecimal?,
        placeName: String?,
        visitedAt: LocalDateTime?,
        source: MarkerSource,
    ) {
        this.centerLat = centerLat
        this.centerLng = centerLng
        this.placeName = placeName
        this.visitedAt = visitedAt
        this.source = source
    }

    fun changeRepresentativeImage(representativeImage: Image?) {
        this.representativeImage = representativeImage
    }
}
