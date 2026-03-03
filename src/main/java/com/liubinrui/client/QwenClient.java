package com.liubinrui.client;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class QwenClient {

    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
    //系统提示语，用户提示语，apikey
    public String chat(String systemMessage, String userMessage, String apiKey) throws Exception {
        // 构造请求体
        JSONObject requestBody = JSONUtil.createObj()
                .set("model", "qwen-max")
                .set("input", JSONUtil.createObj()
                        .set("messages", JSONUtil.createArray()
                                .put(JSONUtil.createObj().set("role", "system").set("content", systemMessage))
                                .put(JSONUtil.createObj().set("role", "user").set("content", userMessage))))
                .set("parameters", JSONUtil.createObj()
                        .set("result_format", "message"));

        // 创建带超时的 client
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.MINUTES)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toString(), MediaType.get("application/json; charset=utf-8")))
                .build();

        try (Response response = client.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                throw new RuntimeException("Qwen API error: " + response.code() + " - " + response.body().string());
            }

            JSONObject jsonResponse = JSONUtil.parseObj(response.body().string());
            return jsonResponse
                    .getJSONObject("output")
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getStr("content")
                    .trim();
        }
    }

}