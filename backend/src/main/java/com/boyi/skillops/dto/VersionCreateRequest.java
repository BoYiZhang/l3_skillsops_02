package com.boyi.skillops.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class VersionCreateRequest {
    @NotBlank
    private String version;
    private String changelog;
}
