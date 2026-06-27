package com.boyi.skillops.controller;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.common.Result;
import com.boyi.skillops.dto.MarketQueryRequest;
import com.boyi.skillops.service.MarketService;
import com.boyi.skillops.vo.SkillVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/market")
public class MarketController {

    @Autowired private MarketService marketService;

    @GetMapping("/skills")
    public Result<PageResult<SkillVO>> query(MarketQueryRequest request) {
        return Result.success(marketService.queryMarket(request));
    }

    @PostMapping("/skills/{id}/install")
    public Result<Void> install(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        marketService.install(id, userId);
        return Result.success();
    }

    @GetMapping("/skills/{id}/install-status")
    public Result<Map<String, Object>> installStatus(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return Result.success(marketService.getInstallStatus(id, userId));
    }
}
