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

public class GithubUrlValidateProcessor implements ToolStateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(GithubUrlValidateProcessor.class);
    private static final Pattern GITHUB_URL_PATTERN = Pattern
            .compile("^https://github\\.com/([\\w.-]+)/([\\w.-]+)(?:/(tree|blob)/([\\w.-]+)(/(.*))?)?$");

    @Override
    public ToolStatus getStatus() {
        return ToolStatus.GITHUB_URL_VALIDATE;
    }

    @Override
    public void process(ToolEntity tool) {
        String uploadUrl = tool.getUploadUrl();
        // 这一步只做源地址合法性检查，不下载源码。
        // 目标是尽早确认仓库、ref、子路径都可访问。
        parseAndValidateGithubUrl(uploadUrl);
    }

    public static GitHubRepoInfo parseAndValidateGithubUrl(String githubUrl) {
        if (githubUrl == null || githubUrl.trim().isEmpty()) {
            throw new BusinessException("GitHub URL 不能为空");
        }

        // 支持仓库根地址，也支持 tree/blob 形式带 ref 和子路径的地址
        Matcher matcher = GITHUB_URL_PATTERN.matcher(githubUrl);
        if (!matcher.matches()) {
            throw new BusinessException("无效的 GitHub URL 格式: " + githubUrl);
        }

        String owner = matcher.group(1);
        String repoName = matcher.group(2);
        String ref = matcher.group(4);
        String pathInRepoWithLeadingSlash = matcher.group(5);
        String pathInRepo = (pathInRepoWithLeadingSlash != null && pathInRepoWithLeadingSlash.startsWith("/"))
                ? pathInRepoWithLeadingSlash.substring(1)
                : pathInRepoWithLeadingSlash;

        logger.info("解析 GitHub URL: owner={}, repo={}, ref={}, pathInRepo={}", owner, repoName, ref, pathInRepo);

        try {
            GitHub github = new GitHubBuilder().build();
            GHRepository repository = github.getRepository(owner + "/" + repoName);

            if (repository == null) {
                throw new BusinessException("GitHub 仓库不存在: " + owner + "/" + repoName);
            }
            if (repository.isPrivate()) {
                throw new BusinessException("GitHub 仓库必须是公开的: " + owner + "/" + repoName);
            }

            if (pathInRepo != null && !pathInRepo.isEmpty()) {
                String effectiveRef = (ref != null && !ref.isEmpty()) ? ref : repository.getDefaultBranch();
                try {
                    repository.getFileContent(pathInRepo, effectiveRef);
                } catch (IOException e) {
                    throw new BusinessException(
                            "GitHub 仓库中指定的路径 '" + pathInRepo + "' 在 ref '" + effectiveRef + "' 中不存在或无法访问");
                }
            }
            return new GitHubRepoInfo(owner, repoName, ref, pathInRepo);

        } catch (IOException e) {
            throw new BusinessException("验证 GitHub URL 时发生 API 错误: " + e.getMessage());
        }
    }

    @Override
    public ToolStatus getNextStatus() {
        return ToolStatus.DEPLOYING;
    }
}
