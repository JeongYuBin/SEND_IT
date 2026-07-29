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

    @PutMapping("/{id}")
    ItineraryDtos.Response update(Principal principal, @PathVariable Long id,
                                  @Valid @RequestBody ItineraryDtos.UpdateRequest request) {
        return service.update(principal.getName(), id, request);
    }

    @PutMapping("/{id}/items/{savedPlaceId}/schedule")
    ItineraryDtos.Response updateItemSchedule(
            Principal principal,
            @PathVariable Long id,
            @PathVariable Long savedPlaceId,
            @Valid @RequestBody ItineraryDtos.UpdateItemScheduleRequest request
    ) {
        return service.updateItemSchedule(principal.getName(), id, savedPlaceId, request);
    }

    @PutMapping("/{id}/items/{savedPlaceId}/transport")
    ItineraryDtos.Response updateItemTransport(
            Principal principal,
            @PathVariable Long id,
            @PathVariable Long savedPlaceId,
            @Valid @RequestBody ItineraryDtos.UpdateItemTransportRequest request
    ) {
        return service.updateItemTransport(
                principal.getName(), id, savedPlaceId, request.transportType());
    }

    @PutMapping("/{id}/items/order")
    ItineraryDtos.Response reorder(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody ItineraryDtos.ReorderRequest request
    ) {
        return service.reorder(principal.getName(), id, request);
    }

    @PostMapping("/{id}/items")
    ItineraryDtos.Response addItem(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody ItineraryDtos.AddItemRequest request
    ) {
        return service.addItem(principal.getName(), id, request);
    }

    @DeleteMapping("/{id}/items/{savedPlaceId}")
    ItineraryDtos.Response removeItem(
            Principal principal,
            @PathVariable Long id,
            @PathVariable Long savedPlaceId
    ) {
        return service.removeItem(principal.getName(), id, savedPlaceId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(Principal principal, @PathVariable Long id) {
        service.delete(principal.getName(), id);
    }
}
