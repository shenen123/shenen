package com.liubinrui.strategy.question;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.liubinrui.client.QwenClient;
import com.liubinrui.common.ErrorCode;
import com.liubinrui.enums.AppTypeEnum;
import com.liubinrui.enums.ScoreLevelEnum;
import com.liubinrui.exception.ThrowUtils;
import com.liubinrui.model.dto.question.QuestionAnswerDTO;
import com.liubinrui.model.dto.scoring_result.ScoringResultAddRequest;
import com.liubinrui.model.entity.*;
import com.liubinrui.service.AppService;
import com.liubinrui.service.QuestionService;
import com.liubinrui.service.ScoringResultService;
import com.liubinrui.service.UserAnswerService;
import com.liubinrui.strategy.ScoringStrategy;
import io.swagger.models.auth.In;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;


@Component
@Slf4j
public class ScoreBasedScoringStrategy implements ScoringStrategy {
    @Resource
    private ScoringResultService scoringResultService;
    @Resource
    private AppService appService;
    @Resource
    private QuestionService questionService;
    @Resource
    private QwenClient qwenClient;

    @Value("${ai.dashscope.api-key}")
    private String dashscopeApiKey;
    @Resource
    private UserAnswerService userAnswerService;
    @Resource
    private RedissonClient redissonClient;
    // 定义哪些 appType 属于“得分类”
    private static final Set<Integer> SCORE_BASED_TYPES = Set.of(
            AppTypeEnum.MATH.getValue(),      // 1
            AppTypeEnum.CALCULUS.getValue()   // 2
    );
    public static final String AI_TEST_SCORING_SYSTEM_MESSAGE =
            "你是一位严谨的数学测验判题专家。我会提供以下信息：\n" +
                    "1. 应用名称\n" +
                    "2. 应用描述\n" +
                    "3. 用户对一系列计算题的回答列表，格式为：[{\"title\": \"题目内容\", \"answer\": \"用户选择的答案\"}]\n" +
                    "\n" +
                    "【你的任务】\n" +
                    "1. 对每一道题目，必须先自行计算出正确答案；\n" +
                    "2. 将用户提供的 answer 与你计算的正确答案进行严格比对（字符串形式，如 \"26\"）；\n" +
                    "3. 判断规则：\n" +
                    "   - 如果 answer 为 null，视为未作答，不得分；\n" +
                    "   - 如果 answer 是整数字符串，但数值 ≠ 正确答案 → 答错，不得分；\n" +
                    "   - 只有当 answer 的整数值 == 正确答案 → 答对，得 1 分；\n" +
                    "4. 最终得分百分比 = (答对题数 ÷ 总题数) × 100，其中 **总题数 = 所有题目数量（包括未作答和答错的题目）**，结果四舍五入取整（0~100）。\n" +
                    "\n" +
                    "【输出要求】\n" +
                    "- resultName：基于得分的简短评语（如 \"优秀\"(≥90)、\"良好\"(80-89)、\"继续努力\"(60-79)、\"需要加强\"(<60)）；\n" +
                    "- resultScore：计算出的得分百分比（整数字符串，如 \"75\"）；\n" +
                    "- resultDesc：不多于 200 字的个性化反馈，需包含：\n" +
                    "   • 明确指出典型错题（如 \"45 - 19 你答了 27，正确答案是 26\"）；\n" +
                    "   • 如有未作答题，也请提醒（如 \"有 2 题未作答，建议尽量完成所有题目\"）；\n" +
                    "   • 分析错误原因（如退位减法不熟练）；\n" +
                    "   • 针对性练习建议；\n" +
                    "   • 鼓励性语言；\n" +
                    "- 必须严格以纯 JSON 格式输出，仅包含一个 JSON 对象；\n" +
                    "- 不要任何额外文字、注释、Markdown 或反引号；\n" +
                    "- 输出必须以 { 开头，以 } 结尾。\n" +
                    "\n" +
                    "【正确输出示例】\n" +
                    "{\"resultName\": \"需要加强\", \"resultScore\": \"50\", \"resultDesc\": \"本次测试共4题，你答对2题，有1题未作答，得分50%。特别注意：'45 - 19 = ?' 你答了 '27'，但正确答案是 '26'，说明你在退位减法上容易出错。建议每天练习10道竖式计算，重点训练借位。考试时尽量完成所有题目，不要空题！坚持练习，你一定能进步！\"}\n" +
                    "\n" +
                    "现在，请严格按上述规则分析并直接输出 JSON：";
    private final Cache<String, String> aiScoringCache = Caffeine.newBuilder()
            .initialCapacity(1024)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();
    private static final String AI_ANSWER_LOCK_PREFIX = "lock:ai_answer:";

    @Override
    public boolean supports(Integer appType) {
        return SCORE_BASED_TYPES.contains(appType);
    }

