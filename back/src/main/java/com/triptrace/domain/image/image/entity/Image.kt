package com.triptrace.domain.image.image.entity

import com.triptrace.domain.member.member.entity.Member
import com.triptrace.domain.post.post.entity.Post
import com.triptrace.domain.trip.trip.entity.Trip
import com.triptrace.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
class Image protected constructor() : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id", nullable = false)
    lateinit var owner: Member
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "trip_id", nullable = false)
    lateinit var trip: Trip
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "post_id")
    var post: Post? = null
    @Column(nullable = false) lateinit var originalFileUrl: String
    var thumbnailUrl: String? = null
    @Column(nullable = false) var fileSize: Long = 0
    @Column(length = 50, nullable = false) lateinit var mimeType: String
    @Column(precision = 10, scale = 7) var gpsLat: BigDecimal? = null
    @Column(precision = 10, scale = 7) var gpsLng: BigDecimal? = null
    var capturedAt: LocalDateTime? = null
    var deviceInfo: String? = null
    @Enumerated(EnumType.STRING) @Column(length = 10, nullable = false)
    lateinit var uploadStatus: UploadStatus

    constructor(owner: Member, trip: Trip, post: Post?, originalFileUrl: String, thumbnailUrl: String?, fileSize: Long, mimeType: String, gpsLat: BigDecimal?, gpsLng: BigDecimal?, capturedAt: LocalDateTime?, deviceInfo: String?, uploadStatus: UploadStatus) : this() {
        this.owner = owner; this.trip = trip; this.post = post; this.originalFileUrl = originalFileUrl; this.thumbnailUrl = thumbnailUrl; this.fileSize = fileSize; this.mimeType = mimeType; this.gpsLat = gpsLat; this.gpsLng = gpsLng; this.capturedAt = capturedAt; this.deviceInfo = deviceInfo; this.uploadStatus = uploadStatus
    }
    constructor(owner: Member, trip: Trip, post: Post?, originalFileUrl: String, thumbnailUrl: String?, fileSize: Long, mimeType: String, uploadStatus: UploadStatus) : this(owner, trip, post, originalFileUrl, thumbnailUrl, fileSize, mimeType, null, null, null, null, uploadStatus)

    fun modifyPost(post: Post?) { this.post = post }
    fun disconnectPost() { post = null }
    fun modifyStatus(uploadStatus: UploadStatus) { this.uploadStatus = uploadStatus }
    fun connectPost(post: Post) { this.post = post }
}
