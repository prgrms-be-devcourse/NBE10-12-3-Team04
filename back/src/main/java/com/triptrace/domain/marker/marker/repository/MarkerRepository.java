package com.triptrace.domain.marker.marker.repository

import com.triptrace.domain.marker.marker.entity.Marker
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface MarkerRepository : JpaRepository<Marker?, Long?> {
    fun findByPostId(postId: Long?): Optional<Marker?>?

    fun findByPostIdIn(postIds: MutableList<Long?>?): MutableList<Marker?>?

    fun findByRepresentativeImageId(representativeImageId: Long?): MutableList<Marker?>?
}
