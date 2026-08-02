package com.triptrace.domain.trip.trip.repository

import com.triptrace.domain.post.post.entity.Post
import com.triptrace.domain.trip.trip.dto.TripSearchCondition
import com.triptrace.domain.trip.trip.dto.TripSearchLocation
import com.triptrace.domain.trip.trip.dto.TripSearchScope
import com.triptrace.domain.trip.trip.dto.TripSearchSort
import com.triptrace.domain.trip.trip.entity.Trip
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class TripSearchRepositoryImpl(
    private val entityManager: EntityManager,
) : TripSearchRepository {
    override fun search(
        condition: TripSearchCondition,
        pageable: Pageable,
    ): Page<Trip> {
        val cb = entityManager.criteriaBuilder
        val contentQuery = cb.createQuery(Trip::class.java)
        val root = contentQuery.from(Trip::class.java)
        root.fetch<Trip, Any>("representativeImage", JoinType.LEFT)
        contentQuery.select(root).where(*createPredicates(cb, contentQuery, root, condition)).orderBy(createOrders(cb, root, condition))
        val content =
            entityManager
                .createQuery(
                    contentQuery,
                ).setFirstResult(pageable.offset.toInt())
                .setMaxResults(pageable.pageSize)
                .resultList
        val countQuery = cb.createQuery(Long::class.java)
        val countRoot = countQuery.from(Trip::class.java)
        countQuery.select(cb.countDistinct(countRoot.get<Long>("id"))).where(*createPredicates(cb, countQuery, countRoot, condition))
        return PageImpl(content, pageable, entityManager.createQuery(countQuery).singleResult)
    }

    private fun createOrders(
        cb: CriteriaBuilder,
        trip: Root<Trip>,
        condition: TripSearchCondition,
    ): List<Order> =
        when (condition.sort) {
            TripSearchSort.LATEST -> {
                listOf(cb.desc(trip.get<Any>("createdAt")), cb.desc(trip.get<Long>("id")))
            }

            TripSearchSort.OLDEST -> {
                listOf(cb.asc(trip.get<Any>("createdAt")), cb.asc(trip.get<Long>("id")))
            }

            TripSearchSort.MOST_LIKED -> {
                listOf(
                    cb.desc(trip.get<Long>("likeCount")),
                    cb.desc(trip.get<Any>("createdAt")),
                    cb.desc(trip.get<Long>("id")),
                )
            }

            TripSearchSort.LEAST_LIKED -> {
                listOf(
                    cb.asc(trip.get<Long>("likeCount")),
                    cb.desc(trip.get<Any>("createdAt")),
                    cb.desc(trip.get<Long>("id")),
                )
            }
        }

    override fun findPublicLocations(): List<TripSearchLocation> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(TripSearchLocation::class.java)
        val trip = query.from(Trip::class.java)
        query
            .select(cb.construct(TripSearchLocation::class.java, trip.get<String>("country"), trip.get<String>("city")))
            .where(
                cb.isTrue(trip.get("visibility")),
                cb.isNotNull(trip.get<String>("country")),
                cb.notEqual(cb.trim(trip.get("country")), ""),
                cb.isNotNull(trip.get<String>("city")),
                cb.notEqual(cb.trim(trip.get("city")), ""),
            ).distinct(true)
        return entityManager.createQuery(query).resultList
    }

    private fun createPredicates(
        cb: CriteriaBuilder,
        query: CriteriaQuery<*>,
        trip: Root<Trip>,
        condition: TripSearchCondition,
    ): Array<Predicate> {
        val predicates = mutableListOf(cb.isTrue(trip.get("visibility")))
        condition.country?.let { predicates += cb.equal(cb.lower(trip.get("country")), it.lowercase()) }
        condition.city?.let { predicates += cb.equal(cb.lower(trip.get("city")), it.lowercase()) }
        condition.tokens.orEmpty().forEach { predicates += createKeywordPredicate(cb, query, trip, condition.scope, it) }
        return predicates.toTypedArray()
    }

    private fun createKeywordPredicate(
        cb: CriteriaBuilder,
        query: CriteriaQuery<*>,
        trip: Root<Trip>,
        scope: TripSearchScope,
        token: String,
    ): Predicate {
        val pattern = "%$token%"
        val tripTitle = cb.like(cb.lower(trip.get("title")), pattern)
        return when (scope) {
            TripSearchScope.TRIP_TITLE -> tripTitle
            TripSearchScope.POST_TITLE -> createPostExists(cb, query, trip, pattern, true, false)
            TripSearchScope.POST_CONTENT -> createPostExists(cb, query, trip, pattern, false, true)
            TripSearchScope.ALL -> cb.or(tripTitle, createPostExists(cb, query, trip, pattern, true, true))
        }
    }

    private fun createPostExists(
        cb: CriteriaBuilder,
        query: CriteriaQuery<*>,
        trip: Root<Trip>,
        pattern: String,
        searchTitle: Boolean,
        searchMemo: Boolean,
    ): Predicate {
        val subquery = query.subquery(Int::class.java)
        val post = subquery.from(Post::class.java)
        val keywords = mutableListOf<Predicate>()
        if (searchTitle) keywords += cb.like(cb.lower(post.get("title")), pattern)
        if (searchMemo) keywords += cb.like(cb.lower(post.get<String>("memo").`as`(String::class.java)), pattern)
        subquery.select(cb.literal(1)).where(cb.equal(post.get<Trip>("trip"), trip), cb.or(*keywords.toTypedArray()))
        return cb.exists(subquery)
    }
}
