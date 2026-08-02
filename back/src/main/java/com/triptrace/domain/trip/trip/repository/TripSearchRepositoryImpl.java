package com.triptrace.domain.trip.trip.repository;

import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.trip.trip.dto.TripSearchCondition;
import com.triptrace.domain.trip.trip.dto.TripSearchLocation;
import com.triptrace.domain.trip.trip.dto.TripSearchScope;
import com.triptrace.domain.trip.trip.entity.Trip;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class TripSearchRepositoryImpl implements TripSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Trip> search(TripSearchCondition condition, Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Trip> contentQuery = criteriaBuilder.createQuery(Trip.class);
        Root<Trip> contentRoot = contentQuery.from(Trip.class);
        contentRoot.fetch("representativeImage", jakarta.persistence.criteria.JoinType.LEFT);
        contentQuery
            .select(contentRoot)
            .where(createPredicates(criteriaBuilder, contentQuery, contentRoot, condition))
            .orderBy(createOrders(criteriaBuilder, contentRoot, condition));

        List<Trip> content = entityManager
            .createQuery(contentQuery)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();

        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<Trip> countRoot = countQuery.from(Trip.class);
        countQuery
            .select(criteriaBuilder.countDistinct(countRoot.get("id")))
            .where(createPredicates(criteriaBuilder, countQuery, countRoot, condition));

        long total = entityManager.createQuery(countQuery).getSingleResult();
        return new PageImpl<>(content, pageable, total);
    }

    private List<Order> createOrders(
        CriteriaBuilder criteriaBuilder,
        Root<Trip> trip,
        TripSearchCondition condition
    ) {
        return switch (condition.sort()) {
            case LATEST -> List.of(
                criteriaBuilder.desc(trip.get("createdAt")),
                criteriaBuilder.desc(trip.get("id"))
            );
            case OLDEST -> List.of(
                criteriaBuilder.asc(trip.get("createdAt")),
                criteriaBuilder.asc(trip.get("id"))
            );
            case MOST_LIKED -> List.of(
                criteriaBuilder.desc(trip.get("likeCount")),
                criteriaBuilder.desc(trip.get("createdAt")),
                criteriaBuilder.desc(trip.get("id"))
            );
            case LEAST_LIKED -> List.of(
                criteriaBuilder.asc(trip.get("likeCount")),
                criteriaBuilder.desc(trip.get("createdAt")),
                criteriaBuilder.desc(trip.get("id"))
            );
        };
    }

    @Override
    public List<TripSearchLocation> findPublicLocations() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<TripSearchLocation> query = criteriaBuilder.createQuery(TripSearchLocation.class);
        Root<Trip> trip = query.from(Trip.class);

        query
            .select(
                criteriaBuilder.construct(
                    TripSearchLocation.class,
                    trip.get("country"),
                    trip.get("city")
                )
            )
            .where(
                criteriaBuilder.isTrue(trip.get("visibility")),
                criteriaBuilder.isNotNull(trip.get("country")),
                criteriaBuilder.notEqual(criteriaBuilder.trim(trip.get("country")), ""),
                criteriaBuilder.isNotNull(trip.get("city")),
                criteriaBuilder.notEqual(criteriaBuilder.trim(trip.get("city")), "")
            )
            .distinct(true);

        return entityManager.createQuery(query).getResultList();
    }

    private Predicate[] createPredicates(
        CriteriaBuilder criteriaBuilder,
        CriteriaQuery<?> query,
        Root<Trip> trip,
        TripSearchCondition condition
    ) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.isTrue(trip.get("visibility")));

        if (condition.country() != null) {
            predicates.add(
                criteriaBuilder.equal(
                    criteriaBuilder.lower(trip.get("country")),
                    condition.country().toLowerCase()
                )
            );
        }
        if (condition.city() != null) {
            predicates.add(
                criteriaBuilder.equal(
                    criteriaBuilder.lower(trip.get("city")),
                    condition.city().toLowerCase()
                )
            );
        }

        for (String token : condition.tokens()) {
            predicates.add(createKeywordPredicate(criteriaBuilder, query, trip, condition.scope(), token));
        }
        return predicates.toArray(Predicate[]::new);
    }

    private Predicate createKeywordPredicate(
        CriteriaBuilder criteriaBuilder,
        CriteriaQuery<?> query,
        Root<Trip> trip,
        TripSearchScope scope,
        String token
    ) {
        String pattern = "%" + token + "%";
        Predicate tripTitle = criteriaBuilder.like(criteriaBuilder.lower(trip.get("title")), pattern);

        return switch (scope) {
            case TRIP_TITLE -> tripTitle;
            case POST_TITLE -> createPostExists(criteriaBuilder, query, trip, pattern, true, false);
            case POST_CONTENT -> createPostExists(criteriaBuilder, query, trip, pattern, false, true);
            case ALL -> criteriaBuilder.or(
                tripTitle,
                createPostExists(criteriaBuilder, query, trip, pattern, true, true)
            );
        };
    }

    private Predicate createPostExists(
        CriteriaBuilder criteriaBuilder,
        CriteriaQuery<?> query,
        Root<Trip> trip,
        String pattern,
        boolean searchTitle,
        boolean searchMemo
    ) {
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<Post> post = subquery.from(Post.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(post.get("trip"), trip));

        List<Predicate> keywordPredicates = new ArrayList<>();
        if (searchTitle) {
            keywordPredicates.add(
                criteriaBuilder.like(criteriaBuilder.lower(post.get("title")), pattern)
            );
        }
        if (searchMemo) {
            keywordPredicates.add(
                criteriaBuilder.like(
                    criteriaBuilder.lower(post.get("memo").as(String.class)),
                    pattern
                )
            );
        }
        predicates.add(criteriaBuilder.or(keywordPredicates.toArray(Predicate[]::new)));

        subquery.select(criteriaBuilder.literal(1)).where(predicates.toArray(Predicate[]::new));
        return criteriaBuilder.exists(subquery);
    }
}
