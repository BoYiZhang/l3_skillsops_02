package com.boyi.skillops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("skill_audits")
public class SkillAudit extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long skillId;
    private Long auditorId;
    private String action;
    private String reason;
}
