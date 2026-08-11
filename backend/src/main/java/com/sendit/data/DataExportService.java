package com.sendit.data;

import com.sendit.itinerary.ItineraryService;
import com.sendit.place.SavedPlaceService;
import com.sendit.share.ShareService;
import com.sendit.user.UserProfileService;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataExportService {
    private final UserProfileService profiles;
    private final SavedPlaceService savedPlaces;
    private final ItineraryService itineraries;
    private final ShareService shares;

    public DataExportService(
            UserProfileService profiles,
            SavedPlaceService savedPlaces,
            ItineraryService itineraries,
            ShareService shares
    ) {
        this.profiles = profiles;
        this.savedPlaces = savedPlaces;
        this.itineraries = itineraries;
        this.shares = shares;
    }

    @Transactional(readOnly = true)
    public DataExportDtos.Response export(String email) {
        return new DataExportDtos.Response(
                "1.0",
                Instant.now(),
                profiles.get(email),
                savedPlaces.list(email),
                itineraries.list(email),
                shares.list(email)
        );
    }
}
