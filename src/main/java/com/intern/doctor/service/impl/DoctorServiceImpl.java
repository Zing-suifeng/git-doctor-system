package com.intern.doctor.service.impl;

import com.intern.doctor.checker.Checker;
import com.intern.doctor.config.RepoProps;
import com.intern.doctor.dto.RepoFile;
import com.intern.doctor.dto.RepoRef;
import com.intern.doctor.service.AiAdviceClient;
import com.intern.doctor.service.DoctorService;
import com.intern.doctor.service.RepoApiClient;
import com.intern.doctor.vo.CheckReportVO;
import com.intern.doctor.vo.DimensionVO;
import com.intern.doctor.vo.RiskVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 体检流程编排：
 * 解析地址 → 拿仓库信息 → 拿文件树 → 读文件内容 → 逐个检测器跑 → 汇总评分
 */
@Service
public class DoctorServiceImpl implements DoctorService {
    private static final Logger log = LoggerFactory.getLogger(DoctorServiceImpl.class);

    private final RepoApiClient repoApi;
    private final RepoProps props;
    private final List<Checker> checkers;
    private final AiAdviceClient aiClient;

    /** 不带 AI 客户端的构造（兼容无 AI 依赖的场景） */
    public DoctorServiceImpl(RepoApiClient repoApi, RepoProps props, List<Checker> checkers) {
        this(repoApi, props, checkers, null);
    }

    /** Spring 注入：带可选的 AI 客户端 */
    @org.springframework.beans.factory.annotation.Autowired
    public DoctorServiceImpl(RepoApiClient repoApi, RepoProps props, List<Checker> checkers, AiAdviceClient aiClient) {
        this.repoApi = repoApi;
        this.props = props;
        this.checkers = checkers;
        this.aiClient = aiClient;
    }

    @Override
    public CheckReportVO check(String repoUrl) {
        // 1. 解析地址
        RepoRef ref = repoApi.parseRepo(repoUrl);

        // 2. 仓库信息 + 文件树
        RepoApiClient.RepoMeta meta = repoApi.fetchMeta(ref);
        List<RepoApiClient.TreeItem> tree = repoApi.fetchTree(ref, meta.defaultBranch());

        // 3. 拉取文件内容（限量，避免把 API 打爆）
        List<RepoFile> files = loadFiles(ref, tree);

        // 4. 跑所有检测器
        List<RiskVO> risks = new ArrayList<>();
        List<DimensionVO> dims = new ArrayList<>();
        int total = 0, full = 0;
        for (Checker checker : checkers) {
            dims.add(checker.checkToVO(files, risks));
            total += checker.fullScore();
        }
        for (DimensionVO d : dims) full += d.getScore();

        // 5. 算总分（换算成 100 分制）
        int score = total == 0 ? 0 : (int) Math.round(full * 100.0 / total);

        // 6. 组装报告
        CheckReportVO report = new CheckReportVO();
        report.setRepoFullName(meta.fullName());
        report.setScore(score);
        report.setLevel(levelOf(score));
        report.setSummary(summaryOf(score, meta.fullName()));
        report.setDimensions(dims);
        report.setRisks(sortRisks(risks));
        report.setCheckedAt(LocalDateTime.now());

        // 7. AI 接入建议（可选，失败不影响核心评分）
        if (aiClient != null) {
            String advice = aiClient.advise(report);
            if (advice != null) report.setAiAdvice(advice);
        }

        log.info("体检完成 repo={} score={} risks={} aiAdvice={}", meta.fullName(), score, risks.size(), report.getAiAdvice() != null);
        return report;
    }

    /**
     * 分级拉取文件内容，控制 API 调用量：
     * 文件名即可判定的检测（Windows 脚本、依赖清单、启动脚本）只留路径、不发 API；
     * 必须读内容的检测（README、硬编码路径、密钥）按扩展名分桶抽样，
     * 高价值文件（README、.env、配置、启动脚本）必读，总量受 maxContentFiles 限制。
     */
    private List<RepoFile> loadFiles(RepoRef ref, List<RepoApiClient.TreeItem> tree) {
        // 先按 max-files 截断树：大仓库（比如误传 node_modules）只保留前 N 个路径，防内存爆
        List<RepoApiClient.TreeItem> bounded = tree.stream()
                .limit(props.getMaxFiles())
                .toList();

        // 第一级：文件路径进 files（文件名检测用），content 暂时为空
        List<RepoFile> files = new ArrayList<>(bounded.size());
        for (RepoApiClient.TreeItem item : bounded) {
            files.add(new RepoFile(item.path(), ""));
        }

        // 第二级：在截断后的范围内挑「值得读内容」的文件
        Set<String> toRead = selectContentFiles(bounded);
        log.info("文件树共 {} 个文件（纳入 {} 个），抽样读取 {} 个内容（省 API 调用）",
                tree.size(), bounded.size(), toRead.size());

        // 按路径建索引，读到内容后回填（保留原顺序）
        Map<String, RepoFile> byPath = new HashMap<>();
        for (RepoFile f : files) byPath.put(f.getPath(), f);
        for (RepoApiClient.TreeItem item : bounded) {
            if (!toRead.contains(item.path())) continue;
            String content = repoApi.fetchFileContent(ref, item.sha());
            RepoFile target = byPath.get(item.path());
            if (target != null && content != null) target.setContent(content);
        }
        return files;
    }

