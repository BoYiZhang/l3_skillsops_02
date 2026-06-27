package com.boyi.skillops.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RatingVO {
    private Long id;
    private Long userId;
    private String username;
    private Integer rating;
    private String comment;
    private LocalDateTime createTime;
}
