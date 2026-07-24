package com.sendit.place;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "places")
public class Place {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(name = "normalized_name", nullable = false, length = 200)
    private String normalizedName;
    @Column(length = 100)
    private String category;
    @Column(length = 500)
    private String address;
    @Column(name = "road_address", length = 500)
    private String roadAddress;
    private Double latitude;
    private Double longitude;
    @Column(length = 50)
    private String phone;
    @Column(name = "homepage_url", length = 2048)
    private String homepageUrl;
    @Column(columnDefinition = "text")
    private String description;
    @Column(name = "primary_image_url", length = 2048)
    private String primaryImageUrl;
    @Column(name = "data_source", nullable = false, length = 30)
    private String dataSource;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Place() {}

    public Place(String name, String category, String address, String roadAddress,
                 Double latitude, Double longitude, String description, String imageUrl) {
        this.name = name.trim();
        this.normalizedName = normalize(name);
        this.category = category;
        this.address = address;
        this.roadAddress = roadAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.primaryImageUrl = imageUrl;
        this.dataSource = "USER";
    }

    private String normalize(String value) {
        return value.trim().toLowerCase().replaceAll("\\s+", "");
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getAddress() { return address; }
    public String getRoadAddress() { return roadAddress; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getDescription() { return description; }
    public String getPrimaryImageUrl() { return primaryImageUrl; }
}

