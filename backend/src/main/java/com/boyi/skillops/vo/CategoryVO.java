package com.boyi.skillops.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CategoryVO {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createTime;
}
