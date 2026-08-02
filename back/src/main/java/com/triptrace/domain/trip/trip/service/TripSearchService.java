package com.triptrace.domain.trip.trip.service;

import com.triptrace.domain.trip.trip.dto.TripSearchCondition;
import com.triptrace.domain.trip.trip.dto.TripSearchLocationResponse;
import com.triptrace.domain.trip.trip.dto.TripSearchResponse;
import com.triptrace.domain.trip.trip.dto.TripSearchScope;
import com.triptrace.domain.trip.trip.dto.TripSearchSort;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.error.TripErrorCode;
import com.triptrace.domain.trip.trip.repository.TripSearchRepository;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.global.exception.ServiceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class TripSearchService {

    private static final int PREVIEW_MAX_LENGTH = 100;

    private final TripSearchRepository tripSearchRepository;
    private final PostRepository postRepository;

    public TripSearchService(
        final TripSearchRepository tripSearchRepository,
        final PostRepository postRepository
    ) {
        this.tripSearchRepository = tripSearchRepository;
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public Page<TripSearchResponse> search(
        String keyword,
        TripSearchScope scope,
        String country,
        String city,
        TripSearchSort sort,
        Pageable pageable
    ) {
        String normalizedCountry = normalizeFilter(country);
        String normalizedCity = normalizeFilter(city);
        if (normalizedCity != null && normalizedCountry == null) {
            throw new ServiceException(TripErrorCode.CITY_REQUIRES_COUNTRY);
        }

        TripSearchCondition condition = new TripSearchCondition(
            tokenize(keyword),
            scope == null ? TripSearchScope.ALL : scope,
            normalizedCountry,
            normalizedCity,
            sort == null ? TripSearchSort.LATEST : sort
        );
        Page<Trip> trips = tripSearchRepository.search(condition, pageable);
        Map<Long, List<Post>> postsByTripId = findPostsByTripId(trips.getContent());

        return trips.map(trip -> toResponse(trip, postsByTripId.getOrDefault(trip.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public List<TripSearchLocationResponse> findLocations() {
        Map<String, TreeSet<String>> citiesByCountry = new TreeMap<>();
        tripSearchRepository.findPublicLocations().forEach(location -> {
            String country = normalizeFilter(location.country());
            String city = normalizeFilter(location.city());
            if (country == null || city == null) {
                return;
            }
            citiesByCountry
                .computeIfAbsent(country, ignored -> new TreeSet<>())
                .add(city);
        });

        return citiesByCountry.entrySet()
            .stream()
            .map(entry -> new TripSearchLocationResponse(entry.getKey(), List.copyOf(entry.getValue())))
            .toList();
    }

    private List<String> tokenize(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }

        String normalized = keyword
            .replaceAll("[%_\\\\]", " ")
            .trim()
            .toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> tokens = new LinkedHashSet<>(List.of(normalized.split("\\s+")));
        return List.copyOf(tokens);
    }

    private String normalizeFilter(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Map<Long, List<Post>> findPostsByTripId(List<Trip> trips) {
        if (trips.isEmpty()) {
            return Map.of();
        }

        List<Long> tripIds = trips.stream().map(Trip::getId).toList();
        return postRepository.findByTripIdInOrderByDateAscIdAsc(tripIds)
            .stream()
            .collect(Collectors.groupingBy(post -> post.getTrip().getId()));
    }

    private TripSearchResponse toResponse(Trip trip, List<Post> posts) {
        String thumbnailUrl = trip.getRepresentativeImage() == null
            ? null
            : trip.getRepresentativeImage().getThumbnailUrl();
        String previewText = posts.isEmpty() ? null : createPreview(posts.getFirst().getMemo());

        return new TripSearchResponse(
            trip.getId(),
            trip.getTitle(),
            thumbnailUrl,
            trip.getStartDate(),
            trip.getEndDate(),
            trip.getCountry(),
            trip.getCity(),
            previewText
        );
    }

    private String createPreview(String memo) {
        if (!StringUtils.hasText(memo)) {
            return null;
        }

        String normalized = memo.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= PREVIEW_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, PREVIEW_MAX_LENGTH - 3) + "...";
    }
}
