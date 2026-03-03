package com.liubinrui.strategy.question;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.liubinrui.client.QwenClient;
import com.liubinrui.common.ErrorCode;
import com.liubinrui.enums.AppTypeEnum;
import com.liubinrui.exception.ThrowUtils;
import com.liubinrui.model.dto.question.QuestionAnswerDTO;
import com.liubinrui.model.dto.scoring_result.ScoringResultAddRequest;
import com.liubinrui.model.entity.*;
import com.liubinrui.service.AppService;
import com.liubinrui.service.QuestionService;
import com.liubinrui.service.UserAnswerService;
import com.liubinrui.strategy.ScoringStrategy;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class MBTIScoringStrategy implements ScoringStrategy {
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
    public static final String AI_TEST_SCORING_SYSTEM_MESSAGE =
            "你是一位专业的 MBTI 性格分析师。我会提供以下信息：\n" +
                    "1. 应用名称\n" +
                    "2. 应用描述（通常为 MBTI 性格测试）\n" +
                    "3. 用户对一系列题目的回答列表，格式为：[{\"title\": \"题目内容\", \"answer\": \"用户选择的答案\"}]\n" +
                    "\n" +
                    "请你根据用户的回答，分析其最可能的 MBTI 性格类型（必须是 4 个大写字母，如 ISTJ、ENFP、INTP 等）。\n" +
                    "\n" +
                    "【输出要求】\n" +
                    "- resultName：填写 MBTI 类型（例如 \"ISTJ\"），不要加任何额外文字；\n" +
                    "- resultDesc：对该性格类型的详细解读，包括行为特征、优势、潜在挑战等，不少于 200 字；\n" +
                    "- 必须严格以纯 JSON 格式输出，且仅包含一个 JSON 对象；\n" +
                    "- 不要包含任何解释、注释、Markdown、代码块或额外字符；\n" +
                    "- 输出必须以 { 开头，以 } 结尾。\n" +
                    "\n" +
                    "【正确输出示例】\n" +
                    "{\"resultName\": \"ISTJ\", \"resultDesc\": \"你是一个注重实际、有责任感的人...（此处为 200+ 字描述）\"}\n" +
                    "\n" +
                    "现在，请开始分析并直接输出 JSON：";
    //构造本地缓存
    private final Cache<String, String> aiScoringCache = Caffeine.newBuilder()
            .initialCapacity(1024)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    //分布式锁
    private static final String AI_ANSWER_LOCK_PREFIX = "lock:ai_answer:";

    @Override
    public boolean supports(Integer appType) {
        return AppTypeEnum.MBTI.getValue().equals(appType);
    }

    @Override
    public void execute(ScoringResultAddRequest request, ScoringResult result) {
        Long appId = request.getAppId();
        App app = appService.getById(appId);
        Long answerId = request.getAnswerId();
        ThrowUtils.throwIf(ObjUtil.isNull(answerId), ErrorCode.NOT_FOUND_ERROR);
        UserAnswer userAnswer = userAnswerService.getById(answerId);
        log.info("userAnswer:{}", userAnswer);
        List<String> choices = userAnswer.getChoices();
        List<Question> questions = questionService.getQuestions(appId);
        //返回给AI题目和用户答案
        List<QuestionAnswerDTO> questionAnswerDTOList = new ArrayList<>();
        String choicesStr = JSONUtil.toJsonStr(choices != null ? choices : new ArrayList<>());
        // 构建缓存 key
        String cacheKey = buildCacheKey(appId, choicesStr);
        log.info("cacheKey:{}", cacheKey);
        // 尝试从缓存获取 AI 结果
        String cachedAiResponse = aiScoringCache.getIfPresent(cacheKey);
        if (cachedAiResponse != null) {
            log.info("AI scoring cache hit for key: {}", cacheKey);
            parseAndSetResult(cachedAiResponse, result);
            return;
        }
        // ========== 缓存未命中，加分布式锁防止击穿 ==========
        String lockKey = AI_ANSWER_LOCK_PREFIX + cacheKey;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 等待最多 3 秒抢锁，持有锁 15 秒自动释放（防止死锁）
            boolean locked = lock.tryLock(3, 15, TimeUnit.SECONDS);
            if (locked) {
                // 抢到锁后，**再次检查缓存**（double-check）,因为可能在等待锁的过程中，其他线程已写入缓存
                cachedAiResponse = aiScoringCache.getIfPresent(cacheKey);
                if (cachedAiResponse != null) {
                    log.info("AI scoring cache hit for key: {}", cacheKey);
                    parseAndSetResult(cachedAiResponse, result);
                    return;
                }
                //调用AI
                for (int i = 0; i < questions.size(); i++) {
                    String title = questions.get(i).getQuestionContent();
                    // 获取用户对该题的答案 key（可能越界或为空）
                    String choiceKey = null;
                    if (i < choices.size())
                        choiceKey = choices.get(i);
                    String userAnswerValue = null;
                    // 如果有作答（非空 key），则查找 value
                    if (StrUtil.isNotBlank(choiceKey)) {
                        List<QuestionOption> optionList = questions.get(i).getOptions();
                        if (optionList != null) {
                            for (QuestionOption questionOption : optionList) {
                                if (choiceKey.equals(questionOption.getKey())){
                                    userAnswerValue = questionOption.getValue();
                                    break;
                                }
                            }
                        }
                    }
                    // 无论是否作答，都添加到 DTO 列表（未作答时 userAnswer 为 null 或 ""）
                    QuestionAnswerDTO dto = new QuestionAnswerDTO();
                    dto.setTitle(title);
                    dto.setUserAnswer(userAnswerValue); // 未作答时为 null
                    questionAnswerDTOList.add(dto);
                }
                log.info("questionAnswerDTOList:{}", questionAnswerDTOList);
                // 真正调用 AI 并写入缓存
                log.info("Calling AI service for key: {}", cacheKey);
                // 构造 userMessage
                String userMessage = getAiTestScoringUserMessage(app,questionAnswerDTOList);
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
        } catch (Exception e) {
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

    private String getAiTestScoringUserMessage(App app, List<QuestionAnswerDTO> questionAnswerDTOList) {
        StringBuilder userMessage = new StringBuilder();
        userMessage.append(app.getAppName()).append("\n");
        userMessage.append(app.getAppDesc()).append("\n");
        userMessage.append(JSONUtil.toJsonStr(questionAnswerDTOList));
        return userMessage.toString();
    }

    private String buildCacheKey(Long appId, String choicesJson) {
        // 使用 Hutool 的 DigestUtil 计算 MD5，确保 key 稳定且无敏感信息
        return DigestUtil.md5Hex(appId + ":" + choicesJson);
    }

    private void parseAndSetResult(String aiResponse, ScoringResult result) {
        try {
            // 使用 Hutool 解析 JSON
            JSONObject jsonObject = JSONUtil.parseObj(aiResponse);

            String resultName = jsonObject.getStr("resultName");
            String resultDesc = jsonObject.getStr("resultDesc");

            // 校验关键字段是否存在
            if (StrUtil.isBlank(resultName) || StrUtil.isBlank(resultDesc)) {
                throw new RuntimeException("AI response missing 'resultName' or 'resultDesc'");
            }

            // 填充结果
            result.setResultName(resultName);
            result.setResultDesc(resultDesc);
            result.setResultPicture(""); // 可选，若 AI 不返回图片可留空

        } catch (Exception e) {
            log.warn("Failed to parse AI scoring response: {}", aiResponse, e);
            // 解析失败时提供兜底文案
            result.setResultName("评分失败");
            result.setResultDesc("AI 返回结果格式异常，请稍后重试。");
        }
    }
}
