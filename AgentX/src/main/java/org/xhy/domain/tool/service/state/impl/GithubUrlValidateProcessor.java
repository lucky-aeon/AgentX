package org.xhy.domain.tool.service.state.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component; // 或 @Service
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.model.dto.GitHubRepoInfo;
import org.xhy.domain.tool.service.state.ToolStateProcessor;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.github.GitHubService; // 导入 GitHubService
import org.xhy.infrastructure.github.GitHubUrlParser;

import java.io.IOException;

/**
 * GitHub URL 验证处理器。
 * 负责验证上传的 GitHub URL 是否合法。
 * URL 格式解析已委托给 GitHubUrlParser。
 * API 验证部分已委托给 GitHubService。
 */
@Component // 或者 @Service，取决于你的 Spring 管理策略
public class GithubUrlValidateProcessor implements ToolStateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(GithubUrlValidateProcessor.class);

    private final GitHubService gitHubService; // 注入 GitHubService

    // 构造函数注入 GitHubService
    public GithubUrlValidateProcessor(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @Override
    public ToolStatus getStatus() {
        return ToolStatus.GITHUB_URL_VALIDATE;
    }

    @Override
    public void process(ToolEntity tool) {
        String uploadUrl = tool.getUploadUrl();
        // 使用 GitHubUrlParser 进行 URL 的格式解析，允许不带分支/Tag或指向子路径
        GitHubRepoInfo repoInfo = GitHubUrlParser.parseGithubUrl(uploadUrl);

        // 调用 GitHubService 进行 API 层面的仓库、引用（分支/Tag）和路径存在性验证
        try {
            gitHubService.validateGitHubRepoRefAndPath(repoInfo);
            logger.info("GitHub URL 验证成功：{}", uploadUrl);
        } catch (IOException e) {
            logger.error("通过 GitHubService 验证 URL 失败：{}", uploadUrl, e);
            throw new BusinessException("验证 GitHub URL 时发生 API 错误：" + e.getMessage());
        }
    }

    @Override
    public ToolStatus getNextStatus() {
        // 假设部署步骤依然存在，或者直接到 MANUAL_REVIEW 后的发布
        return ToolStatus.DEPLOYING;
    }
}