package com.sendit.place;

import com.sendit.collection.Collection;
import com.sendit.share.SharedContent;
import com.sendit.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "user_saved_places")
public class UserSavedPlace {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "place_id")
    private Place place;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "shared_content_id")
    private SharedContent sharedContent;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "collection_id")
    private Collection collection;
    @Column(length = 1000)
    private String memo;
    @Enumerated(EnumType.STRING) @Column(name = "visit_status", nullable = false)
    private VisitStatus visitStatus;
    @Column(nullable = false)
    private int priority;
    @CreationTimestamp @Column(name = "saved_at", nullable = false, updatable = false)
    private Instant savedAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserSavedPlace() {}
    public UserSavedPlace(User user, Place place, SharedContent sharedContent,
                          Collection collection, String memo, int priority) {
        this.user = user; this.place = place; this.sharedContent = sharedContent;
        this.collection = collection; this.memo = memo; this.priority = priority;
        this.visitStatus = VisitStatus.WANT_TO_VISIT;
    }
    public Long getId() { return id; }
    public Place getPlace() { return place; }
    public Collection getCollection() { return collection; }
    public String getMemo() { return memo; }
    public VisitStatus getVisitStatus() { return visitStatus; }
    public int getPriority() { return priority; }
    public Instant getSavedAt() { return savedAt; }
    public void update(String memo, VisitStatus status, Integer priority, Collection collection) {
        if (memo != null) this.memo = memo;
        if (status != null) this.visitStatus = status;
        if (priority != null) this.priority = priority;
        this.collection = collection;
    }
}

