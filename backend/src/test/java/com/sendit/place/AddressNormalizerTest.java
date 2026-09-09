package com.sendit.place;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AddressNormalizerTest {
    @Test
    void expandsRegionAbbreviations() {
        assertThat(AddressNormalizer.normalize("서울 중구 세종대로 110"))
                .isEqualTo("서울특별시 중구 세종대로 110");
        assertThat(AddressNormalizer.normalize("강원도 속초시 해오름로 190"))
                .isEqualTo("강원특별자치도 속초시 해오름로 190");
        assertThat(AddressNormalizer.normalize("전북 전주시 완산구"))
                .isEqualTo("전북특별자치도 전주시 완산구");
    }

    @Test
    void preservesAlreadyNormalizedOrUnknownAddresses() {
        assertThat(AddressNormalizer.normalize("서울특별시 송파구 잠실동"))
                .isEqualTo("서울특별시 송파구 잠실동");
        assertThat(AddressNormalizer.normalize("일본 오사카부 오사카시"))
                .isEqualTo("일본 오사카부 오사카시");
        assertThat(AddressNormalizer.normalize("  부산   해운대구  "))
                .isEqualTo("부산광역시 해운대구");
    }
}
