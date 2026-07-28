package com.sendit.itinerary;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/itineraries")
public class ItineraryController {
    private final ItineraryService service;

    public ItineraryController(ItineraryService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ItineraryDtos.Response create(Principal principal,
                                   @Valid @RequestBody ItineraryDtos.CreateRequest request) {
        return service.create(principal.getName(), request);
    }

    @GetMapping
    List<ItineraryDtos.Response> list(Principal principal) {
        return service.list(principal.getName());
    }

    @GetMapping("/{id}")
    ItineraryDtos.Response get(Principal principal, @PathVariable Long id) {
        return service.get(principal.getName(), id);
    }
}
