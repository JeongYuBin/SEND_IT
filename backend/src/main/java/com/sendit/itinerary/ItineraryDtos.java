package com.sendit.itinerary;

import jakarta.validation.Valid;
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

    public record UpdateRequest(
            @NotBlank @Size(max = 150) String title,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotNull LocalTime dailyStartTime,
            @NotNull LocalTime dailyEndTime,
            @NotNull TransportType transportType
    ) {}

    public record UpdateItemScheduleRequest(
            LocalDate visitDate,
            LocalTime startTime,
            @NotNull @Min(15) @Max(720) Integer stayMinutes
    ) {}

    public record UpdateItemTransportRequest(@NotNull TransportType transportType) {}

    public record ReorderItemRequest(
            @NotNull Long savedPlaceId,
            @NotNull LocalDate visitDate,
            @Min(1) int sequence,
            TransportType transportTypeFromPrevious
    ) {}

    public record ReorderRequest(
            @NotEmpty List<@NotNull @Valid ReorderItemRequest> items
    ) {}

    public record ItemResponse(
            Long savedPlaceId, int sequence, int daySequence, LocalDate visitDate,
            LocalTime arrivalTime, LocalTime departureTime,
            int travelMinutesFromPrevious, Double distanceKmFromPrevious,
            boolean coordinateAvailable, LocalDate preferredVisitDate,
            LocalTime preferredStartTime, String name, String category,
            String address, Double latitude, Double longitude, String imageUrl,
            int stayMinutes, TransitRouteResponse transit,
            List<RoutePathPointResponse> routePathFromPrevious,
            TransportType transportTypeFromPrevious, boolean crossDayTransfer
    ) {}

    public record RoutePathPointResponse(double latitude, double longitude) {}

    public record TransitRouteResponse(
            String type, int totalMinutes, int totalDistanceMeters,
            int transfers, int fare, String landingUrl,
            List<TransitStepResponse> steps
    ) {}

    public record TransitStepResponse(
            String type, String guidance, int minutes, int distanceMeters,
            String startStop, String endStop, List<String> vehicles
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
