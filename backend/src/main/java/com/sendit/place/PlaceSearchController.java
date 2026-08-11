package com.sendit.place;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/places")
public class PlaceSearchController {
    private final KakaoPlaceDirectoryClient kakao;

    public PlaceSearchController(KakaoPlaceDirectoryClient kakao) {
        this.kakao = kakao;
    }

    @GetMapping("/search")
    PlaceSearchDtos.Response search(
            @RequestParam @NotBlank @Size(max = 100) String query,
            @RequestParam(defaultValue = "1") @Min(1) @Max(45) int page
    ) {
        return kakao.search(query, page);
    }
}
