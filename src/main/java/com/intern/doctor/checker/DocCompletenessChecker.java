package com.intern.doctor.checker;

import com.intern.doctor.dto.RepoFile;
import com.intern.doctor.vo.RiskVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * 检测 1：README / 依赖清单 / 启动脚本 的完整度
 * 三项各占 13 分，共 40 分
 */
@Component
public class DocCompletenessChecker implements Checker {

    private static final String[] DEPENDENCY_FILES = {
            "pom.xml", "package.json", "requirements.txt", "go.mod", "build.gradle", "Cargo.toml"
    };
    private static final String[] START_FILES = {
            "start.sh", "run.sh", "makefile", "docker-compose.yml", "docker-compose.yaml", "start.bat"
    };

    @Override
    public String name() { return "文档与脚本完整度"; }

    @Override
    public int fullScore() { return 40; }

    @Override
    public int check(List<RepoFile> files, List<RiskVO> risks) {
        int score = 0;

        // README：存在 + 内容超过一定长度
        boolean readmeOk = files.stream().anyMatch(f -> {
            String n = f.getPath().toLowerCase(Locale.ROOT);
            return n.endsWith("readme.md") && f.getContent() != null && f.getContent().length() > 100;
        });
        if (readmeOk) score += 13;
        else risks.add(new RiskVO("LOW", "文档缺失", "README.md", 0, "未发现有效的 README（缺失或内容过短）"));

        // 依赖清单
        boolean depOk = files.stream().anyMatch(f -> {
            String n = f.getPath().toLowerCase(Locale.ROOT);
            for (String d : DEPENDENCY_FILES) if (n.endsWith(d)) return true;
            return false;
        });
        if (depOk) score += 13;
        else risks.add(new RiskVO("LOW", "依赖清单缺失", "-", 0, "未发现常见依赖清单文件"));

        // 启动脚本
        boolean startOk = files.stream().anyMatch(f -> {
            String n = f.getPath().toLowerCase(Locale.ROOT);
            for (String s : START_FILES) if (n.endsWith(s)) return true;
            return false;
        });
        if (startOk) score += 14;
        else risks.add(new RiskVO("LOW", "启动脚本缺失", "-", 0, "未发现常见启动脚本或 compose 文件"));

        return score;
    }

    @Override
    public String detail(int score) {
        if (score == 40) return "README、依赖清单、启动脚本齐全";
        if (score >= 20) return "文档或脚本部分缺失";
        return "关键文档与脚本大面积缺失";
    }
}
