package com.liubinrui.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liubinrui.model.dto.scoring_result.ScoringResultAddRequest;
import com.liubinrui.model.dto.scoring_result.ScoringResultQueryRequest;
import com.liubinrui.model.entity.ScoringResult;
import com.liubinrui.model.vo.ScoringResultVO;

import javax.servlet.http.HttpServletRequest;

/**
 * 评分结果服务
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
public interface ScoringResultService extends IService<ScoringResult> {

    /**
     * 校验数据
     *
     * @param scoring_result
     * @param add 对创建的数据进行校验
     */
    void validScoringResult(ScoringResult scoring_result, boolean add);

    /**
     * 获取查询条件
     *
     * @param scoring_resultQueryRequest
     * @return
     */
    QueryWrapper<ScoringResult> getQueryWrapper(ScoringResultQueryRequest scoring_resultQueryRequest);
    
    /**
     * 获取评分结果封装
     *
     * @param scoring_result
     * @param request
     * @return
     */
    ScoringResultVO getScoringResultVO(ScoringResult scoring_result, HttpServletRequest request);

    /**
     * 分页获取评分结果封装
     *
     * @param scoring_resultPage
     * @param request
     * @return
     */
    Page<ScoringResultVO> getScoringResultVOPage(Page<ScoringResult> scoring_resultPage, HttpServletRequest request);

    /**
     * 创造字符串评分结果
     * @param scoringResultAddRequest
     */
    String createStringScoringResult(ScoringResultAddRequest scoringResultAddRequest);

    /**
     * 创建得分类评分结果
     * @param scoringResultAddRequest
     * @return
     */
    Integer createIntegerScoringResult(ScoringResultAddRequest scoringResultAddRequest);
}
