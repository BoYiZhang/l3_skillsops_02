package com.boyi.skillops.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boyi.skillops.entity.Skill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SkillMapper extends BaseMapper<Skill> {
    @Update("UPDATE skills SET install_count = install_count + 1 WHERE id = #{skillId}")
    void updateInstallCount(Long skillId);

    @Update("UPDATE skills SET avg_rating = (SELECT AVG(rating) FROM skill_ratings WHERE skill_id = #{skillId}) WHERE id = #{skillId}")
    void updateAvgRating(Long skillId);
}
