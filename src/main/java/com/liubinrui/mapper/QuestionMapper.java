package com.liubinrui.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liubinrui.model.entity.Question;

import java.util.List;

/**
* @author 刘斌瑞
* @description 针对表【question(题目)】的数据库操作Mapper
* @createDate 2026-02-02 07:35:08
* @Entity generator.domain.Question
*/
public interface QuestionMapper extends BaseMapper<Question> {

     List<Question> getByAppId(Long appId);
}




