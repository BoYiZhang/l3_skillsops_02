package com.boyi.skillops.service;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.dto.MarketQueryRequest;
import com.boyi.skillops.vo.SkillVO;

import java.util.Map;

public interface MarketService {
    PageResult<SkillVO> queryMarket(MarketQueryRequest request);
    void install(Long skillId, Long userId);
    Map<String, Object> getInstallStatus(Long skillId, Long userId);
}
