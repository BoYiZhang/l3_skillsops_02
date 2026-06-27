package com.boyi.skillops.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VersionVO {
    private Long id;
    private Long skillId;
    private String version;
    private String changelog;
    private LocalDateTime createTime;
}
