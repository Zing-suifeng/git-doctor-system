package com.intern.doctor.checker;

import com.intern.doctor.dto.RepoFile;
import com.intern.doctor.vo.RiskVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 检测 3：硬编码路径（C:\ 、/Users/ 、/home/ 等）
 * 写死绝对路径会导致换环境后找不到文件，占 15 分（没有则满分）
 */
@Component
public class HardcodedPathChecker implements Checker {

    // Windows 盘符路径：C:\xxx  或  C:/xxx
    private static final Pattern WIN_PATH = Pattern.compile("[A-Za-z]:[\\\\/](?!\\s)[\\w\\-.\\\\/ ]{3,}");
    // macOS 用户目录：/Users/xxx
    private static final Pattern MAC_PATH = Pattern.compile("/Users/[\\w\\-.]+");
    // Linux home 目录：/home/xxx（排除 /home/ 后直接跟变量的情况由人工判断）
    private static final Pattern LINUX_PATH = Pattern.compile("/home/[\\w\\-.]+");

    @Override
    public String name() { return "硬编码路径"; }

    @Override
    public int fullScore() { return 15; }

    @Override
    public int check(List<RepoFile> files, List<RiskVO> risks) {
        int hits = 0;
        for (RepoFile f : files) {
            String content = f.getContent();
            if (content == null || content.isEmpty()) continue;
            String[] lines = content.split("\\R");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (matchesAny(line)) {
                    hits++;
                    if (hits <= 20) {
                        risks.add(new RiskVO("MEDIUM", "硬编码路径", f.getPath(), i + 1,
                                "疑似写死绝对路径，换环境后可能失效：" + mask(line.trim())));
                    }
                }
            }
        }
        return Math.max(0, fullScore() - hits * 3);
    }

    private boolean matchesAny(String line) {
        return WIN_PATH.matcher(line).find()
                || MAC_PATH.matcher(line).find()
                || LINUX_PATH.matcher(line).find();
    }

    /** 只保留前 80 个字符，避免把整行代码塞进报告 */
    private String mask(String s) {
        return s.length() > 80 ? s.substring(0, 80) + "..." : s;
    }
}
