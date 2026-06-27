package com.boyi.skillops.controller;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.common.Result;
import com.boyi.skillops.dto.RatingRequest;
import com.boyi.skillops.service.RatingService;
import com.boyi.skillops.vo.RatingVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/skills")
public class RatingController {

    @Autowired private RatingService ratingService;

    @PostMapping("/{id}/ratings")
    public Result<Void> rate(@PathVariable Long id, @Valid @RequestBody RatingRequest request, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        ratingService.rate(id, request, userId);
        return Result.success();
    }

    @PutMapping("/{id}/ratings")
    public Result<Void> updateRating(@PathVariable Long id, @Valid @RequestBody RatingRequest request, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        ratingService.rate(id, request, userId);
        return Result.success();
    }

    @GetMapping("/{id}/ratings")
    public Result<PageResult<RatingVO>> getRatings(@PathVariable Long id,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return Result.success(ratingService.getRatings(id, page, size));
    }
}
