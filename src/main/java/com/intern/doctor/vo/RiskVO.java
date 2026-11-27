package com.intern.doctor.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一条风险记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskVO {
    /** 风险等级：HIGH / MEDIUM / LOW */
    private String level;
    /** 风险类型，如 密钥泄露、硬编码路径 */
    private String type;
    /** 命中的文件路径 */
    private String file;
    /** 命中行号，0 表示文件级命中 */
    private int line;
    /** 说明（密钥脱敏，不回显原文） */
    private String message;
}
