package com.triptrace.domain.marker.marker.repository

import com.triptrace.domain.marker.marker.entity.Marker
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface MarkerRepository : JpaRepository<Marker, Long> {
    fun findByPostId(postId: Long): Optional<Marker>

    fun findByPostIdIn(postIds: List<Long>): List<Marker>

    fun findByRepresentativeImageId(representativeImageId: Long): List<Marker>
}
