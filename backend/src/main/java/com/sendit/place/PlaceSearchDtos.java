package com.sendit.place;

import java.util.List;

public final class PlaceSearchDtos {
    private PlaceSearchDtos() {
    }

    public record Result(
            String kakaoPlaceId,
            String name,
            String category,
            String categoryGroup,
            String address,
            String roadAddress,
            String phone,
            Double latitude,
            Double longitude,
            String kakaoPlaceUrl
    ) {
    }

    public record Response(List<Result> places, int page, boolean last) {
    }
}
