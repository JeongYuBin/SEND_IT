package com.sendit.itinerary;

import com.sendit.map.KakaoTransitClient;
import com.sendit.map.KakaoDirectionsClient;
import com.sendit.place.Place;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ItineraryRoutePlanner {

    private static final double EARTH_RADIUS_KM = 6371.0088;
    private final KakaoTransitClient transitClient;
    private final KakaoDirectionsClient directionsClient;

    public ItineraryRoutePlanner(
            KakaoTransitClient transitClient,
            KakaoDirectionsClient directionsClient
    ) {
        this.transitClient = transitClient;
        this.directionsClient = directionsClient;
    }

    public List<DaySchedule> plan(Itinerary itinerary) {
        int dayCount = Math.toIntExact(
                ChronoUnit.DAYS.between(itinerary.getStartDate(), itinerary.getEndDate()) + 1);
        List<List<ItineraryItem>> dayBuckets = new ArrayList<>();
        for (int index = 0; index < dayCount; index++) {
            dayBuckets.add(new ArrayList<>());
        }
        List<ItineraryItem> flexibleItems = new ArrayList<>();
        for (ItineraryItem item : itinerary.getItems()) {
            LocalDate preferredDate = item.getPreferredVisitDate();
            if (preferredDate == null
                    || preferredDate.isBefore(itinerary.getStartDate())
                    || preferredDate.isAfter(itinerary.getEndDate())) {
                flexibleItems.add(item);
            } else {
                int dayIndex = Math.toIntExact(
                        ChronoUnit.DAYS.between(itinerary.getStartDate(), preferredDate));
                dayBuckets.get(dayIndex).add(item);
            }
        }
        int targetPlacesPerDay = (int) Math.ceil(
                (double) itinerary.getItems().size() / dayCount);
        int flexibleDayIndex = 0;
        for (ItineraryItem item : nearestNeighborOrder(flexibleItems)) {
            while (flexibleDayIndex < dayCount - 1
                    && dayBuckets.get(flexibleDayIndex).size() >= targetPlacesPerDay) {
                flexibleDayIndex++;
            }
            dayBuckets.get(flexibleDayIndex).add(item);
        }

        List<DaySchedule> schedules = new ArrayList<>();
        ItineraryItem previous = null;
        int previousDayIndex = -1;
        for (int dayIndex = 0; dayIndex < dayCount; dayIndex++) {
            List<ItineraryItem> ordered = new ArrayList<>(
                    nearestNeighborOrder(dayBuckets.get(dayIndex)));
            ordered.sort(Comparator.comparingInt(ItineraryItem::getSequence));
            LocalTime currentTime = itinerary.getDailyStartTime();
            List<ScheduledStop> stops = new ArrayList<>();
            for (int index = 0; index < ordered.size(); index++) {
                ItineraryItem item = ordered.get(index);
                TransportType transportType = item.getTransportTypeFromPrevious() == null
                        ? itinerary.getTransportType()
                        : item.getTransportTypeFromPrevious();
                TravelEstimate travel = estimate(previous, item, transportType);
                boolean crossDayTransfer = previous != null && previousDayIndex != dayIndex;
                if (!crossDayTransfer) {
                    currentTime = currentTime.plusMinutes(travel.minutes());
                }
                LocalTime arrivalTime = item.getPreferredStartTime() != null
                        && item.getPreferredStartTime().isAfter(currentTime)
                        ? item.getPreferredStartTime()
                        : currentTime;
                LocalTime departureTime = arrivalTime.plusMinutes(item.getStayMinutes());
                stops.add(new ScheduledStop(
                        item,
                        index + 1,
                        arrivalTime,
                        departureTime,
                        travel.minutes(),
                        travel.distanceKm(),
                        coordinates(item) != null,
                        travel.transitRoute(),
                        transportType,
                        crossDayTransfer
                ));
                currentTime = departureTime;
                previous = item;
                previousDayIndex = dayIndex;
            }
            schedules.add(new DaySchedule(
                    itinerary.getStartDate().plusDays(dayIndex),
                    dayIndex + 1,
                    stops,
                    dayIndex == dayCount - 1
                            && currentTime.isAfter(itinerary.getDailyEndTime())
            ));
        }
        return schedules;
    }

    List<ItineraryItem> nearestNeighborOrder(List<ItineraryItem> source) {
        if (source.size() < 2) {
            return List.copyOf(source);
        }
        List<ItineraryItem> remaining = new ArrayList<>(source);
        remaining.sort(Comparator.comparingInt(ItineraryItem::getSequence));
        List<ItineraryItem> ordered = new ArrayList<>();
        ItineraryItem current = remaining.removeFirst();
        ordered.add(current);

        while (!remaining.isEmpty()) {
            Coordinates currentCoordinates = coordinates(current);
            if (currentCoordinates == null) {
                current = remaining.removeFirst();
            } else {
                current = remaining.stream()
                        .filter(item -> coordinates(item) != null)
                        .min(Comparator
                                .comparingDouble((ItineraryItem item) ->
                                        distance(currentCoordinates, coordinates(item)))
                                .thenComparingInt(ItineraryItem::getSequence))
                        .orElse(remaining.getFirst());
                remaining.remove(current);
            }
            ordered.add(current);
        }
        return ordered;
    }

    private TravelEstimate estimate(ItineraryItem from, ItineraryItem to,
                                    TransportType transportType) {
        if (from == null) {
            return new TravelEstimate(0, 0.0, null);
        }
        Coordinates fromCoordinates = coordinates(from);
        Coordinates toCoordinates = coordinates(to);
        if (fromCoordinates == null || toCoordinates == null) {
            return new TravelEstimate(20, null, null);
        }
        if (transportType == TransportType.PUBLIC_TRANSIT) {
            Place fromPlace = from.getSavedPlace().getPlace();
            Place toPlace = to.getSavedPlace().getPlace();
            var route = transitClient.route(
                    new KakaoTransitClient.Location(
                            fromPlace.getName(), fromCoordinates.latitude(), fromCoordinates.longitude()),
                    new KakaoTransitClient.Location(
                            toPlace.getName(), toCoordinates.latitude(), toCoordinates.longitude())
            );
            if (route.isPresent()) {
                KakaoTransitClient.TransitRoute transitRoute = route.get();
                return new TravelEstimate(
                        transitRoute.totalMinutes(),
                        Math.round(transitRoute.totalDistanceMeters() / 100.0) / 10.0,
                        transitRoute
                );
            }
        }
        if (transportType == TransportType.CAR || transportType == TransportType.WALKING) {
            var route = directionsClient.route(
                    transportType,
                    new KakaoDirectionsClient.Location(
                            fromCoordinates.latitude(), fromCoordinates.longitude()),
                    new KakaoDirectionsClient.Location(
                            toCoordinates.latitude(), toCoordinates.longitude()));
            if (route.isPresent()) {
                KakaoDirectionsClient.RouteEstimate estimate = route.get();
                return new TravelEstimate(
                        estimate.totalMinutes(),
                        Math.round(estimate.totalDistanceMeters() / 100.0) / 10.0,
                        null
                );
            }
        }
        return new TravelEstimate(0, null, null);
    }

    private Coordinates coordinates(ItineraryItem item) {
        Place place = item.getSavedPlace().getPlace();
        if (place.getLatitude() == null || place.getLongitude() == null) {
            return null;
        }
        return new Coordinates(place.getLatitude(), place.getLongitude());
    }

    private double distance(Coordinates first, Coordinates second) {
        double latitudeDistance = Math.toRadians(second.latitude() - first.latitude());
        double longitudeDistance = Math.toRadians(second.longitude() - first.longitude());
        double firstLatitude = Math.toRadians(first.latitude());
        double secondLatitude = Math.toRadians(second.latitude());
        double haversine = Math.pow(Math.sin(latitudeDistance / 2), 2)
                + Math.cos(firstLatitude) * Math.cos(secondLatitude)
                * Math.pow(Math.sin(longitudeDistance / 2), 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }

    public record DaySchedule(
            LocalDate date,
            int dayNumber,
            List<ScheduledStop> stops,
            boolean exceedsDailyWindow
    ) {}

    public record ScheduledStop(
            ItineraryItem item,
            int daySequence,
            LocalTime arrivalTime,
            LocalTime departureTime,
            int travelMinutesFromPrevious,
            Double distanceKmFromPrevious,
            boolean coordinateAvailable,
            KakaoTransitClient.TransitRoute transitRoute
            ,
            TransportType transportType,
            boolean crossDayTransfer
    ) {}

    private record Coordinates(double latitude, double longitude) {}
    private record TravelEstimate(
            int minutes,
            Double distanceKm,
            KakaoTransitClient.TransitRoute transitRoute
    ) {}
}
