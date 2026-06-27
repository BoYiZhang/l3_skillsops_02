package com.boyi.skillops.dto;

import lombok.Data;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class RatingRequest {
    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;
    private String comment;
}
