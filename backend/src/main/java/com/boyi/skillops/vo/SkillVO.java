package com.boyi.skillops.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SkillVO {
    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private Long authorId;
    private String authorName;
    private String repoUrl;
    private String docUrl;
    private String status;
    private Long installCount;
    private Double avgRating;
    private String latestVersion;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
