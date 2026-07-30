package com.triptrace.domain.trip.trip.entity;

import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

@Entity
public class Trip extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @Column(nullable = false)
    private String title;

    private String country;

    private String city;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Column(nullable = false)
    private boolean visibility;

    @Column(nullable = false)
    private Long likeCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "representative_image_id")
    private Image representativeImage;

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        this.likeCount--;
    }

    public void modify(
        String title,
        String country,
        String city,
        LocalDateTime startDate,
        LocalDateTime endDate,
        boolean visibility
    ) {
        this.title = title;
        this.country = country;
        this.city = city;
        this.startDate = startDate;
        this.endDate = endDate;
        this.visibility = visibility;
    }

    public void changeRepresentativeImage(Image representativeImage) {
        this.representativeImage = representativeImage;
    }

    public void changeAutoRecordDefaults(
        String country,
        String city,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        this.country = country;
        this.city = city;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void changeDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Trip(
        Member owner,
        String title,
        String country,
        String city,
        LocalDateTime startDate,
        LocalDateTime endDate,
        boolean visibility
    ) {
        this.owner = owner;
        this.title = title;
        this.country = country;
        this.city = city;
        this.startDate = startDate;
        this.endDate = endDate;
        this.visibility = visibility;
    }

    @java.lang.SuppressWarnings("all")
    public Member getOwner() {
        return this.owner;
    }

    @java.lang.SuppressWarnings("all")
    public String getTitle() {
        return this.title;
    }

    @java.lang.SuppressWarnings("all")
    public String getCountry() {
        return this.country;
    }

    @java.lang.SuppressWarnings("all")
    public String getCity() {
        return this.city;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getStartDate() {
        return this.startDate;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getEndDate() {
        return this.endDate;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isVisibility() {
        return this.visibility;
    }

    @java.lang.SuppressWarnings("all")
    public Long getLikeCount() {
        return this.likeCount;
    }

    @java.lang.SuppressWarnings("all")
    public Image getRepresentativeImage() {
        return this.representativeImage;
    }

    @java.lang.SuppressWarnings("all")
    public Trip() {
    }
}
