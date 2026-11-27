package com.intern.doctor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.intern.doctor.common.BusinessException;
import com.intern.doctor.config.RepoProps;
import com.intern.doctor.dto.RepoFile;
import com.intern.doctor.dto.RepoRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 负责和代码托管平台 API 打交道：解析仓库地址、拿文件树、读文件内容。
 *
 * 同时支持 GitHub 和 Gitee，两家 API 结构基本一致
 * （default_branch / git/trees / git/blobs 都对齐），
 * 差异只在根地址、认证方式和请求头。
 */
@Component
public class RepoApiClient {
    private static final Logger log = LoggerFactory.getLogger(RepoApiClient.class);

    private final WebClient webClient;
    private final RepoProps props;

    public RepoApiClient(WebClient repoWebClient, RepoProps props) {
        this.webClient = repoWebClient;
        this.props = props;
    }

    /**
     * 从网址解析出平台、owner、repo。
     * 支持 https://github.com/owner/repo 和 https://gitee.com/owner/repo
     */
    public RepoRef parseRepo(String url) {
        String path = url.trim();
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        if (path.endsWith(".git")) path = path.substring(0, path.length() - 4);

        String platform;
        String prefix;
        if (path.startsWith("https://github.com/")) {
            platform = "github";
            prefix = "https://github.com/";
        } else if (path.startsWith("https://gitee.com/")) {
            platform = "gitee";
            prefix = "https://gitee.com/";
        } else {
            throw new BusinessException("请填写 https://github.com/owner/repo 或 https://gitee.com/owner/repo 格式的公开仓库地址");
        }

        String rest = path.substring(prefix.length());
        String[] parts = rest.split("/");
        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new BusinessException("仓库地址不完整，需要 owner/repo 两部分");
        }
        return new RepoRef(platform, parts[0], parts[1]);
    }

    /** 拿仓库基本信息（默认分支 + 完整名） */
    public RepoMeta fetchMeta(RepoRef ref) {
        JsonNode node = get(ref, "/repos/" + ref.getOwner() + "/" + ref.getRepo(), JsonNode.class);
        if (node == null) throw new BusinessException("无法获取仓库信息，请检查地址是否正确");

        String branch = node.path("default_branch").asText("master");
        return new RepoMeta(
                node.path("full_name").asText(ref.getOwner() + "/" + ref.getRepo()),
                branch
        );
    }

    /** 递归拿文件树 */
    public List<TreeItem> fetchTree(RepoRef ref, String branch) {
        String uri = "/repos/" + ref.getOwner() + "/" + ref.getRepo() + "/git/trees/" + branch + "?recursive=1";
        JsonNode node = get(ref, uri, JsonNode.class);
        if (node == null || !node.path("tree").isArray()) {
            throw new BusinessException("无法获取仓库文件树（可能是空仓库或分支不存在）");
        }

        List<TreeItem> items = new ArrayList<>();
        for (JsonNode n : node.path("tree")) {
            if (!"blob".equals(n.path("type").asText())) continue; // 只要文件，不要目录
            items.add(new TreeItem(
                    n.path("path").asText(),
                    n.path("sha").asText()
            ));
        }
        return items;
    }

    /**
     * 读取文件内容。两家平台的 blob API 都返回 base64。
     * 二进制文件、超大文件直接跳过（返回空内容），只做文件名层面的检测。
     */
    public String fetchFileContent(RepoRef ref, String sha) {
        try {
            JsonNode node = get(ref, "/repos/" + ref.getOwner() + "/" + ref.getRepo() + "/git/blobs/" + sha, JsonNode.class);
            if (node == null || !node.hasNonNull("content")) return "";
            if (!"base64".equals(node.path("encoding").asText())) return "";

            byte[] bytes = Base64.getDecoder().decode(node.path("content").asText().replace("\n", ""));
            if (bytes.length > props.getMaxFileBytes()) return "";  // 跳过超大文件
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.debug("读取文件失败 sha={}: {}", sha, e.getMessage());
            return "";
        }
    }

    /**
     * 统一的 GET：按平台拼根地址、认证方式和请求头。
     * GitHub token 走 Authorization: Bearer；Gitee token 走 query 参数 access_token。
     */
    private <T> T get(RepoRef ref, String path, Class<T> type) {
        String base = ref.isGitee() ? props.getGiteeApiBase() : props.getGithubApiBase();
        String token = ref.isGitee() ? props.getGiteeToken() : props.getGithubToken();

        String uri = base + path;
        if (ref.isGitee() && token != null && !token.isBlank()) {
            uri += (path.contains("?") ? "&" : "?") + "access_token=" + token;
        }

        try {
            WebClient.RequestHeadersSpec<?> spec = webClient.get().uri(uri);
            if (!ref.isGitee() && token != null && !token.isBlank()) {
                spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
            return spec.retrieve().bodyToMono(type).block();
        } catch (WebClientResponseException.NotFound e) {
            throw new BusinessException("仓库或分支不存在：" + path);
        } catch (WebClientResponseException.TooManyRequests e) {
            throw new BusinessException("API 调用次数超限，请配置 token 后重试", 429);
        } catch (WebClientResponseException e) {
            throw new BusinessException("API 调用失败（" + e.getStatusCode().value() + "）", 502);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("调用平台 API 时出错：" + e.getMessage(), 502);
        }
    }

    /** 仓库元信息 */
    public static record RepoMeta(String fullName, String defaultBranch) {}

    /** 文件树条目 */
    public static record TreeItem(String path, String sha) {}
}
