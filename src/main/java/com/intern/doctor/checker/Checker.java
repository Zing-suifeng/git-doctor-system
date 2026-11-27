package com.intern.doctor.checker;

import com.intern.doctor.dto.RepoFile;
import com.intern.doctor.vo.DimensionVO;
import com.intern.doctor.vo.RiskVO;

import java.util.List;

/**
 * 检测器接口：每个检测项实现这个接口，便于扩展
 */
public interface Checker {
    /** 维度名称 */
    String name();

    /** 该项满分 */
    int fullScore();

    /** 执行检测，返回该维度得分 */
    int check(List<RepoFile> files, List<RiskVO> risks);

    /** 包装成 VO */
    default DimensionVO checkToVO(List<RepoFile> files, List<RiskVO> risks) {
        int score = check(files, risks);
        return new DimensionVO(name(), score, fullScore(), detail(score));
    }

    /** 根据得分生成说明，子类可覆盖 */
    default String detail(int score) {
        if (score == fullScore()) return "完整";
        if (score > 0) return "部分缺失";
        return "缺失";
    }
}
