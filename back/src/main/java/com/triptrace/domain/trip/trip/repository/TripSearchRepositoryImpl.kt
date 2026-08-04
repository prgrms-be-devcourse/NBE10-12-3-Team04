package com.triptrace.domain.trip.trip.repository

import com.triptrace.domain.post.post.entity.Post
import com.triptrace.domain.trip.trip.dto.TripSearchCondition
import com.triptrace.domain.trip.trip.dto.TripSearchLocation
import com.triptrace.domain.trip.trip.dto.TripSearchScope
import com.triptrace.domain.trip.trip.dto.TripSearchSort
import com.triptrace.domain.trip.trip.entity.Trip
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
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
import java.util.Locale

@Repository
class TripSearchRepositoryImpl : TripSearchRepository {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    override fun search(condition: TripSearchCondition, pageable: Pageable): Page<Trip> {
        val criteriaBuilder = entityManager.criteriaBuilder

        val contentQuery = criteriaBuilder.createQuery(Trip::class.java)
        val contentRoot = contentQuery.from(Trip::class.java)
        contentRoot.fetch<Any, Any>("representativeImage", JoinType.LEFT)
        contentQuery
            .select(contentRoot)
            .where(*createPredicates(criteriaBuilder, contentQuery, contentRoot, condition))
            .orderBy(createOrders(criteriaBuilder, contentRoot, condition))

        val content = entityManager.createQuery(contentQuery)
            .setFirstResult(pageable.offset.toInt())
            .setMaxResults(pageable.pageSize)
            .resultList

        val countQuery = criteriaBuilder.createQuery(Long::class.java)
        val countRoot = countQuery.from(Trip::class.java)
        countQuery
            .select(criteriaBuilder.countDistinct(countRoot.get<Any>("id")))
            .where(*createPredicates(criteriaBuilder, countQuery, countRoot, condition))

        val total = entityManager.createQuery(countQuery).singleResult
        return PageImpl(content, pageable, total)
    }

    private fun createOrders(
        criteriaBuilder: CriteriaBuilder,
        trip: Root<Trip>,
        condition: TripSearchCondition
    ): List<Order> = when (condition.sort) {
        TripSearchSort.LATEST -> listOf(
            criteriaBuilder.desc(trip.get<Any>("createdAt")),
            criteriaBuilder.desc(trip.get<Any>("id"))
        )
        TripSearchSort.OLDEST -> listOf(
            criteriaBuilder.asc(trip.get<Any>("createdAt")),
            criteriaBuilder.asc(trip.get<Any>("id"))
        )
        TripSearchSort.MOST_LIKED -> listOf(
            criteriaBuilder.desc(trip.get<Any>("likeCount")),
            criteriaBuilder.desc(trip.get<Any>("createdAt")),
            criteriaBuilder.desc(trip.get<Any>("id"))
        )
        TripSearchSort.LEAST_LIKED -> listOf(
            criteriaBuilder.asc(trip.get<Any>("likeCount")),
            criteriaBuilder.desc(trip.get<Any>("createdAt")),
            criteriaBuilder.desc(trip.get<Any>("id"))
        )
    }

    override fun findPublicLocations(): List<TripSearchLocation> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val query = criteriaBuilder.createQuery(TripSearchLocation::class.java)
        val trip = query.from(Trip::class.java)

        query
            .select(
                criteriaBuilder.construct(
                    TripSearchLocation::class.java,
                    trip.get<Any>("country"),
                    trip.get<Any>("city")
                )
            )
            .where(
                criteriaBuilder.isTrue(trip.get("visibility")),
                criteriaBuilder.isNotNull(trip.get<Any>("country")),
                criteriaBuilder.notEqual(criteriaBuilder.trim(trip.get("country")), ""),
                criteriaBuilder.isNotNull(trip.get<Any>("city")),
                criteriaBuilder.notEqual(criteriaBuilder.trim(trip.get("city")), "")
            )
            .distinct(true)

        return entityManager.createQuery(query).resultList
    }

    private fun createPredicates(
        criteriaBuilder: CriteriaBuilder,
        query: CriteriaQuery<*>,
        trip: Root<Trip>,
        condition: TripSearchCondition
    ): Array<Predicate> {
        val predicates = mutableListOf<Predicate>()
        predicates += criteriaBuilder.isTrue(trip.get("visibility"))

        condition.country?.let {
            predicates += criteriaBuilder.equal(
                criteriaBuilder.lower(trip.get("country")),
                it.lowercase(Locale.getDefault())
            )
        }
        condition.city?.let {
            predicates += criteriaBuilder.equal(
                criteriaBuilder.lower(trip.get("city")),
                it.lowercase(Locale.getDefault())
            )
        }

        for (token in condition.tokens) {
            predicates += createKeywordPredicate(criteriaBuilder, query, trip, condition.scope, token)
        }
        return predicates.toTypedArray()
    }

    private fun createKeywordPredicate(
        criteriaBuilder: CriteriaBuilder,
        query: CriteriaQuery<*>,
        trip: Root<Trip>,
        scope: TripSearchScope,
        token: String
    ): Predicate {
        val pattern = "%$token%"
        val tripTitle = criteriaBuilder.like(criteriaBuilder.lower(trip.get("title")), pattern)

        return when (scope) {
            TripSearchScope.TRIP_TITLE -> tripTitle
            TripSearchScope.POST_TITLE -> createPostExists(criteriaBuilder, query, trip, pattern, true, false)
            TripSearchScope.POST_CONTENT -> createPostExists(criteriaBuilder, query, trip, pattern, false, true)
            TripSearchScope.ALL -> criteriaBuilder.or(
                tripTitle,
                createPostExists(criteriaBuilder, query, trip, pattern, true, true)
            )
        }
    }

    private fun createPostExists(
        criteriaBuilder: CriteriaBuilder,
        query: CriteriaQuery<*>,
        trip: Root<Trip>,
        pattern: String,
        searchTitle: Boolean,
        searchMemo: Boolean
    ): Predicate {
        val subquery = query.subquery(Int::class.java)
        val post = subquery.from(Post::class.java)
        val predicates = mutableListOf(criteriaBuilder.equal(post.get<Any>("trip"), trip))
        val keywordPredicates = mutableListOf<Predicate>()

        if (searchTitle) {
            keywordPredicates += criteriaBuilder.like(criteriaBuilder.lower(post.get("title")), pattern)
        }
        if (searchMemo) {
            keywordPredicates += criteriaBuilder.like(
                criteriaBuilder.lower(post.get<Any>("memo").`as`(String::class.java)),
                pattern
            )
        }
        predicates += criteriaBuilder.or(*keywordPredicates.toTypedArray())

        subquery.select(criteriaBuilder.literal(1)).where(*predicates.toTypedArray())
        return criteriaBuilder.exists(subquery)
    }
}
