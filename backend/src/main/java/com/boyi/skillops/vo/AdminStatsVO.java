package com.boyi.skillops.vo;

import lombok.Data;
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
    public static class TrendItem {
        private String date;
        private long count;
    }

    @Data
    public static class AuthorStat {
        private Long authorId;
        private String authorName;
        private long skillCount;
    }
}
