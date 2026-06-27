package com.boyi.skillops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
public class AdminStatsVO {
    private long totalSkills;
    private long totalUsers;
    private long totalInstalls;
    private List<TrendItem> installTrend;
    private List<AuthorStat> topAuthors;
    private List<SkillVO> topSkills;

    // ======== 新增字段 ========
    private double avgRating;
    private List<TrendItem> userTrend;
    private List<CategoryStat> categoryDistribution;
    private List<RatingDist> ratingDistribution;
    private AuditSummary auditSummary;

    // ======== 已有内部类 ========
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendItem {
        private String date;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorStat {
        private Long authorId;
        private String authorName;
        private long skillCount;
    }

    // ======== 新增内部类 ========
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStat {
        private String categoryName;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingDist {
        private int star;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditSummary {
        private long pending;
        private long published;
        private long delisted;
        private long draft;
    }
}
