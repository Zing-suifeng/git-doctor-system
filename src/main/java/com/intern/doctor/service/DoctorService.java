package com.intern.doctor.service;

import com.intern.doctor.vo.CheckReportVO;

/**
 * 体检编排服务
 */
public interface DoctorService {
    /**
     * 对一个 GitHub 仓库做接入体检
     *
     * @param repoUrl 仓库地址
     * @return 体检报告
     */
    CheckReportVO check(String repoUrl);
}
