package org.xhy.domain.tool.service.state.impl;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.model.dto.GitHubRepoInfo;
import org.xhy.domain.tool.service.state.ToolStateProcessor;
import org.xhy.infrastructure.exception.BusinessException;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** GitHub URL验证处理器 */
public class GithubUrlValidateProcessor implements ToolStateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(GithubUrlValidateProcessor.class);

    // GitHub URL正则表达式验证，捕获 owner, repo, type (tree/blob), ref (分支/tag), 和 repo内路径
    private static final Pattern GITHUB_URL_PATTERN = Pattern
            .compile("^https://github\\.com/([\\w.-]+)/([\\w.-]+)(?:/(tree|blob)/([\\w.-]+)(/(.*))?)?$");

    @Override
    public ToolStatus getStatus() {
        return ToolStatus.GITHUB_URL_VALIDATE;
    }

    @Override
    public void process(ToolEntity tool) {
        String uploadUrl = tool.getUploadUrl();
        GitHubRepoInfo repoInfo = parseAndValidateGithubUrl(uploadUrl);

        // （可选）可以将解析出的 repoInfo 存入 ToolEntity 的某个临时字段或关联对象，
        // 以便后续 ToolPublishingService 使用，避免重复解析。这里暂时不存。
        // tool.setParsedRepoInfo(repoInfo);
    }

    /** 解析并验证GitHub URL。 如果验证成功，返回 GitHubRepoInfo 对象。 如果失败，抛出 BusinessException。 */
    public static GitHubRepoInfo parseAndValidateGithubUrl(String githubUrl) {
        if (githubUrl == null || githubUrl.trim().isEmpty()) {
            throw new BusinessException("GitHub URL不能为空");
        }

        Matcher matcher = GITHUB_URL_PATTERN.matcher(githubUrl);
        if (!matcher.matches()) {
            throw new BusinessException("无效的GitHub URL格式: " + githubUrl);
        }

        String owner = matcher.group(1);
        String repoName = matcher.group(2);
        String ref = matcher.group(4); // 分支、标签名. 如果URL是根，则为null
        String pathInRepoWithLeadingSlash = matcher.group(5); // 仓库内路径，例如 "/src" 或 "/file.txt"，如果URL指向ref根则为null
        String pathInRepo = (pathInRepoWithLeadingSlash != null && pathInRepoWithLeadingSlash.startsWith("/"))
                ? pathInRepoWithLeadingSlash.substring(1)
                : pathInRepoWithLeadingSlash;

        logger.info("解析GitHub URL: owner={}, repo={}, ref={}, pathInRepo={}", owner, repoName, ref, pathInRepo);

        try {
            GitHub github = new GitHubBuilder().build(); // 可配置为使用认证的GitHub实例
            GHRepository repository = github.getRepository(owner + "/" + repoName);

            if (repository == null) {
                throw new BusinessException("GitHub仓库不存在: " + owner + "/" + repoName);
            }
            if (repository.isPrivate()) {
                throw new BusinessException("GitHub仓库必须是公开的: " + owner + "/" + repoName);
            }

            // 如果指定了ref，校验ref是否存在 (注意：API可能没有直接校验ref的方法，通常下载时会失败)
            // 此处简单验证，更可靠的验证是在下载时进行
            // 如果指定了pathInRepo，可以尝试获取其内容来验证
            if (pathInRepo != null && !pathInRepo.isEmpty()) {
                String effectiveRef = (ref != null && !ref.isEmpty()) ? ref : repository.getDefaultBranch();
                try {
                    repository.getFileContent(pathInRepo, effectiveRef);
                    logger.info("路径 {} 在 ref {} 中验证成功", pathInRepo, effectiveRef);
                } catch (IOException e) {
                    logger.warn("无法验证路径 {} 在 ref {} (owner={}, repo={}): {}", pathInRepo, effectiveRef, owner, repoName,
                            e.getMessage());
                    throw new BusinessException(
                            "GitHub仓库中指定的路径 '" + pathInRepo + "' 在 ref '" + effectiveRef + "' 中不存在或无法访问。");
                }
            }
            logger.info("GitHub URL验证成功: {}", githubUrl);
            return new GitHubRepoInfo(owner, repoName, ref, pathInRepo);

        } catch (IOException e) {
            logger.error("GitHub API验证失败 for URL: " + githubUrl, e);
            throw new BusinessException("验证GitHub URL时发生API错误: " + e.getMessage());
        }
    }

    @Override
    public ToolStatus getNextStatus() {
        return ToolStatus.DEPLOYING; // 假设部署步骤依然存在，或者直接到MANUAL_REVIEW后的发布
    }
}