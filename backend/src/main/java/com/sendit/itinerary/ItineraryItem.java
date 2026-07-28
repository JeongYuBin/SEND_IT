package com.sendit.itinerary;

import com.sendit.place.UserSavedPlace;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "itinerary_items")
public class ItineraryItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "itinerary_id")
    private Itinerary itinerary;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "saved_place_id")
    private UserSavedPlace savedPlace;
    @Column(nullable = false)
    private int sequence;
    @Column(name = "stay_minutes", nullable = false)
    private int stayMinutes;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ItineraryItem() {}

    ItineraryItem(Itinerary itinerary, UserSavedPlace savedPlace, int sequence, int stayMinutes) {
        this.itinerary = itinerary;
        this.savedPlace = savedPlace;
        this.sequence = sequence;
        this.stayMinutes = stayMinutes;
    }

    public UserSavedPlace getSavedPlace() { return savedPlace; }
    public int getSequence() { return sequence; }
    public int getStayMinutes() { return stayMinutes; }
}
