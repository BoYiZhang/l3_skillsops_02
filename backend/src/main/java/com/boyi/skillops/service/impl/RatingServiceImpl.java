package com.boyi.skillops.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.dto.RatingRequest;
import com.boyi.skillops.entity.SkillInstall;
import com.boyi.skillops.entity.SkillRating;
import com.boyi.skillops.entity.User;
import com.boyi.skillops.enums.ErrorCode;
import com.boyi.skillops.exception.BusinessException;
import com.boyi.skillops.mapper.SkillInstallMapper;
import com.boyi.skillops.mapper.SkillMapper;
import com.boyi.skillops.mapper.SkillRatingMapper;
import com.boyi.skillops.mapper.UserMapper;
import com.boyi.skillops.service.RatingService;
import com.boyi.skillops.vo.RatingVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RatingServiceImpl implements RatingService {

    @Autowired private SkillRatingMapper ratingMapper;
    @Autowired private SkillInstallMapper installMapper;
    @Autowired private SkillMapper skillMapper;
    @Autowired private UserMapper userMapper;

    @Override
    @Transactional
    public void rate(Long skillId, RatingRequest request, Long userId) {
        SkillInstall install = installMapper.selectOne(
                new LambdaQueryWrapper<SkillInstall>().eq(SkillInstall::getUserId, userId).eq(SkillInstall::getSkillId, skillId));
        if (install == null) throw new BusinessException(ErrorCode.NOT_INSTALLED);

        SkillRating exist = ratingMapper.selectOne(
                new LambdaQueryWrapper<SkillRating>().eq(SkillRating::getUserId, userId).eq(SkillRating::getSkillId, skillId));
        if (exist != null) {
            exist.setRating(request.getRating());
            exist.setComment(request.getComment());
            ratingMapper.updateById(exist);
        } else {
            SkillRating rating = new SkillRating();
            rating.setUserId(userId); rating.setSkillId(skillId);
            rating.setRating(request.getRating()); rating.setComment(request.getComment());
            ratingMapper.insert(rating);
        }
        skillMapper.updateAvgRating(skillId);
    }

    @Override
    public PageResult<RatingVO> getRatings(Long skillId, int page, int size) {
        Page<SkillRating> p = new Page<>(page, size);
        Page<SkillRating> result = ratingMapper.selectPage(p,
                new LambdaQueryWrapper<SkillRating>().eq(SkillRating::getSkillId, skillId).orderByDesc(SkillRating::getCreateTime));
        List<RatingVO> vos = result.getRecords().stream().map(r -> {
            RatingVO vo = new RatingVO();
            BeanUtils.copyProperties(r, vo);
            User u = userMapper.selectById(r.getUserId());
            if (u != null) vo.setUsername(u.getUsername());
            vo.setCreateTime(r.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
        PageResult<RatingVO> pr = new PageResult<>();
        pr.setRecords(vos); pr.setTotal(result.getTotal()); pr.setSize(result.getSize()); pr.setCurrent(result.getCurrent());
        return pr;
    }
}
