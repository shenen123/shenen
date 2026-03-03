package com.liubinrui.enums;

import org.springframework.util.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用类型枚举（对应出题策略）
 */
public enum AppTypeEnum {

    MBTI(0, "MBTI性格测试"),        // 测评类
    MATH(1, "小学数学"),           // 得分类
    CALCULUS(2, "微积分");         // 得分类

    private final Integer value;
    private final String text;

    AppTypeEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    public static AppTypeEnum getEnumByValue(Integer value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (AppTypeEnum type : AppTypeEnum.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }

    public static List<Integer> getValues() {
        return Arrays.stream(values())
                .map(AppTypeEnum::getValue)
                .collect(Collectors.toList());
    }

    public Integer getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}