package com.sendit.tourism;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
