package org.app.mintonmatchapi.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.app.mintonmatchapi.review.entity.ReviewSentiment;

import java.util.List;

@Getter
@Setter
public class ReviewCreateRequest {

    @NotNull
    private Long revieweeId;

    @NotNull
    private ReviewSentiment sentiment;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer score;

    @Size(max = 10)
    private List<@Size(max = 40) String> hashtags;

    @Size(max = 2000)
    private String detail;
}
