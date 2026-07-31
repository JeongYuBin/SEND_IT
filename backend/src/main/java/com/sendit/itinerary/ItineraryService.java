package com.sendit.itinerary;

import com.sendit.collection.ResourceNotFoundException;
import com.sendit.map.KakaoTransitClient;
import com.sendit.place.Place;
import com.sendit.place.UserSavedPlace;
import com.sendit.place.UserSavedPlaceRepository;
import com.sendit.place.VisitStatus;
import com.sendit.user.UserRepository;
import java.time.LocalDate;
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
    private final ItineraryRoutePlanner routePlanner;

    public ItineraryService(ItineraryRepository itineraries, UserRepository users,
                            UserSavedPlaceRepository savedPlaces,
                            ItineraryRoutePlanner routePlanner) {
        this.itineraries = itineraries;
        this.users = users;
        this.savedPlaces = savedPlaces;
        this.routePlanner = routePlanner;
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
        itinerary.markGenerated();
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

    public ItineraryDtos.Response update(String email, Long id,
                                          ItineraryDtos.UpdateRequest request) {
        validateDatesAndTimes(request.startDate(), request.endDate(),
                request.dailyStartTime(), request.dailyEndTime());
        Itinerary itinerary = ownedItinerary(email, id);
        itinerary.update(request.title(), request.startDate(), request.endDate(),
                request.dailyStartTime(), request.dailyEndTime(), request.transportType());
        return response(itinerary);
    }

    public ItineraryDtos.Response updateItemSchedule(
            String email,
            Long id,
            Long savedPlaceId,
            ItineraryDtos.UpdateItemScheduleRequest request
    ) {
        Itinerary itinerary = ownedItinerary(email, id);
        if (request.visitDate() != null
                && (request.visitDate().isBefore(itinerary.getStartDate())
                || request.visitDate().isAfter(itinerary.getEndDate()))) {
            throw new IllegalArgumentException("방문일은 여행 기간 안에서 선택해 주세요.");
        }
        if (request.startTime() != null && request.visitDate() == null) {
            throw new IllegalArgumentException("방문 시간을 지정하려면 방문일도 선택해 주세요.");
        }
        ItineraryItem item = itinerary.getItems().stream()
                .filter(candidate -> candidate.getSavedPlace().getId().equals(savedPlaceId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("여행 계획에서 장소를 찾을 수 없습니다."));
        item.updateSchedule(request.visitDate(), request.startTime(), request.stayMinutes());
        itinerary.markGenerated();
        return response(itinerary);
    }

    public void delete(String email, Long id) {
        itineraries.delete(ownedItinerary(email, id));
    }

    public ItineraryDtos.Response updateItemTransport(
            String email, Long id, Long savedPlaceId, TransportType transportType
    ) {
        Itinerary itinerary = ownedItinerary(email, id);
        itineraryItem(itinerary, savedPlaceId).updateTransportType(transportType);
        return response(itinerary);
    }

    public ItineraryDtos.Response addItem(
            String email, Long id, ItineraryDtos.AddItemRequest request
    ) {
        Itinerary itinerary = ownedItinerary(email, id);
        if (request.visitDate().isBefore(itinerary.getStartDate())
                || request.visitDate().isAfter(itinerary.getEndDate())) {
            throw new IllegalArgumentException("방문일은 여행 기간 안에서 선택해 주세요.");
        }
        boolean alreadyIncluded = itinerary.getItems().stream()
                .anyMatch(item -> item.getSavedPlace().getId().equals(request.savedPlaceId()));
        if (alreadyIncluded) {
            return response(itinerary);
        }
        UserSavedPlace savedPlace = savedPlaces
                .findByIdAndUserEmail(request.savedPlaceId(), email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "저장 장소를 찾을 수 없습니다."));
        validateEventDate(savedPlace.getPlace(), request.visitDate());
        int nextSequence = itinerary.getItems().stream()
                .mapToInt(ItineraryItem::getSequence)
                .max()
                .orElse(0) + 1;
        itinerary.addPlace(savedPlace, nextSequence, request.visitDate());
        savedPlace.update(null, VisitStatus.PLANNED, null, savedPlace.getCollection());
        itinerary.markGenerated();
        return response(itinerary);
    }

    private void validateEventDate(Place place, LocalDate visitDate) {
        if (place.getEventStartDate() != null
                && visitDate.isBefore(place.getEventStartDate())) {
            throw new IllegalArgumentException(
                    "행사 시작일(" + place.getEventStartDate() + ") 이후의 날짜를 선택해 주세요.");
        }
        if (place.getEventEndDate() != null
                && visitDate.isAfter(place.getEventEndDate())) {
            throw new IllegalArgumentException(
                    "행사 종료일(" + place.getEventEndDate() + ") 이전의 날짜를 선택해 주세요.");
        }
    }

    public ItineraryDtos.Response removeItem(
            String email, Long id, Long savedPlaceId
    ) {
        Itinerary itinerary = ownedItinerary(email, id);
        if (!itinerary.removePlace(savedPlaceId)) {
            throw new ResourceNotFoundException("여행 경로에서 장소를 찾을 수 없습니다.");
        }
        itinerary.markGenerated();
        return response(itinerary);
    }

    public ItineraryDtos.Response reorder(
            String email, Long id, ItineraryDtos.ReorderRequest request
    ) {
        Itinerary itinerary = ownedItinerary(email, id);
        if (request.items().size() != itinerary.getItems().size()
                || request.items().stream().map(ItineraryDtos.ReorderItemRequest::savedPlaceId)
                .distinct().count() != itinerary.getItems().size()) {
            throw new IllegalArgumentException("모든 장소를 중복 없이 전달해 주세요.");
        }
        for (int index = 0; index < request.items().size(); index++) {
            itineraryItem(itinerary, request.items().get(index).savedPlaceId())
                    .updateOrdering(request.items().get(index).visitDate(), 10_000 + index);
        }
        itineraries.flush();
        for (var ordered : request.items()) {
            if (ordered.visitDate().isBefore(itinerary.getStartDate())
                    || ordered.visitDate().isAfter(itinerary.getEndDate())) {
                throw new IllegalArgumentException("여행 기간 안의 날짜를 선택해 주세요.");
            }
            ItineraryItem item = itineraryItem(itinerary, ordered.savedPlaceId());
            item.updateOrdering(ordered.visitDate(), ordered.sequence());
            if (ordered.transportTypeFromPrevious() != null) {
                item.updateTransportType(ordered.transportTypeFromPrevious());
            }
        }
        return response(itinerary);
    }

    private ItineraryItem itineraryItem(Itinerary itinerary, Long savedPlaceId) {
        return itinerary.getItems().stream()
                .filter(item -> item.getSavedPlace().getId().equals(savedPlaceId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "여행 계획에서 장소를 찾을 수 없습니다."));
    }

    private Itinerary ownedItinerary(String email, Long id) {
        return itineraries.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new ResourceNotFoundException("여행 계획을 찾을 수 없습니다."));
    }

    private void validate(ItineraryDtos.CreateRequest request) {
        validateDatesAndTimes(request.startDate(), request.endDate(),
                request.dailyStartTime(), request.dailyEndTime());
        if (new LinkedHashSet<>(request.savedPlaceIds()).size() != request.savedPlaceIds().size()) {
            throw new IllegalArgumentException("같은 장소를 중복 선택할 수 없습니다.");
        }
    }

    private void validateDatesAndTimes(
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            java.time.LocalTime dailyStartTime,
            java.time.LocalTime dailyEndTime
    ) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
        }
        if (!dailyEndTime.isAfter(dailyStartTime)) {
            throw new IllegalArgumentException("종료 시간은 시작 시간보다 늦어야 합니다.");
        }
    }

    private ItineraryDtos.Response response(Itinerary itinerary) {
        List<ItineraryDtos.DayResponse> days = routePlanner.plan(itinerary).stream()
                .map(day -> new ItineraryDtos.DayResponse(
                        day.date(),
                        day.dayNumber(),
                        day.exceedsDailyWindow(),
                        day.stops().stream()
                                .map(stop -> itemResponse(day, stop))
                                .toList()
                ))
                .toList();
        List<ItineraryDtos.ItemResponse> items = days.stream()
                .flatMap(day -> day.items().stream())
                .toList();
        return new ItineraryDtos.Response(itinerary.getId(), itinerary.getTitle(),
                itinerary.getStartDate(), itinerary.getEndDate(),
                itinerary.getDailyStartTime(), itinerary.getDailyEndTime(),
                itinerary.getTransportType(), itinerary.getStatus(), items,
                days, itinerary.getCreatedAt());
    }

    private ItineraryDtos.ItemResponse itemResponse(
            ItineraryRoutePlanner.DaySchedule day,
            ItineraryRoutePlanner.ScheduledStop stop
    ) {
        UserSavedPlace saved = stop.item().getSavedPlace();
        Place place = saved.getPlace();
        return new ItineraryDtos.ItemResponse(
                saved.getId(),
                stop.item().getSequence(),
                stop.daySequence(),
                day.date(),
                stop.arrivalTime(),
                stop.departureTime(),
                stop.travelMinutesFromPrevious(),
                stop.distanceKmFromPrevious(),
                stop.coordinateAvailable(),
                stop.item().getPreferredVisitDate(),
                stop.item().getPreferredStartTime(),
                place.getName(),
                place.getCategory(),
                place.getRoadAddress() == null ? place.getAddress() : place.getRoadAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getPrimaryImageUrl(),
                stop.item().getStayMinutes(),
                transitResponse(stop.transitRoute()),
                stop.routePath().stream()
                        .map(point -> new ItineraryDtos.RoutePathPointResponse(
                                point.latitude(), point.longitude()))
                        .toList(),
                stop.transportType(),
                stop.crossDayTransfer(),
                stop.operatingInfo() == null ? null : stop.operatingInfo().hours(),
                stop.operatingInfo() == null ? null : stop.operatingInfo().restDays(),
                stop.visitWarning()
        );
    }

    private ItineraryDtos.TransitRouteResponse transitResponse(
            KakaoTransitClient.TransitRoute route
    ) {
        if (route == null) return null;
        return new ItineraryDtos.TransitRouteResponse(
                route.type(),
                route.totalMinutes(),
                route.totalDistanceMeters(),
                route.transfers(),
                route.fare(),
                route.landingUrl(),
                route.steps().stream()
                        .map(step -> new ItineraryDtos.TransitStepResponse(
                                step.type(),
                                step.guidance(),
                                step.minutes(),
                                step.distanceMeters(),
                                step.startStop(),
                                step.endStop(),
                                step.vehicles()
                        ))
                        .toList()
        );
    }
}
