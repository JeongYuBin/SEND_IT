package com.sendit.collection;

import com.sendit.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "collections")
public class Collection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(length = 500)
    private String description;
    @Column(name = "cover_image_url", length = 2048)
    private String coverImageUrl;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Collection() {}
    public Collection(User user, String name, String description) {
        this.user = user; this.name = name.trim(); this.description = description;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public void update(String name, String description) {
        if (name != null) this.name = name.trim();
        if (description != null) this.description = description;
    }
}

