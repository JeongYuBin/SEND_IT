package com.sendit.itinerary;

import com.sendit.collection.ResourceNotFoundException;
import com.sendit.place.Place;
import com.sendit.place.UserSavedPlace;
import com.sendit.place.UserSavedPlaceRepository;
import com.sendit.place.VisitStatus;
import com.sendit.user.UserRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ItineraryService {
    private final ItineraryRepository itineraries;
    private final UserRepository users;
    private final UserSavedPlaceRepository savedPlaces;

    public ItineraryService(ItineraryRepository itineraries, UserRepository users,
                            UserSavedPlaceRepository savedPlaces) {
        this.itineraries = itineraries;
        this.users = users;
        this.savedPlaces = savedPlaces;
    }

    public ItineraryDtos.Response create(String email, ItineraryDtos.CreateRequest request) {
        validate(request);
        var user = users.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        List<Long> selectedIds = new LinkedHashSet<>(request.savedPlaceIds()).stream().toList();
        List<UserSavedPlace> ownedPlaces = savedPlaces.findByIdInAndUserEmail(selectedIds, email);
        if (ownedPlaces.size() != selectedIds.size()) {
            throw new IllegalArgumentException("선택한 장소 중 접근할 수 없는 장소가 있습니다.");
        }
        Map<Long, UserSavedPlace> placesById = ownedPlaces.stream()
                .collect(Collectors.toMap(UserSavedPlace::getId, Function.identity()));
        Itinerary itinerary = new Itinerary(user, request.title(), request.startDate(),
                request.endDate(), request.dailyStartTime(), request.dailyEndTime(),
                request.transportType());
        for (int index = 0; index < selectedIds.size(); index++) {
            UserSavedPlace savedPlace = placesById.get(selectedIds.get(index));
            itinerary.addPlace(savedPlace, index + 1);
            savedPlace.update(null, VisitStatus.PLANNED, null, savedPlace.getCollection());
        }
        return response(itineraries.save(itinerary));
    }

    @Transactional(readOnly = true)
    public List<ItineraryDtos.Response> list(String email) {
        return itineraries.findByUserEmailOrderByStartDateDescCreatedAtDesc(email)
                .stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public ItineraryDtos.Response get(String email, Long id) {
        return response(itineraries.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new ResourceNotFoundException("여행 계획을 찾을 수 없습니다.")));
    }

    private void validate(ItineraryDtos.CreateRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
        }
        if (!request.dailyEndTime().isAfter(request.dailyStartTime())) {
            throw new IllegalArgumentException("종료 시간은 시작 시간보다 늦어야 합니다.");
        }
        if (new LinkedHashSet<>(request.savedPlaceIds()).size() != request.savedPlaceIds().size()) {
            throw new IllegalArgumentException("같은 장소를 중복 선택할 수 없습니다.");
        }
    }

    private ItineraryDtos.Response response(Itinerary itinerary) {
        List<ItineraryDtos.ItemResponse> items = itinerary.getItems().stream().map(item -> {
            UserSavedPlace saved = item.getSavedPlace();
            Place place = saved.getPlace();
            return new ItineraryDtos.ItemResponse(saved.getId(), item.getSequence(),
                    place.getName(), place.getCategory(),
                    place.getRoadAddress() == null ? place.getAddress() : place.getRoadAddress(),
                    place.getLatitude(), place.getLongitude(), place.getPrimaryImageUrl(),
                    item.getStayMinutes());
        }).toList();
        return new ItineraryDtos.Response(itinerary.getId(), itinerary.getTitle(),
                itinerary.getStartDate(), itinerary.getEndDate(),
                itinerary.getDailyStartTime(), itinerary.getDailyEndTime(),
                itinerary.getTransportType(), itinerary.getStatus(), items,
                itinerary.getCreatedAt());
    }
}
