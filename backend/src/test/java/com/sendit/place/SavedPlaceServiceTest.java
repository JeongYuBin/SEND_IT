package com.sendit.place;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sendit.collection.CollectionRepository;
import com.sendit.share.SharedContentPlaceRepository;
import com.sendit.share.SharedContentRepository;
import com.sendit.tourism.TourApiClient;
import com.sendit.user.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SavedPlaceServiceTest {

    @Test
    void updatesNameWithoutRequiringAnAddress() {
        UserRepository users = mock(UserRepository.class);
        PlaceRepository places = mock(PlaceRepository.class);
        UserSavedPlaceRepository savedPlaces = mock(UserSavedPlaceRepository.class);
        CollectionRepository collections = mock(CollectionRepository.class);
        SharedContentRepository shares = mock(SharedContentRepository.class);
        TourApiClient tourApiClient = mock(TourApiClient.class);
        UserSavedPlaceSourceRepository savedPlaceSources = mock(UserSavedPlaceSourceRepository.class);
        SharedContentPlaceRepository extractedPlaces = mock(SharedContentPlaceRepository.class);
        PlaceDuplicateMatcher duplicateMatcher = mock(PlaceDuplicateMatcher.class);
        PlaceLocationResolver locationResolver = mock(PlaceLocationResolver.class);
        SavedPlaceService service = new SavedPlaceService(
                users, places, savedPlaces, collections, shares, tourApiClient,
                savedPlaceSources, extractedPlaces, duplicateMatcher, locationResolver);

        Place place = mock(Place.class);
        UserSavedPlace saved = mock(UserSavedPlace.class);
        when(savedPlaces.findByIdAndUserEmail(7L, "user@example.com"))
                .thenReturn(Optional.of(saved));
        when(saved.getPlace()).thenReturn(place);
        when(place.getName()).thenReturn("잘못 추출된 제목");
        when(savedPlaceSources.findBySavedPlaceIdOrderByLinkedAtDesc(saved.getId()))
                .thenReturn(List.of());

        service.update("user@example.com", 7L, new SavedPlaceDtos.UpdateRequest(
                "갓포 yp류", null, null, "", null,
                null, null, null, null));

        verify(place).updateUserDetails("갓포 yp류", null, null, "", null);
        verify(locationResolver, never()).resolve("갓포 yp류", "");
    }
}
