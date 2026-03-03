package com.liubinrui;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.liubinrui.model.entity.UserAnswer;
import com.liubinrui.service.UserAnswerService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
@SpringBootTest(properties = "spring.cache.type=simple")

class LldatiApplicationTests {

    @Resource
    private UserAnswerService userAnswerService;

    @Test
    void testSharding() {
        // 只插入 appId 字段，验证分表路由
        UserAnswer answer = new UserAnswer();
        answer.setAppId(1L); // 应路由到 user_answer_1
        answer.setUserId(1L);
        userAnswerService.save(answer);

        // 查询验证
        UserAnswer result = userAnswerService.getOne(Wrappers.lambdaQuery(UserAnswer.class).eq(UserAnswer::getAppId, 1L));
        System.out.println("查询结果：" + result);
    }

}
