package com.intern.doctor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 接入建议的可配置项（可选功能，默认关闭）。
 * key 走环境变量 DEEPSEEK_API_KEY，不写进文件。
 */
@Component
@ConfigurationProperties(prefix = "doctor.ai")
public class AiProps {
    /** 是否启用 AI 建议；没配 key 时即使 true 也不会调用 */
    private boolean enabled = false;
    /** OpenAI 兼容的服务根地址 */
    private String apiBase = "https://api.deepseek.com";
    /** 模型名 */
    private String model = "deepseek-chat";
    /** API key，从环境变量注入 */
    private String apiKey = "";
    /** 调用超时秒数，超时就跳过 */
    private int timeoutSeconds = 15;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getApiBase() { return apiBase; }
    public void setApiBase(String apiBase) { this.apiBase = apiBase; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    /** key 为空或没开启就直接跳过，保证开箱即用 */
    public boolean available() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
