package com.intern.doctor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.intern.doctor.config.AiProps;
import com.intern.doctor.vo.CheckReportVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调大模型给接入建议。OpenAI 兼容格式，DeepSeek / 其他兼容服务都能用。
 * 这是可选功能：没配 key、超时、报错都返回 null，绝不影响核心评分。
 */
@Component
public class AiAdviceClient {
    private static final Logger log = LoggerFactory.getLogger(AiAdviceClient.class);

    private final AiProps props;
    private final WebClient webClient;

    public AiAdviceClient(AiProps props) {
        this.props = props;
        this.webClient = WebClient.builder().build();
    }

    /** 生成接入建议；任何异常都吞掉返回 null */
    public String advise(CheckReportVO report) {
        if (!props.available()) return null;
        try {
            String base = props.getApiBase().replaceAll("/+$", "");
            JsonNode content = webClient.post()
                    .uri(base + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(buildBody(report))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                    .block();

            if (content == null) return null;
            String advice = content.path("choices").path(0).path("message").path("content").asText("");
            return advice.isBlank() ? null : advice.trim();
        } catch (Exception e) {
            log.warn("AI 建议调用失败，已跳过：{}", e.getMessage());
            return null;
        }
    }

    /** 把体检结果拼成给模型的提示词 */
    private Map<String, Object> buildBody(CheckReportVO report) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是代码仓库接入评审助手。下面是一个仓库的体检结果，请给出简短、具体的接入建议。\n");
        sb.append("要求：先说结论（能不能接入），再列2-4条最该优先整改的点，别重复分数明细，别编造仓库里没有的东西，200字以内。\n\n");
        sb.append("仓库：").append(report.getRepoFullName()).append("\n");
        sb.append("就绪度评分：").append(report.getScore()).append("/100，").append(report.getLevel()).append("\n");
        sb.append("各项得分：\n");
        report.getDimensions().forEach(d ->
                sb.append("- ").append(d.getName()).append(" ").append(d.getScore()).append("/").append(d.getFullScore()).append("\n"));
        sb.append("主要风险：\n");
        report.getRisks().stream().limit(15).forEach(r ->
                sb.append("- [").append(r.getLevel()).append("] ").append(r.getType())
                        .append(" @ ").append(r.getFile()).append("\n"));

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", sb.toString()));

        Map<String, Object> body = new HashMap<>();
        body.put("model", props.getModel());
        body.put("messages", messages);
        body.put("temperature", 0.3);
        return body;
    }
}
