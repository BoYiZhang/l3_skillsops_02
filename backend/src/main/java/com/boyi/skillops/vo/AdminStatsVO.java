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
}
