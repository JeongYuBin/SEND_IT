package com.sendit.place;

import com.sendit.collection.Collection;
import com.sendit.collection.CollectionRepository;
import com.sendit.collection.ResourceNotFoundException;
import com.sendit.share.SharedContent;
import com.sendit.share.SharedContentRepository;
import com.sendit.share.AnalysisStatus;
import com.sendit.tourism.TourApiClient;
import com.sendit.user.UserRepository;
import java.util.List;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SavedPlaceService {
    private final UserRepository users;
    private final PlaceRepository places;
    private final UserSavedPlaceRepository savedPlaces;
    private final CollectionRepository collections;
    private final SharedContentRepository shares;
    private final TourApiClient tourApiClient;
    private final UserSavedPlaceSourceRepository savedPlaceSources;

    public SavedPlaceService(UserRepository users, PlaceRepository places,
            UserSavedPlaceRepository savedPlaces, CollectionRepository collections,
            SharedContentRepository shares, TourApiClient tourApiClient,
            UserSavedPlaceSourceRepository savedPlaceSources) {
        this.users=users; this.places=places; this.savedPlaces=savedPlaces;
        this.collections=collections; this.shares=shares; this.tourApiClient=tourApiClient;
        this.savedPlaceSources = savedPlaceSources;
    }

    public SavedPlaceDtos.Response create(String email, SavedPlaceDtos.CreateRequest request) {
        var user = users.findByEmail(email).orElseThrow();
        validateCoordinates(request.latitude(), request.longitude());
        String normalizedName = request.name().trim().toLowerCase().replaceAll("\\s+", "");
        var tourismDetail = tourApiClient.detail(
                request.tourismContentId(), request.tourismContentTypeId());
        String description = tourismDetail.map(TourApiClient.TourismPlaceDetail::description)
                .orElse(request.description());
        String imageUrl = tourismDetail.map(TourApiClient.TourismPlaceDetail::imageUrl)
                .orElse(request.imageUrl());
        Place place = duplicatePlace(normalizedName, request)
                .orElseGet(() -> places.save(new Place(
                        request.name(), request.category(), request.address(),
                        request.roadAddress(), request.latitude(), request.longitude(),
                        description, imageUrl)));
        place.mergeExternalDetails(
                request.category(), request.address(), request.roadAddress(),
                request.latitude(), request.longitude(), description, imageUrl,
                request.phone(), request.kakaoPlaceId(), request.kakaoPlaceUrl());
        place.updateEventPeriod(request.eventStartDate(), request.eventEndDate());
        tourismDetail.ifPresent(detail -> place.enrichTourismDetails(
                detail.contentId(), detail.contentTypeId(), detail.description(),
                detail.imageUrl(), detail.phone(), detail.homepageUrl(),
                detail.operatingHours(), detail.restDays(), detail.parkingInfo()));
        Collection collection = collection(email, request.collectionId());
        var existingSaved = savedPlaces.findByUserIdAndPlaceId(user.getId(), place.getId());
        if (existingSaved.isPresent()) {
            UserSavedPlace saved = existingSaved.get();
            saved.update(
                    request.memo(), request.priority(),
                    request.collectionId() == null ? saved.getCollection() : collection);
            attachSource(saved, request.sharedContentId(), email);
            return response(saved);
        }
        SharedContent share = request.sharedContentId() == null ? null
                : shares.findByIdAndUserEmail(request.sharedContentId(), email)
                .orElseThrow(() -> new ResourceNotFoundException("공유 콘텐츠를 찾을 수 없습니다."));
        UserSavedPlace saved = savedPlaces.save(new UserSavedPlace(user, place, share, collection,
                request.memo(), request.priority() == null ? 0 : request.priority()));
        if (share != null) attachSource(saved, share);
        return response(saved);
    }

    public void autoSaveAnalyzedShare(Long sharedContentId) {
        SharedContent share = shares.findById(sharedContentId)
                .orElseThrow(() -> new ResourceNotFoundException("공유 콘텐츠를 찾을 수 없습니다."));
        if (share.getAnalysisStatus() != AnalysisStatus.COMPLETED
                || share.getExtractedPlaceName() == null || share.getExtractedPlaceName().isBlank()
                || savedPlaces.existsByUserIdAndSharedContentId(
                        share.getUser().getId(), sharedContentId)) {
            return;
        }
        create(share.getUser().getEmail(), new SavedPlaceDtos.CreateRequest(
                share.getExtractedPlaceName(),
                share.getExtractedCategory(),
                share.getExtractedAddress(),
                share.getExtractedAddress(),
                share.getExtractedLatitude(),
                share.getExtractedLongitude(),
                null,
                share.getThumbnailUrl(),
                null,
                null,
                null,
                null,
                sharedContentId,
                null,
                null,
                0,
                null,
                null,
                null
        ));
    }

    @Transactional(readOnly = true)
    public List<SavedPlaceDtos.Response> list(String email) {
        return savedPlaces.findByUserEmailOrderBySavedAtDesc(email).stream()
                .map(this::response).toList();
    }

    public SavedPlaceDtos.Response get(String email, Long id) {
        var saved = owned(email, id);
        autoSyncTourism(saved.getPlace());
        return response(saved);
    }

    public SavedPlaceDtos.Response update(String email, Long id, SavedPlaceDtos.UpdateRequest request) {
        var saved = owned(email, id);
        Collection selectedCollection;
        if (Boolean.TRUE.equals(request.clearCollection())) {
            selectedCollection = null;
        } else if (request.collectionId() != null) {
            selectedCollection = collection(email, request.collectionId());
        } else {
            selectedCollection = saved.getCollection();
        }
        saved.update(request.memo(), request.priority(), selectedCollection);
        return response(saved);
    }

    public void delete(String email, Long id) {
        var saved = owned(email, id);
        savedPlaces.delete(saved);
    }

    private UserSavedPlace owned(String email, Long id) {
        return savedPlaces.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new ResourceNotFoundException("저장 장소를 찾을 수 없습니다."));
    }
    private Collection collection(String email, Long id) {
        if (id == null) return null;
        return collections.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new ResourceNotFoundException("컬렉션을 찾을 수 없습니다."));
    }
    private void validateCoordinates(Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null))
            throw new IllegalArgumentException("위도와 경도는 함께 입력해야 합니다.");
    }
    private void autoSyncTourism(Place place) {
        if (place.getTourismContentId() != null) return;
        Instant attemptedAt = place.getTourismSyncAttemptedAt();
        if (attemptedAt != null && attemptedAt.isAfter(Instant.now().minus(7, ChronoUnit.DAYS))) {
            return;
        }
        var detail = tourApiClient.findDetail(place.getName(),
                place.getRoadAddress() == null ? place.getAddress() : place.getRoadAddress());
        if (detail.isEmpty()) {
            place.markTourismSyncAttempted();
            return;
        }
        var matched = detail.get();
        place.enrichTourismDetails(
                matched.contentId(), matched.contentTypeId(), matched.description(),
                matched.imageUrl(), matched.phone(), matched.homepageUrl(),
                matched.operatingHours(), matched.restDays(), matched.parkingInfo());
    }
    private SavedPlaceDtos.Response response(UserSavedPlace saved) {
        Place p=saved.getPlace(); Collection c=saved.getCollection();
        SharedContent share=saved.getSharedContent();
        var sources = savedPlaceSources.findBySavedPlaceIdOrderByLinkedAtDesc(saved.getId()).stream()
                .map(source -> {
                    SharedContent content = source.getSharedContent();
                    return new SavedPlaceDtos.SourceResponse(
                            content.getId(), content.getSourceType(), content.getTitle(),
                            content.getDescription(), content.getOriginalUrl(),
                            content.getThumbnailUrl(), source.getLinkedAt());
                }).toList();
        String originalUrl = sources.isEmpty()
                ? (share == null ? null : share.getOriginalUrl()) : sources.get(0).originalUrl();
        return new SavedPlaceDtos.Response(saved.getId(), p.getId(), p.getName(), p.getCategory(),
                p.getAddress(), p.getRoadAddress(), p.getLatitude(), p.getLongitude(),
                p.getDescription(), p.getPrimaryImageUrl(), p.getPhone(), p.getHomepageUrl(),
                p.getTourismContentId(), p.getTourismContentTypeId(),
                p.getOperatingHours(), p.getRestDays(), p.getParkingInfo(),
                p.getEventStartDate(), p.getEventEndDate(),
                c==null?null:c.getId(),
                c==null?null:c.getName(), saved.getMemo(), saved.getPriority(), saved.getSavedAt(),
                originalUrl, p.getKakaoPlaceId(), p.getKakaoPlaceUrl(), sources);
    }

    private void attachSource(UserSavedPlace saved, Long sharedContentId, String email) {
        if (sharedContentId == null) return;
        SharedContent share = shares.findByIdAndUserEmail(sharedContentId, email)
                .orElseThrow(() -> new ResourceNotFoundException("공유 콘텐츠를 찾을 수 없습니다."));
        attachSource(saved, share);
    }

    private void attachSource(UserSavedPlace saved, SharedContent share) {
        if (!savedPlaceSources.existsBySavedPlaceIdAndSharedContentId(
                saved.getId(), share.getId())) {
            savedPlaceSources.save(new UserSavedPlaceSource(saved, share));
        }
    }

    private java.util.Optional<Place> duplicatePlace(
            String normalizedName, SavedPlaceDtos.CreateRequest request
    ) {
        if (request.kakaoPlaceId() != null && !request.kakaoPlaceId().isBlank()) {
            var byKakao = places.findFirstByKakaoPlaceId(request.kakaoPlaceId());
            if (byKakao.isPresent()) return byKakao;
        }
        if (request.tourismContentId() != null && !request.tourismContentId().isBlank()) {
            var byTourism = places.findFirstByTourismContentId(request.tourismContentId());
            if (byTourism.isPresent()) return byTourism;
        }
        if (request.latitude() != null && request.longitude() != null) {
            var nearby = places.findNearbyDuplicate(
                    normalizedName, request.latitude(), request.longitude());
            if (nearby.isPresent()) return nearby;
        }
        if (request.roadAddress() != null && !request.roadAddress().isBlank()) {
            var byRoadAddress = places.findFirstByNormalizedNameAndRoadAddress(
                    normalizedName, request.roadAddress());
            if (byRoadAddress.isPresent()) return byRoadAddress;
        }
        if (request.address() != null && !request.address().isBlank()) {
            var byAddress = places.findFirstByNormalizedNameAndAddress(
                    normalizedName, request.address());
            if (byAddress.isPresent()) return byAddress;
        }
        return places.findFirstByNormalizedNameAndLatitudeAndLongitude(
                normalizedName, request.latitude(), request.longitude());
    }
}
