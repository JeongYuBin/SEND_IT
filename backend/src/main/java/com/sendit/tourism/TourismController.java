package com.sendit.tourism;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.time.LocalDate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Validated
@RestController
@RequestMapping("/api/v1/tourism")
public class TourismController {

    private final TourApiClient tourApiClient;

    public TourismController(TourApiClient tourApiClient) {
        this.tourApiClient = tourApiClient;
    }

    @GetMapping("/nearby")
    List<TourApiClient.NearbyPlace> nearby(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double latitude,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double longitude,
            @RequestParam(defaultValue = "5000") @Min(100) @Max(20000) int radius
    ) {
        return tourApiClient.nearby(latitude, longitude, radius);
    }

    @GetMapping("/operating-info")
    OperatingInfoResponse operatingInfo(
            @RequestParam String name,
            @RequestParam(required = false) String address
    ) {
        return tourApiClient.operatingInfo(name, address)
                .map(info -> new OperatingInfoResponse(info.hours(), info.restDays(), true))
                .orElseGet(() -> new OperatingInfoResponse(null, null, false));
    }

    record OperatingInfoResponse(String hours, String restDays, boolean available) {}

    @GetMapping("/events")
    List<TourApiClient.Festival> events(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double latitude,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double longitude,
            @RequestParam(defaultValue = "30000") @Min(1000) @Max(100000) int radius
    ) {
        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "종료일은 시작일보다 빠를 수 없습니다.");
        }
        return tourApiClient.festivals(startDate, endDate, latitude, longitude, radius);
    }

    @GetMapping("/places/{contentId}")
    TourApiClient.TourismPlaceDetail detail(
            @org.springframework.web.bind.annotation.PathVariable String contentId,
            @RequestParam String contentTypeId
    ) {
        return tourApiClient.detail(contentId, contentTypeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "관광 상세정보를 찾을 수 없습니다."));
    }
}
