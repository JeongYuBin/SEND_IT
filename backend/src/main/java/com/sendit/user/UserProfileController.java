package com.sendit.user;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserProfileController {
    private final UserProfileService service;

    public UserProfileController(UserProfileService service) {
        this.service = service;
    }

    @GetMapping
    UserProfileDtos.Response get(Principal principal) {
        return service.get(principal.getName());
    }

    @PatchMapping
    UserProfileDtos.Response update(
            Principal principal,
            @Valid @RequestBody UserProfileDtos.UpdateRequest request
    ) {
        return service.update(principal.getName(), request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            Principal principal,
            @Valid @RequestBody UserProfileDtos.DeleteRequest request
    ) {
        service.delete(principal.getName(), request);
    }
}