    /**
     * 挑选需要读内容的文件：
     * 1) 高价值名单：README、.env、依赖清单、启动脚本、常见配置文件——必读；
     * 2) 代码文件按扩展名分桶，每桶抽样 codeSamplePerType 个——代表性读；
     * 3) 锁文件、二进制、图片——跳过（既是省调用，也消除 package-lock 误报）。
     */
    private Set<String> selectContentFiles(List<RepoApiClient.TreeItem> tree) {
        Set<String> result = new LinkedHashSet<>();

        // 必读的高价值文件（按文件名判断）
        for (RepoApiClient.TreeItem item : tree) {
            String name = item.path().toLowerCase(Locale.ROOT);
            if (isHighValue(name) && !isSkipped(name)) {
                result.add(item.path());
                if (result.size() >= props.getMaxContentFiles()) return result;
            }
        }

        // 代码文件按扩展名分桶，每桶限量抽样
        Map<String, Integer> bucketCount = new HashMap<>();
        for (RepoApiClient.TreeItem item : tree) {
            if (result.size() >= props.getMaxContentFiles()) break;
            String name = item.path().toLowerCase(Locale.ROOT);
            String ext = extensionOf(name);
            if (ext == null || !isCodeExtension(ext) || isSkipped(name)) continue;
            int used = bucketCount.getOrDefault(ext, 0);
            if (used >= props.getCodeSamplePerType()) continue;
            bucketCount.put(ext, used + 1);
            result.add(item.path());
        }
        return result;
    }

    /** 高价值文件：文档、密钥配置、依赖清单、启动脚本 */
    private boolean isHighValue(String lowerName) {
        String base = lowerName.substring(lowerName.lastIndexOf('/') + 1);
        if (base.startsWith("readme")) return true;
        if (base.equals(".env") || base.startsWith(".env.")) return true;
        if (base.equals("dockerfile") || base.startsWith("docker-compose")) return true;
        for (String must : new String[]{"pom.xml", "package.json", "requirements.txt", "go.mod",
                "build.gradle", "cargo.toml", "makefile", "start.sh", "run.sh"}) {
            if (base.equals(must)) return true;
        }
        // 配置文件：密钥常常藏在这里
        for (String ext : new String[]{".yml", ".yaml", ".properties", ".toml", ".ini", ".conf"}) {
            if (base.endsWith(ext)) return true;
        }
        return false;
    }

    /**
     * 跳过读内容的文件：锁文件（npm 元数据会造成硬编码路径误报）、
     * 二进制、图片、字体、压缩包、日志。这些只做文件名检测。
     */
    private boolean isSkipped(String lowerName) {
        for (String kw : new String[]{"package-lock.json", "yarn.lock", "pnpm-lock.yaml",
                "composer.lock", "cargo.lock", "go.sum"}) {
            if (lowerName.endsWith(kw)) return true;
        }
        String ext = extensionOf(lowerName);
        if (ext == null) return false;
        for (String skip : new String[]{"png", "jpg", "jpeg", "gif", "svg", "ico", "bmp", "webp",
                "pdf", "zip", "tar", "gz", "jar", "class", "o", "so", "dll", "exe",
                "woff", "woff2", "ttf", "eot", "mp3", "mp4", "lock", "log"}) {
            if (ext.equals(skip)) return true;
        }
        return false;
    }

    /** 参与内容检测的常见源码扩展名 */
    private boolean isCodeExtension(String ext) {
        for (String code : new String[]{"java", "js", "ts", "jsx", "tsx", "vue", "py", "go",
                "rb", "php", "c", "cpp", "cs", "xml", "sql", "json", "sh", "bat", "cmd", "ps1"}) {
            if (ext.equals(code)) return true;
        }
        return false;
    }

    /** 取小写文件名的扩展名（无点返回 null） */
    private String extensionOf(String lowerName) {
        String base = lowerName.substring(lowerName.lastIndexOf('/') + 1);
        int dot = base.lastIndexOf('.');
        return dot > 0 && dot < base.length() - 1 ? base.substring(dot + 1) : null;
    }

    /** 按严重程度排序：HIGH > MEDIUM > LOW */
    private List<RiskVO> sortRisks(List<RiskVO> risks) {
        return risks.stream().sorted((a, b) -> {
            int ai = weight(a.getLevel()), bi = weight(b.getLevel());
            return Integer.compare(bi, ai);
        }).toList();
    }

    private int weight(String level) {
        if ("HIGH".equals(level)) return 3;
        if ("MEDIUM".equals(level)) return 2;
        return 1;
    }

    private String levelOf(int score) {
        if (score >= 80) return "可接入";
        if (score >= 60) return "需整改";
        return "高风险";
    }

    private String summaryOf(int score, String repo) {
        if (score >= 80) return "仓库 " + repo + " 整体规范，可进入接入流程";
        if (score >= 60) return "仓库 " + repo + " 基本可用，但存在若干需整改项";
        return "仓库 " + repo + " 存在明显风险，建议整改后再接入";
    }
}
