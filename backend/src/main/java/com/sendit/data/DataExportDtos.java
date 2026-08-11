package com.sendit.data;

import com.sendit.itinerary.ItineraryDtos;
import com.sendit.place.SavedPlaceDtos;
import com.sendit.share.ShareDtos;
import com.sendit.user.UserProfileDtos;
import java.time.Instant;
import java.util.List;

public final class DataExportDtos {
    private DataExportDtos() {
    }

    public record Response(
            String formatVersion,
            Instant exportedAt,
            UserProfileDtos.Response profile,
            List<SavedPlaceDtos.Response> savedPlaces,
            List<ItineraryDtos.Response> itineraries,
            List<ShareDtos.ShareDetailResponse> sharedContents
    ) {
    }
}
