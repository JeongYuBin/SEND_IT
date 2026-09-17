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
    void savesEveryExtractedPlaceIntoTheOneSelectedCollectionExactlyOnce() {
        Fixture fixture = new Fixture();
        var service = org.mockito.Mockito.spy(fixture.service);
        var share = mock(com.sendit.share.SharedContent.class);
        var user = mock(com.sendit.user.User.class);
        var collection = mock(com.sendit.collection.Collection.class);
        when(user.getEmail()).thenReturn("user@example.com");
        when(collection.getId()).thenReturn(12L);
        when(share.getUser()).thenReturn(user);
        when(share.getTargetCollection()).thenReturn(collection);
        when(share.getAnalysisStatus()).thenReturn(com.sendit.share.AnalysisStatus.COMPLETED);
        when(fixture.shares.findForSaving(7L)).thenReturn(Optional.of(share));
        var candidates = java.util.stream.IntStream.range(0, 6).mapToObj(i -> new com.sendit.share.SharedContentPlace(share, i,
                new com.sendit.share.PageMetadata(null, null, null, "장소" + i, "음식점", "서울 종로구", 37.5, 127.0))).toList();
        when(fixture.extracted.findBySharedContentIdOrderByDisplayOrder(7L)).thenReturn(candidates);
        var response = mock(SavedPlaceDtos.Response.class);
        when(response.savedPlaceId()).thenReturn(99L);
        org.mockito.Mockito.doReturn(response).when(service).create(org.mockito.ArgumentMatchers.eq("user@example.com"), org.mockito.ArgumentMatchers.any());
        service.autoSaveAnalyzedShare(7L);
        service.autoSaveAnalyzedShare(7L);
        var requests = org.mockito.ArgumentCaptor.forClass(SavedPlaceDtos.CreateRequest.class);
        verify(service, org.mockito.Mockito.times(6)).create(org.mockito.ArgumentMatchers.eq("user@example.com"), requests.capture());
        org.assertj.core.api.Assertions.assertThat(requests.getAllValues()).allMatch(request -> request.collectionId().equals(12L));
    }

    @Test
    void completedAnalysisDoesNotSaveBeforeCollectionConfirmation() {
        Fixture fixture = new Fixture();
        var share = mock(com.sendit.share.SharedContent.class);
        when(fixture.shares.findForSaving(7L)).thenReturn(Optional.of(share));
        when(share.getAnalysisStatus()).thenReturn(com.sendit.share.AnalysisStatus.COMPLETED);
        fixture.service.autoSaveAnalyzedShare(7L);
        org.mockito.Mockito.verifyNoInteractions(fixture.extracted, fixture.places);
    }

    @Test
    void rejectsMissingCollectionBeforeResolvingOrSavingPlace() {
        Fixture fixture = new Fixture();
        var user = mock(com.sendit.user.User.class);
        when(fixture.users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> fixture.service.create("user@example.com", new SavedPlaceDtos.CreateRequest(
                "카페", "카페", null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("컬렉션");
        org.mockito.Mockito.verifyNoInteractions(fixture.places, fixture.collections);
    }

    private static class Fixture {
        final UserRepository users = mock(UserRepository.class);
        final PlaceRepository places = mock(PlaceRepository.class);
        final CollectionRepository collections = mock(CollectionRepository.class);
        final SharedContentRepository shares = mock(SharedContentRepository.class);
        final SharedContentPlaceRepository extracted = mock(SharedContentPlaceRepository.class);
        final SavedPlaceService service = new SavedPlaceService(users, places, mock(UserSavedPlaceRepository.class), collections, shares,
                mock(TourApiClient.class), mock(UserSavedPlaceSourceRepository.class), extracted, mock(PlaceDuplicateMatcher.class), mock(PlaceLocationResolver.class));
    }

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
