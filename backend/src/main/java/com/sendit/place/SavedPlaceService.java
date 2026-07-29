package com.sendit.place;

import com.sendit.collection.Collection;
import com.sendit.collection.CollectionRepository;
import com.sendit.collection.ResourceNotFoundException;
import com.sendit.share.SharedContent;
import com.sendit.share.SharedContentRepository;
import com.sendit.user.UserRepository;
import java.util.List;
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

    public SavedPlaceService(UserRepository users, PlaceRepository places,
            UserSavedPlaceRepository savedPlaces, CollectionRepository collections,
            SharedContentRepository shares) {
        this.users=users; this.places=places; this.savedPlaces=savedPlaces;
        this.collections=collections; this.shares=shares;
    }

    public SavedPlaceDtos.Response create(String email, SavedPlaceDtos.CreateRequest request) {
        var user = users.findByEmail(email).orElseThrow();
        validateCoordinates(request.latitude(), request.longitude());
        String normalizedName = request.name().trim().toLowerCase().replaceAll("\\s+", "");
        Place place = places.findFirstByNormalizedNameAndLatitudeAndLongitude(
                        normalizedName, request.latitude(), request.longitude())
                .orElseGet(() -> places.save(new Place(
                        request.name(), request.category(), request.address(),
                        request.roadAddress(), request.latitude(), request.longitude(),
                        request.description(), request.imageUrl())));
        var existingSaved = savedPlaces.findByUserIdAndPlaceId(user.getId(), place.getId());
        if (existingSaved.isPresent()) {
            return response(existingSaved.get());
        }
        Collection collection = collection(email, request.collectionId());
        SharedContent share = request.sharedContentId() == null ? null
                : shares.findByIdAndUserEmail(request.sharedContentId(), email)
                .orElseThrow(() -> new ResourceNotFoundException("공유 콘텐츠를 찾을 수 없습니다."));
        return response(savedPlaces.save(new UserSavedPlace(user, place, share, collection,
                request.memo(), request.priority() == null ? 0 : request.priority())));
    }

    @Transactional(readOnly = true)
    public List<SavedPlaceDtos.Response> list(String email) {
        return savedPlaces.findByUserEmailOrderBySavedAtDesc(email).stream()
                .map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public SavedPlaceDtos.Response get(String email, Long id) {
        return response(owned(email, id));
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
        saved.update(request.memo(), request.visitStatus(), request.priority(),
                selectedCollection);
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
    private SavedPlaceDtos.Response response(UserSavedPlace saved) {
        Place p=saved.getPlace(); Collection c=saved.getCollection();
        SharedContent share=saved.getSharedContent();
        return new SavedPlaceDtos.Response(saved.getId(), p.getId(), p.getName(), p.getCategory(),
                p.getAddress(), p.getRoadAddress(), p.getLatitude(), p.getLongitude(),
                p.getDescription(), p.getPrimaryImageUrl(), c==null?null:c.getId(),
                c==null?null:c.getName(), saved.getMemo(), saved.getVisitStatus(),
                saved.getPriority(), saved.getSavedAt(),
                share==null?null:share.getOriginalUrl());
    }
}
