package com.intern.doctor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 代码托管平台的可配置项（同时支持 GitHub 和 Gitee）
 */
@Component
@ConfigurationProperties(prefix = "doctor.repo")
public class RepoProps {
    /** GitHub API 根地址 */
    private String githubApiBase = "https://api.github.com";
    /** Gitee API 根地址 */
    private String giteeApiBase = "https://gitee.com/api/v5";
    /** GitHub token（可选，提高限流） */
    private String githubToken = "";
    /** Gitee token（可选，私有仓库或提高限流） */
    private String giteeToken = "";
    /** 最多纳入检测的文件数（文件名层面），防止超大仓库爆内存 */
    private int maxFiles = 1500;
    /** 单文件最大读取字节数 */
    private int maxFileBytes = 200000;
    /** 每类代码文件最多抽样读取几个内容（控制 API 调用量） */
    private int codeSamplePerType = 5;
    /** 需要读内容的文件总数上限（含 README/.env/配置/代码抽样） */
    private int maxContentFiles = 40;

    public String getGithubApiBase() { return githubApiBase; }
    public void setGithubApiBase(String githubApiBase) { this.githubApiBase = githubApiBase; }
    public String getGiteeApiBase() { return giteeApiBase; }
    public void setGiteeApiBase(String giteeApiBase) { this.giteeApiBase = giteeApiBase; }
    public String getGithubToken() { return githubToken; }
    public void setGithubToken(String githubToken) { this.githubToken = githubToken; }
    public String getGiteeToken() { return giteeToken; }
    public void setGiteeToken(String giteeToken) { this.giteeToken = giteeToken; }
    public int getMaxFiles() { return maxFiles; }
    public void setMaxFiles(int maxFiles) { this.maxFiles = maxFiles; }
    public int getMaxFileBytes() { return maxFileBytes; }
    public void setMaxFileBytes(int maxFileBytes) { this.maxFileBytes = maxFileBytes; }
    public int getCodeSamplePerType() { return codeSamplePerType; }
    public void setCodeSamplePerType(int codeSamplePerType) { this.codeSamplePerType = codeSamplePerType; }
    public int getMaxContentFiles() { return maxContentFiles; }
    public void setMaxContentFiles(int maxContentFiles) { this.maxContentFiles = maxContentFiles; }
}
