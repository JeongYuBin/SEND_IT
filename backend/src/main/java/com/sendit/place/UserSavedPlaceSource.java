package com.sendit.place;

import com.sendit.share.SharedContent;
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
@Table(name = "user_saved_place_sources")
public class UserSavedPlaceSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "saved_place_id", nullable = false)
    private UserSavedPlace savedPlace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shared_content_id", nullable = false)
    private SharedContent sharedContent;

    @CreationTimestamp
    @jakarta.persistence.Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;

    protected UserSavedPlaceSource() {
    }

    public UserSavedPlaceSource(UserSavedPlace savedPlace, SharedContent sharedContent) {
        this.savedPlace = savedPlace;
        this.sharedContent = sharedContent;
    }

    public SharedContent getSharedContent() {
        return sharedContent;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }
}
