package org.app.mintonmatchapi.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.app.mintonmatchapi.user.entity.Level;

@Getter
@Setter
@NoArgsConstructor
public class ProfileUpdateRequest {

    @Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
    private String nickname;

    @Size(max = 2000, message = "프로필 이미지 URL은 2000자 이하여야 합니다.")
    private String profileImg;

    private Level level;

    /**
     * 관심 지역 1 (행정구역 코드 7~10자리)
     */
    @Pattern(regexp = "^$|^[0-9]{7,10}$", message = "관심 지역은 7~10자리 숫자 형식이어야 합니다.")
    @Size(max = 50)
    private String interestLoc1;

    /**
     * 관심 지역 2 (행정구역 코드 7~10자리)
     */
    @Pattern(regexp = "^$|^[0-9]{7,10}$", message = "관심 지역은 7~10자리 숫자 형식이어야 합니다.")
    @Size(max = 50)
    private String interestLoc2;

    @Size(max = 100, message = "라켓 정보는 100자 이하여야 합니다.")
    private String racketInfo;

    @Size(max = 50, message = "플레이 스타일은 50자 이하여야 합니다.")
    private String playStyle;
}
