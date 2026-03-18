package org.app.mintonmatchapi.match.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.app.mintonmatchapi.match.entity.CostPolicy;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class MatchCreateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
    private String title;

    @NotBlank(message = "설명은 필수입니다.")
    private String description;

    @NotNull(message = "경기 날짜는 필수입니다.")
    private LocalDate matchDate;

    @NotNull(message = "시작 시간은 필수입니다.")
    private LocalTime startTime;

    @NotNull(message = "소요 시간은 필수입니다.")
    @Min(value = 30, message = "소요 시간은 30분 이상이어야 합니다.")
    @Max(value = 240, message = "소요 시간은 240분 이하여야 합니다.")
    private Integer durationMin;

    @Size(max = 200, message = "장소명은 200자 이하여야 합니다.")
    private String locationName;

    @NotBlank(message = "행정구역 코드는 필수입니다.")
    @Pattern(regexp = "^[0-9]{7,10}$", message = "행정구역 코드는 7~10자리 숫자 형식이어야 합니다.")
    private String regionCode;

    @NotNull(message = "모집 인원은 필수입니다.")
    @Min(value = 2, message = "모집 인원은 2명 이상이어야 합니다.")
    @Max(value = 12, message = "모집 인원은 12명 이하여야 합니다.")
    private Integer maxPeople;

    @Pattern(regexp = "^$|^[A-Za-z,]+$", message = "희망 급수는 A,B,C 형식이어야 합니다.")
    @Size(max = 50)
    private String targetLevels;

    @NotNull(message = "비용 분담 방식은 필수입니다.")
    private CostPolicy costPolicy;

    @Size(max = 2000, message = "이미지 URL은 2000자 이하여야 합니다.")
    private String imageUrl;

    private Double latitude;
    private Double longitude;
}
