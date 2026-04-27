package com.picture.backend.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.picture.backend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.picture.backend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.picture.backend.api.aliyunai.model.CreateTextGenPictureRequest;
import com.picture.backend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.picture.backend.exception.BusinessException;
import com.picture.backend.exception.ErrorCode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@ConfigurationProperties(prefix = "aliyunai")
@Data
public class AliYunAiApi {
    // 读取配置文件
    private String apiKey;
    // 生图模型型号
    private String model;
    private static final String baseHttpApiUrl = "https://dashscope.aliyuncs.com/api/v1";

    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建扩图任务
     *
     * @param createOutPaintingTaskRequest
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        if (createOutPaintingTaskRequest == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "扩图参数为空");
        }
        // 发送请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                // 必须开启异步处理，设置为enable。
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }
            // 解析响应
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            String errorCode = response.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                String errorMessage = response.getMessage();
                log.error("AI 扩图失败，errorCode:{}, errorMessage:{}", errorCode, errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图接口响应异常");
            }
            return response;
        }
    }

    /**
     * 查询创建的扩图任务
     *
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务 id 不能为空");
        }
        try (HttpResponse httpResponse = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .execute()) {
            if (!httpResponse.isOk()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }
    }

    /**
     * 创建文生图异步任务
     *
     * @return taskId
     */
    public String createTextGenPictureTask(CreateTextGenPictureRequest request) {
        String prompt = request.getPrompt();
        boolean promptExtend = request.isPromptExtend();
        boolean watermark = request.isWatermark();
        String size = request.getSize();

        // 设置parameters参数
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("prompt_extend", promptExtend);
        parameters.put("watermark", watermark);
        parameters.put("seed", 12345);

        ImageSynthesisParam param =
                ImageSynthesisParam.builder()
                        .apiKey(apiKey)
                        .model(model)
                        .prompt(prompt)
                        .n(1)
                        .size(size)
                        .negativePrompt("")
                        .parameters(parameters)
                        .build();

        ImageSynthesis imageSynthesis = new ImageSynthesis();
        ImageSynthesisResult result = null;
        try {
            result = imageSynthesis.asyncCall(param);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建文生图任务失败");
        }
        // 检查接口级别错误
        if (StrUtil.isNotBlank(result.getCode())) {
            log.error("文生图任务创建失败，错误码: {}, 错误信息: {}",
                    result.getCode(), result.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建文生图任务失败");
        }
        // 检查任务级别错误
        if (StrUtil.isNotBlank(result.getOutput().getCode())) {
            log.error("文生图任务执行失败，错误码: {}, 错误信息: {}",
                    result.getOutput().getCode(), result.getOutput().getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文生图任务执行失败");
        }
        String taskId = result.getOutput().getTaskId();
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文生图任务创建失败,taskId为空");
        }
        log.info("taskId:{}", taskId);
        return taskId;
    }


    /**
     * 等待文生图异步任务结束
     *
     * @param taskId 任务id
     */
    public ImageSynthesisResult getTextGenPictureTask(String taskId) {
        ImageSynthesis imageSynthesis = new ImageSynthesis();
        ImageSynthesisResult result = null;
        try {
            result = imageSynthesis.fetch(taskId, apiKey);
        } catch (ApiException | NoApiKeyException e) {
            log.error("查询文生图任务状态失败，taskId: {}", taskId, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "查询任务状态失败");
        }
        // 检查任务级别错误
        if (StrUtil.isNotBlank(result.getOutput().getCode())) {
            log.error("文生图任务执行异常，错误码: {}, 错误信息: {}",
                    result.getOutput().getCode(), result.getOutput().getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文生图任务执行异常");
        }
        return result;
    }
}
