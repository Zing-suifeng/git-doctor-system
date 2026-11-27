package com.intern.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 体检请求
 */
public class CheckRequest {

    @NotBlank(message = "仓库地址不能为空")
    @Pattern(regexp = "^https://(github|gitee)\\.com/[\\w.-]+/[\\w.-]+/?.*$",
             message = "请填写 GitHub 或 Gitee 公开仓库地址，例如 https://gitee.com/owner/repo")
    private String repoUrl;

    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
}
