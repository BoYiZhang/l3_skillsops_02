package com.boyi.skillops.service;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.dto.RatingRequest;
import com.boyi.skillops.vo.RatingVO;

public interface RatingService {
    void rate(Long skillId, RatingRequest request, Long userId);
    PageResult<RatingVO> getRatings(Long skillId, int page, int size);
}
