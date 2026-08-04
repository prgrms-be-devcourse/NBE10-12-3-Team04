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
class Marker @JvmOverloads constructor(
    post: Post,
    centerLat: BigDecimal?,
    centerLng: BigDecimal?,
    placeName: String?,
    visitedAt: LocalDateTime?,
    source: MarkerSource,
    representativeImage: Image? = null
) : BaseEntity() {

    @field:OneToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "post_id", nullable = false, unique = true)
    var post: Post = post
        protected set

    @field:Column(precision = 10, scale = 7)
    var centerLat: BigDecimal? = centerLat
        protected set

    @field:Column(precision = 10, scale = 7)
    var centerLng: BigDecimal? = centerLng
        protected set

    @field:Column(length = 100)
    var placeName: String? = placeName
        protected set

    var visitedAt: LocalDateTime? = visitedAt
        protected set

    @field:Enumerated(EnumType.STRING)
    @field:Column(length = 10, nullable = false)
    var source: MarkerSource = source
        protected set

    @field:OneToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "representative_image_id")
    var representativeImage: Image? = representativeImage
        protected set

    fun modify(
        centerLat: BigDecimal?,
        centerLng: BigDecimal?,
        placeName: String?,
        visitedAt: LocalDateTime?,
        source: MarkerSource
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
