package com.triptrace.domain.image.image.repository

import com.triptrace.domain.image.image.entity.Image
import com.triptrace.domain.trip.trip.entity.Trip
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ImageRepository : JpaRepository<Image, Long> {
    fun findByTripId(tripId: Long): List<Image>
    fun findByPostId(postId: Long): List<Image>
    fun findByPostIdIn(postIds: List<Long>): List<Image>
    fun findByOwnerId(ownerId: Long): List<Image>
    fun findByOriginalFileUrl(originalFileUrl: String): java.util.Optional<Image>
    fun trip(trip: Trip): List<Image>

    @Query("""select i from Image i where i.id = :imageId and i.owner.id = :ownerId and i.trip.id = :tripId""")
    fun findByIdAndOwnerIdAndTripId(@Param("imageId") id: Long, @Param("ownerId") ownerId: Long, @Param("tripId") tripId: Long): java.util.Optional<Image>
}
