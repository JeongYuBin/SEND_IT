package com.sendit.itinerary;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class ItineraryDtos {
    private ItineraryDtos() {}

    public record CreateRequest(
            @NotBlank @Size(max = 150) String title,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotNull LocalTime dailyStartTime,
            @NotNull LocalTime dailyEndTime,
            @NotNull TransportType transportType,
            @NotEmpty @Size(max = 20) List<@NotNull Long> savedPlaceIds
    ) {}

    public record ItemResponse(
            Long savedPlaceId, int sequence, int daySequence, LocalDate visitDate,
            LocalTime arrivalTime, LocalTime departureTime,
            int travelMinutesFromPrevious, Double distanceKmFromPrevious,
            boolean coordinateAvailable, String name, String category,
            String address, Double latitude, Double longitude, String imageUrl,
            int stayMinutes
    ) {}

    public record DayResponse(
            LocalDate date, int dayNumber, boolean exceedsDailyWindow,
            List<ItemResponse> items
    ) {}

    public record Response(
            Long id, String title, LocalDate startDate, LocalDate endDate,
            LocalTime dailyStartTime, LocalTime dailyEndTime,
            TransportType transportType, ItineraryStatus status,
            List<ItemResponse> items, List<DayResponse> days, Instant createdAt
    ) {}
}
