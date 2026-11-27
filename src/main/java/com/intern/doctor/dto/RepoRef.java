package com.intern.doctor.dto;

/**
 * 仓库坐标：平台 + owner + repo
 * 从用户贴进来的网址里解析出来
 */
public class RepoRef {
    /** github 或 gitee */
    private final String platform;
    private final String owner;
    private final String repo;

    public RepoRef(String platform, String owner, String repo) {
        this.platform = platform;
        this.owner = owner;
        this.repo = repo;
    }

    public String getPlatform() { return platform; }
    public String getOwner() { return owner; }
    public String getRepo() { return repo; }
    public boolean isGitee() { return "gitee".equals(platform); }
}