    @Override
    public void execute(ScoringResultAddRequest request, ScoringResult result) {
        Long answerId = request.getAnswerId();
        Long appId = request.getAppId();
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null || appId == null || answerId == null, ErrorCode.NOT_FOUND_ERROR);
        UserAnswer userAnswer = userAnswerService.getById(answerId);
        List<Question> questions = questionService.getQuestions(appId);
        List<QuestionAnswerDTO> questionAnswerDTOList = new ArrayList<>();
        List<String> choices = userAnswer.getChoices();
        // 将 choices 转为稳定字符串（避免 null/空 list 问题）
        String choicesStr = JSONUtil.toJsonStr(choices != null ? choices : new ArrayList<>());
        //构建缓存
        String cacheKey = buildCacheKey(appId, choicesStr);
        // 尝试从缓存获取 AI 结果
        String cachedAiResponse = aiScoringCache.getIfPresent(cacheKey);
        if (StrUtil.isNotBlank(cachedAiResponse)) {
            log.info("AI scoring cache hit for key: {}", cacheKey);
            parseAndSetResult(cachedAiResponse, result);
            return;
        }
        //调用AI获取结果
        String lockKey = AI_ANSWER_LOCK_PREFIX + cacheKey; // 每个缓存 key 独立锁
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 等待最多 3 秒抢锁，持有锁 15 秒自动释放（防止死锁）
            boolean locked = lock.tryLock(3, 15, TimeUnit.SECONDS);
            if (locked) {
                cachedAiResponse = aiScoringCache.getIfPresent(cacheKey);
                if (StrUtil.isNotBlank(cachedAiResponse)) {
                    log.info("AI scoring cache hit for key: {}", cacheKey);
                    parseAndSetResult(cachedAiResponse, result);
                    return;
                }
                for (int i = 0; i < questions.size(); i++) {
                    String title = questions.get(i).getQuestionContent();
                    // 获取用户对该题的答案 key（可能越界或为空）
                    String choiceKey = null;
                    if (i < choices.size()) {
                        choiceKey = choices.get(i);
                    }
                    String userAnswerValue = null;
                    if (choiceKey != null) {
                        List<QuestionOption> optionList = questions.get(i).getOptions();
                        if (CollUtil.isNotEmpty(optionList)) {
                            for (QuestionOption option : optionList) {
                                if (choiceKey.equals(option.getKey())) {
                                    userAnswerValue=option.getValue();
                                    break;
                                }
                            }
                        }
                    }
                    QuestionAnswerDTO questionAnswerDTO=new QuestionAnswerDTO();
                    questionAnswerDTO.setTitle(title);
                    questionAnswerDTO.setUserAnswer(userAnswerValue);
                    questionAnswerDTOList.add(questionAnswerDTO);
                }
                log.info("questionAnswerDTOList:{}", questionAnswerDTOList);
                // 真正调用 AI 并写入缓存
                log.info("Calling AI service for key: {}", cacheKey);
                // 构造 userMessage
                // 构造 userMessage
                String userMessage = getAiTestScoringUserMessage(app, questionAnswerDTOList);
                // 调用AI
                String aiResponse = qwenClient.chat(
                        AI_TEST_SCORING_SYSTEM_MESSAGE,
                        userMessage, dashscopeApiKey
                );
                log.info("aiResponse:{}", aiResponse);
                // 解析 AI 返回的 JSON
                parseAndSetResult(aiResponse, result);
                // 写入缓存（仅当解析成功时）
                aiScoringCache.put(cacheKey, aiResponse);
            }
            else {
                // 抢锁失败：可能是超时，此时可选择：
                // 方案1（推荐）：直接返回兜底结果（避免雪崩）
                // 方案2：重试 or 报错
                log.warn("Failed to acquire lock for key: {}, returning fallback result", cacheKey);
                result.setResultName("系统繁忙");
                result.setResultDesc("评分服务请求过多，请稍后重试。");
            }
        }catch (Exception e) {
            log.error("AI scoring failed", e);
            result.setResultName("系统错误");
            result.setResultDesc("评分服务暂时不可用，请稍后重试。");
        } finally {
            // 安全释放锁
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String buildCacheKey(Long appId, String choicesStr) {
        return DigestUtils.md5DigestAsHex((appId + ":" + choicesStr).getBytes());
    }

    private void parseAndSetResult(String cachedAiResponse, ScoringResult result) {
        try {
            JSONObject jsonObject = JSONUtil.parseObj(cachedAiResponse);
            String resultName = jsonObject.getStr("resultName");
            String resultDesc = jsonObject.getStr("resultDesc");
            String scoreStr = jsonObject.getStr("resultScore");
            Integer resultScore = scoreStr != null ? Integer.valueOf(scoreStr) : null;
            // 校验关键字段是否存在
            if (StrUtil.isBlank(resultName) || StrUtil.isBlank(resultDesc) || ObjUtil.isNull(resultScore)) {
                throw new RuntimeException("AI response missing 'resultName' or 'resultDesc' or 'resultScore'");
            }
            // 填充结果
            result.setResultName(resultName);
            result.setResultDesc(resultDesc);
            result.setResultScore(resultScore);
        } catch (Exception e) {
            log.warn("Failed to parse AI scoring response: {}", cachedAiResponse, e);
            // 解析失败时提供兜底文案
            result.setResultName("评分失败");
            result.setResultDesc("AI 返回结果格式异常，请稍后重试。");
        }
    }
    private String getAiTestScoringUserMessage(App app, List<QuestionAnswerDTO> questionAnswerDTOList) {
        StringBuilder userMessage = new StringBuilder();
        userMessage.append(app.getAppName()).append("\n");
        userMessage.append(app.getAppDesc()).append("\n");
        userMessage.append(JSONUtil.toJsonStr(questionAnswerDTOList));
        return userMessage.toString();
    }
}
