package com.triptrace.domain.post.post.repository

import com.triptrace.domain.post.post.entity.Post
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface PostRepository : JpaRepository<Post, Long> {
    fun findByTripId(tripId: Long?): MutableList<Post?>?

    fun findByTripIdOrderByDateAsc(tripId: Long?): MutableList<Post?>?

    fun findByTripIdInOrderByDateAscIdAsc(tripIds: MutableList<Long?>?): MutableList<Post?>?

    fun findFirstByTripIdOrderByDateAscIdAsc(tripId: Long?): Optional<Post?>?

    fun findFirstByTripIdOrderByDateDescIdDesc(tripId: Long?): Optional<Post?>?

    @Query("select p from Post p join p.trip t join t.owner m where m.id = :ownerId order by p.date asc, p.id asc")
    fun findByOwnerId(@Param("ownerId") ownerId: Long?): MutableList<Post?>?
}
