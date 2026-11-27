package com.intern.doctor.checker;

import com.intern.doctor.dto.RepoFile;
import com.intern.doctor.vo.RiskVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 检测 4：密钥与敏感信息泄露
 * sk- / AKID / Bearer / 私钥头 / .env 入库，占 30 分
 *
 * 安全要点：报告里只回显命中的「类型」和「行号」，绝不回显密钥原文。
 */
@Component
public class SecretLeakChecker implements Checker {

    private static final Pattern OPENAI_KEY = Pattern.compile("sk-[A-Za-z0-9_-]{12,}");
    private static final Pattern ALIYUN_AK = Pattern.compile("(AKID|LTAI)[A-Za-z0-9]{8,}");
    private static final Pattern BEARER = Pattern.compile("Bearer\\s+[A-Za-z0-9._-]{12,}");
    private static final Pattern PRIVATE_KEY = Pattern.compile("-----BEGIN [A-Z ]*PRIVATE KEY-----");
    private static final Pattern GENERIC_SECRET = Pattern.compile(
            "(?i)(password|secret|api[_-]?key|access[_-]?token)\\s*[:=]\\s*[\"']?[A-Za-z0-9_\\-]{8,}");

    @Override
    public String name() { return "密钥泄露"; }

    @Override
    public int fullScore() { return 30; }

    @Override
    public int check(List<RepoFile> files, List<RiskVO> risks) {
        int hits = 0;

        // .env 类文件被提交进仓库
        for (RepoFile f : files) {
            String n = f.getPath().toLowerCase(Locale.ROOT);
            if (n.endsWith(".env") || n.endsWith(".env.local") || n.endsWith(".env.production")) {
                hits++;
                risks.add(new RiskVO("HIGH", "敏感配置文件入库", f.getPath(), 0,
                        "发现 .env 文件，可能包含数据库密码或 API Key"));
            }
        }

        // 内容里的密钥特征
        for (RepoFile f : files) {
            String content = f.getContent();
            if (content == null || content.isEmpty()) continue;
            String[] lines = content.split("\\R");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                String type = detectType(line);
                if (type != null) {
                    hits++;
                    if (hits <= 20) {
                        risks.add(new RiskVO("HIGH", type, f.getPath(), i + 1,
                                "疑似密钥泄露（内容已脱敏，请人工核查该行）"));
                    }
                }
            }
        }
        return Math.max(0, fullScore() - hits * 10);
    }

    /** 返回命中的密钥类型，未命中返回 null；不返回原文 */
    private String detectType(String line) {
        if (OPENAI_KEY.matcher(line).find()) return "OpenAI API Key (sk-)";
        if (ALIYUN_AK.matcher(line).find()) return "阿里云 AccessKey (AKID/LTAI)";
        if (BEARER.matcher(line).find()) return "Bearer Token";
        if (PRIVATE_KEY.matcher(line).find()) return "私钥文件";
        if (GENERIC_SECRET.matcher(line).find()) return "疑似明文口令/密钥";
        return null;
    }
}
