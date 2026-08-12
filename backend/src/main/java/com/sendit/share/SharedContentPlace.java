package com.sendit.share;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "shared_content_places")
public class SharedContentPlace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shared_content_id", nullable = false)
    private SharedContent sharedContent;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false, length = 200)
    private String name;
    @Column(length = 100)
    private String category;
    @Column(length = 500)
    private String address;
    private Double latitude;
    private Double longitude;
    @Column(name = "image_url", length = 2048)
    private String imageUrl;
    @Column(name = "saved_place_id")
    private Long savedPlaceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SharedContentPlace() { }

    public SharedContentPlace(SharedContent sharedContent, int displayOrder, PageMetadata place) {
        this.sharedContent = sharedContent;
        this.displayOrder = displayOrder;
        this.name = place.placeName();
        this.category = place.category();
        this.address = place.address();
        this.latitude = place.latitude();
        this.longitude = place.longitude();
        this.imageUrl = place.imageUrl();
    }

    public Long getId() { return id; }
    public SharedContent getSharedContent() { return sharedContent; }
    public int getDisplayOrder() { return displayOrder; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getAddress() { return address; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getImageUrl() { return imageUrl; }
    public Long getSavedPlaceId() { return savedPlaceId; }
    public void markSaved(Long id) { this.savedPlaceId = id; }
}
