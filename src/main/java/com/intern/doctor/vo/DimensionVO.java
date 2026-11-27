package com.intern.doctor.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个检测维度的结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DimensionVO {
    /** 维度名称，如 README 完整度 */
    private String name;
    /** 该项得分 */
    private int score;
    /** 该项满分 */
    private int fullScore;
    /** 说明 */
    private String detail;
}
