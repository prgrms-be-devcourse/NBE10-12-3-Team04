package com.triptrace.domain.trip.tripAuto.service

import com.triptrace.domain.image.image.entity.Image
import com.triptrace.domain.image.image.repository.ImageRepository
import com.triptrace.domain.marker.marker.entity.Marker
import com.triptrace.domain.marker.marker.entity.MarkerSource
import com.triptrace.domain.marker.marker.geocoding.ReverseGeocodingClient
import com.triptrace.domain.marker.marker.geocoding.ReverseGeocodingResult
import com.triptrace.domain.marker.marker.repository.MarkerRepository
import com.triptrace.domain.post.post.entity.Post
import com.triptrace.domain.post.post.repository.PostRepository
import com.triptrace.domain.trip.trip.entity.Trip
import com.triptrace.domain.trip.trip.repository.TripRepository
import com.triptrace.domain.trip.tripAuto.dto.TripAutoRecordResponse
import com.triptrace.domain.trip.tripAuto.error.TripAutoErrorCode
import com.triptrace.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.TreeMap

@Service
class TripAutoRecordService(
    private val tripRepository: TripRepository,
    private val imageRepository: ImageRepository,
    private val postRepository: PostRepository,
    private val markerRepository: MarkerRepository,
    private val reverseGeocodingClient: ReverseGeocodingClient
) {

    // 자동 생성은 Post 생성, Marker 생성, Image 연결이 하나의 작업이므로 중간 실패 시 전체 롤백
    @Transactional
    fun createAutoRecords(tripId: Long, ownerId: Long): TripAutoRecordResponse {
        // 자동 생성 대상 여행기를 조회하고, 요청자가 해당 여행기의 소유자인지 확인
        val trip = tripRepository.findById(tripId)
            .orElseThrow { ServiceException(TripAutoErrorCode.TRIP_NOT_FOUND) }

        validateOwner(trip, ownerId)

        // 업로드 단계에서 저장된 Image row만 조회
        val images = imageRepository.findByTripId(tripId)

        // 자동 생성에는 촬영 시간과 GPS가 모두 있는 이미지만 사용
        // 메타데이터가 부족한 이미지는 실패가 아니라 제외 대상으로 보고 응답 count에 포함
        val usableImages = images
            .filter { hasAutoRecordMetadata(it) }
            .sortedBy { it.capturedAt }

        // 날짜를 먼저 나눈 뒤, 같은 날짜 안에서 촬영 시간 간격을 기준으로 세부 클러스터를 만든다.
        val clusters = clusterImages(usableImages)
        val records = mutableListOf<TripAutoRecordResponse.GeneratedRecord>()
        var firstMarkerLocation: ReverseGeocodingResult? = null

        for (cluster in clusters) {
            // 현재는 클러스터의 첫 번째 이미지를 대표 이미지로 사용
            // 대표 이미지의 촬영 시간과 GPS가 Post/Marker 생성 기준이 된다.
            val representativeImage = selectRepresentativeImage(cluster)
            val recordDate = representativeImage.capturedAtOrFail().toLocalDate()

            // 장소명은 대표 이미지 GPS를 역지오코딩해서 채운다. 실패하면 기본 문구로 두고 생성은 계속 진행한다.
            val location = reverseGeocodingClient.findLocation(
                representativeImage.gpsLat,
                representativeImage.gpsLng
            )
            if (firstMarkerLocation == null) {
                firstMarkerLocation = location
            }
            val placeName = location?.placeName
            val postTitle = "%s 근처".format(if (StringUtils.hasText(placeName)) placeName else "위치 미정")

            // 클러스터 하나를 여행 기록 게시물 하나로 변환
            val post = postRepository.save(
                Post(
                    trip,
                    recordDate,
                    postTitle,
                    ""
                )
            )

            // 클러스터 하나를 지도 마커 하나로 변환
            // Marker는 대표 Image만 참조
            val marker = markerRepository.save(
                Marker(
                    post,
                    truncateCoordinate(representativeImage.gpsLat),
                    truncateCoordinate(representativeImage.gpsLng),
                    placeName,
                    representativeImage.capturedAt,
                    MarkerSource.AUTO,
                    representativeImage
                )
            )

            if (trip.representativeImage == null) {
                trip.changeRepresentativeImage(representativeImage)
            }

            // 자동 생성된 Post에 클러스터 내 이미지들을 연결
            // 트랜잭션 안의 영속 엔티티라 별도 save 없음
            cluster.forEach { image -> image.connectPost(post) }

            // 클라이언트가 생성 결과를 바로 확인하거나 조회할 수 있도록 id 목록을 응답에 담는다.
            records.add(
                TripAutoRecordResponse.GeneratedRecord(
                    post.getId(),
                    marker.getId(),
                    representativeImage.getId(),
                    representativeImage.thumbnailUrl,
                    postTitle,
                    placeName,
                    recordDate,
                    marker.centerLat,
                    marker.centerLng,
                    cluster.map { it.getId() }
                )
            )
        }

        applyTripAutoRecordDefaults(trip, usableImages, firstMarkerLocation)

        val response = TripAutoRecordResponse(
            trip.getId(),
            records.size, //생성된 post카운트
            records.size, //생성된 marker카운트 -> 자동생성이라 post와 marker의 개수가 같음
            usableImages.size,
            images.size - usableImages.size,
            records
        )

        log.info(
            "[TRIP] auto record completed tripId: {}, ownerId: {}, postCount: {}, markerCount: {}, usedImageCount: {}, skippedImageCount: {}",
            tripId,
            ownerId,
            response.generatedPostCount,
            response.generatedMarkerCount,
            response.usedImageCount,
            response.skippedImageCount
        )

        return response
    }

    private fun applyTripAutoRecordDefaults(
        trip: Trip,
        usableImages: List<Image>,
        firstMarkerLocation: ReverseGeocodingResult?
    ) {
        if (usableImages.isEmpty()) {
            return
        }

        val firstImage = usableImages.first()
        val lastImage = usableImages.last()
        val country = firstMarkerLocation?.country
        val city = firstMarkerLocation?.city

        trip.changeAutoRecordDefaults(
            if (StringUtils.hasText(country)) country else trip.country,
            if (StringUtils.hasText(city)) city else trip.city,
            firstImage.capturedAt,
            lastImage.capturedAt
        )
    }

    private fun clusterImages(images: List<Image>): List<List<Image>> {
        // 먼저 날짜별로 큰 묶음을 만든다. 서로 다른 날짜의 사진은 같은 기록으로 묶지 않는다.
        // 날짜 순서대로 처리되도록 TreeMap에 저장
        val imagesByDate = images.groupByTo(TreeMap<LocalDate, MutableList<Image>>()) {
            it.capturedAtOrFail().toLocalDate()
        }

        val clusters = mutableListOf<List<Image>>()

        for (dailyImages in imagesByDate.values) {
            // 같은 날짜 안에서는 촬영 시간순으로 돌면서 클러스터 첫 이미지와 비교해 새 묶음 여부를 결정
            var currentCluster = mutableListOf<Image>()
            var clusterStartImage: Image? = null

            for (image in dailyImages) {
                // 클러스터 첫 사진 기준 2시간을 넘으면 현재 묶음을 닫고 새 묶음을 시작
                if (clusterStartImage != null && shouldStartNewCluster(clusterStartImage, image)) {
                    clusters.add(currentCluster)
                    currentCluster = mutableListOf()
                    clusterStartImage = image
                }

                if (clusterStartImage == null) {
                    clusterStartImage = image
                }

                currentCluster.add(image)
            }

            if (currentCluster.isNotEmpty()) {
                clusters.add(currentCluster)
            }
        }

        return clusters
    }

    private fun shouldStartNewCluster(clusterStartImage: Image, currentImage: Image): Boolean {
        // 같은 날짜 안에서 클러스터 첫 사진과 2시간을 초과해 떨어진 사진은 다른 기록으로 판단
        return exceedsTimeGap(clusterStartImage, currentImage)
    }

    private fun exceedsTimeGap(clusterStartImage: Image, currentImage: Image): Boolean {
        // 클러스터가 계속 이어져도 시작 사진 기준 2시간을 넘으면 별도 기록으로 나눔
        val minutes = Duration.between(
            clusterStartImage.capturedAtOrFail(),
            currentImage.capturedAtOrFail()
        ).abs().toMinutes()

        return minutes > CLUSTER_TIME_GAP_MINUTES
    }

    private fun selectRepresentativeImage(images: List<Image>): Image {
        // 1차 구현에서는 시간순 첫 번째 이미지를 대표 이미지로 둔다.
        // 이후에는 썸네일 품질, 장소 정확도, 사용자 선택값 등을 기준에 추가할 수 있다.
        return images.first()
    }

    private fun truncateCoordinate(coordinate: BigDecimal?): BigDecimal? {
        // Marker 중심 좌표는 이미지 GPS 공개 정밀도와 동일하게 소수점 7자리까지만 남기고 버림.
        return coordinate?.setScale(MARKER_COORDINATE_SCALE, RoundingMode.DOWN)
    }

    private fun validateOwner(trip: Trip, ownerId: Long) {
        if (trip.owner.getId() != ownerId) {
            throw ServiceException(TripAutoErrorCode.FORBIDDEN)
        }
    }

    private fun hasAutoRecordMetadata(image: Image): Boolean {
        // 촬영 시간과 GPS가 모두 있어야 자동 분리 기준으로 사용 가능
        return image.capturedAt != null &&
            image.gpsLat != null &&
            image.gpsLng != null
    }

    // hasAutoRecordMetadata를 통과한 이미지만 이 지점까지 오므로 촬영 시각은 항상 채워져 있다.
    private fun Image.capturedAtOrFail(): LocalDateTime =
        capturedAt ?: throw IllegalStateException("자동 기록 대상 이미지에 촬영 시각이 없습니다.")

    companion object {
        private val log = LoggerFactory.getLogger(TripAutoRecordService::class.java)

        // 같은 날짜 안에서도 클러스터 첫 사진과 2시간을 넘게 차이나면 다른 묶음으로 분리
        private const val CLUSTER_TIME_GAP_MINUTES = 120L
        private const val MARKER_COORDINATE_SCALE = 7
    }
}
