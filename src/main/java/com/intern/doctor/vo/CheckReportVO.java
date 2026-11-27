package com.intern.doctor.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 体检报告
 */
@Data
public class CheckReportVO {
    /** 仓库全名 owner/repo */
    private String repoFullName;
    /** 接入就绪度评分 0-100 */
    private int score;
    /** 结论：可接入 / 需整改 / 高风险 */
    private String level;
    /** 一句话总结 */
    private String summary;
    /** 各项检测得分 */
    private List<DimensionVO> dimensions = new ArrayList<>();
    /** 风险清单 */
    private List<RiskVO> risks = new ArrayList<>();
    /** 体检时间 */
    private LocalDateTime checkedAt;
    /** AI 生成的接入建议（可选，没配 key 时为 null） */
    private String aiAdvice;
}
