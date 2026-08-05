package com.triptrace.domain.image.image.entity

import com.triptrace.domain.member.member.entity.Member
import com.triptrace.domain.post.post.entity.Post
import com.triptrace.domain.trip.trip.entity.Trip
import com.triptrace.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
class Image(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: Member,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    var trip: Trip,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    var post: Post?,
    @Column(nullable = false)
    var originalFileUrl: String,
    var thumbnailUrl: String?,
    @Column(nullable = false)
    var fileSize: Long,
    @Column(length = 50, nullable = false)
    var mimeType: String,
    @Column(precision = 10, scale = 7)
    var gpsLat: BigDecimal?,
    @Column(precision = 10, scale = 7)
    var gpsLng: BigDecimal?,
    var capturedAt: LocalDateTime?,
    var deviceInfo: String?,
    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    var uploadStatus: UploadStatus,
) : BaseEntity() {
    constructor(
        owner: Member,
        trip: Trip,
        post: Post?,
        originalFileUrl: String,
        thumbnailUrl: String?,
        fileSize: Long,
        mimeType: String,
        uploadStatus: UploadStatus,
    ) : this(
        owner,
        trip,
        post,
        originalFileUrl,
        thumbnailUrl,
        fileSize,
        mimeType,
        null,
        null,
        null,
        null,
        uploadStatus,
    )

    fun modifyPost(post: Post?) {
        this.post = post
    }

    fun disconnectPost() {
        post = null
    }

    fun modifyStatus(uploadStatus: UploadStatus) {
        this.uploadStatus = uploadStatus
    }

    fun connectPost(post: Post) {
        this.post = post
    }
}
