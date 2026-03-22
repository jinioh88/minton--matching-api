package org.app.mintonmatchapi.match.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.app.mintonmatchapi.match.entity.CostPolicy;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 매칭 수정 요청. 모든 필드 선택(partial update).
 * 전달된 필드만 수정하며, null/미포함 필드는 기존 값 유지.
 */
@Getter
@Setter
@NoArgsConstructor
public class MatchUpdateRequest {

    @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
    private String title;

    private String description;

    private LocalDate matchDate;

    private LocalTime startTime;

    @Min(value = 30, message = "소요 시간은 30분 이상이어야 합니다.")
    @Max(value = 240, message = "소요 시간은 240분 이하여야 합니다.")
    private Integer durationMin;

    @Size(max = 200, message = "장소명은 200자 이하여야 합니다.")
    private String locationName;

    @Pattern(regexp = "^[0-9]{7,10}$", message = "행정구역 코드는 7~10자리 숫자 형식이어야 합니다.")
    private String regionCode;

    @Min(value = 2, message = "모집 인원은 2명 이상이어야 합니다.")
    @Max(value = 12, message = "모집 인원은 12명 이하여야 합니다.")
    private Integer maxPeople;

    @Pattern(regexp = "^$|^[A-Za-z,]+$", message = "희망 급수는 A,B,C 형식이어야 합니다.")
    @Size(max = 50)
    private String targetLevels;

    private CostPolicy costPolicy;

    @Size(max = 2000, message = "이미지 URL은 2000자 이하여야 합니다.")
    private String imageUrl;

    private Double latitude;
    private Double longitude;
}
