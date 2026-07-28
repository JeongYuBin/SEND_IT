package com.sendit.itinerary;

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
        for (int dayIndex = 0; dayIndex < dayCount; dayIndex++) {
            List<ItineraryItem> ordered = new ArrayList<>(
                    nearestNeighborOrder(dayBuckets.get(dayIndex)));
            ordered.sort(Comparator
                    .comparing(ItineraryItem::getPreferredStartTime,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparingInt(ItineraryItem::getSequence));
            LocalTime currentTime = itinerary.getDailyStartTime();
            List<ScheduledStop> stops = new ArrayList<>();
            ItineraryItem previous = null;

            for (int index = 0; index < ordered.size(); index++) {
                ItineraryItem item = ordered.get(index);
                TravelEstimate travel = estimate(previous, item, itinerary.getTransportType());
                currentTime = currentTime.plusMinutes(travel.minutes());
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
                        coordinates(item) != null
                ));
                currentTime = departureTime;
                previous = item;
            }
            schedules.add(new DaySchedule(
                    itinerary.getStartDate().plusDays(dayIndex),
                    dayIndex + 1,
                    stops,
                    currentTime.isAfter(itinerary.getDailyEndTime())
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
            return new TravelEstimate(0, 0.0);
        }
        Coordinates fromCoordinates = coordinates(from);
        Coordinates toCoordinates = coordinates(to);
        if (fromCoordinates == null || toCoordinates == null) {
            return new TravelEstimate(20, null);
        }
        double straightDistance = distance(fromCoordinates, toCoordinates);
        double routeFactor = transportType == TransportType.WALKING ? 1.2 : 1.35;
        double routeDistance = straightDistance * routeFactor;
        double speed = switch (transportType) {
            case WALKING -> 4.0;
            case PUBLIC_TRANSIT -> 22.0;
            case CAR -> 35.0;
        };
        int rawMinutes = (int) Math.ceil(routeDistance / speed * 60);
        int roundedMinutes = rawMinutes == 0 ? 0 : Math.max(5, ((rawMinutes + 4) / 5) * 5);
        return new TravelEstimate(roundedMinutes,
                Math.round(routeDistance * 10.0) / 10.0);
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
            boolean coordinateAvailable
    ) {}

    private record Coordinates(double latitude, double longitude) {}
    private record TravelEstimate(int minutes, Double distanceKm) {}
}
