package com.boyi.skillops.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class SkillCreateRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String description;
    @NotNull
    private Long categoryId;
    private String repoUrl;
    private String docUrl;
}
