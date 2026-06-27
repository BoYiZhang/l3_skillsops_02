package com.boyi.skillops.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InstallVO {
    private Long id;
    private Long skillId;
    private String skillName;
    private String skillDescription;
    private String version;
    private String status;
    private LocalDateTime createTime;
}
