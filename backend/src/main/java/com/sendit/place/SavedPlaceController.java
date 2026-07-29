package com.sendit.place;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/saved-places")
public class SavedPlaceController {
    private final SavedPlaceService service;
    public SavedPlaceController(SavedPlaceService service) { this.service=service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    SavedPlaceDtos.Response create(Principal principal,
            @Valid @RequestBody SavedPlaceDtos.CreateRequest request) {
        return service.create(principal.getName(), request);
    }
    @GetMapping
    List<SavedPlaceDtos.Response> list(Principal principal) {
        return service.list(principal.getName());
    }
    @GetMapping("/{id}")
    SavedPlaceDtos.Response get(Principal principal, @PathVariable Long id) {
        return service.get(principal.getName(), id);
    }
    @PatchMapping("/{id}")
    SavedPlaceDtos.Response update(Principal principal, @PathVariable Long id,
            @Valid @RequestBody SavedPlaceDtos.UpdateRequest request) {
        return service.update(principal.getName(), id, request);
    }
    @PostMapping("/{id}/sync-tourism")
    SavedPlaceDtos.Response syncTourism(Principal principal, @PathVariable Long id) {
        return service.syncTourism(principal.getName(), id);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(Principal principal, @PathVariable Long id) {
        service.delete(principal.getName(), id);
    }
}
