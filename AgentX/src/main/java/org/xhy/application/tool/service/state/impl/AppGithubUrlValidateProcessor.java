package org.xhy.application.tool.service.state.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.application.tool.service.state.AppToolStateProcessor;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.model.dto.GitHubRepoInfo;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.github.GitHubService;
import org.xhy.infrastructure.github.GitHubUrlParser;

import java.io.IOException;

/** 应用层GitHub URL验证处理器
 * 
 * 职责： 1. 验证上传的GitHub URL是否合法 2. 调用基础设施层GitHubService进行API验证 3. 转换到下一个状态（部署） */
public class AppGithubUrlValidateProcessor implements AppToolStateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AppGithubUrlValidateProcessor.class);

    private final GitHubService gitHubService;

    /** 构造函数，注入GitHubService
     * 
     * @param gitHubService GitHub服务 */
    public AppGithubUrlValidateProcessor(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @Override
    public ToolStatus getStatus() {
        return ToolStatus.GITHUB_URL_VALIDATE;
    }

    @Override
    public void process(ToolEntity tool) {
        String uploadUrl = tool.getUploadUrl();
        logger.info("开始验证GitHub URL: {} (工具ID: {})", uploadUrl, tool.getId());

        try {
            logger.debug("解析GitHub URL: {}", uploadUrl);
            GitHubRepoInfo repoInfo = GitHubUrlParser.parseGithubUrl(uploadUrl);
            logger.debug("解析结果 - 仓库: {}, 引用: {}, 路径: {}", repoInfo.getFullName(), repoInfo.getRef(),
                    repoInfo.getPathInRepo());

            logger.debug("开始调用GitHub API验证");
            gitHubService.validateGitHubRepoRefAndPath(repoInfo);
            logger.debug("GitHub API验证完成");

            logger.info("GitHub URL 验证成功：{} (工具ID: {})", uploadUrl, tool.getId());

        } catch (IOException e) {
            logger.error("通过 GitHubService 验证 URL 失败：{} (工具ID: {})", uploadUrl, tool.getId(), e);
            throw new BusinessException("验证 GitHub URL 时发生 API 错误：" + e.getMessage(), e);
        } catch (BusinessException e) {
            logger.error("GitHub URL 验证失败：{} (工具ID: {}), 错误：{}", uploadUrl, tool.getId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("验证 GitHub URL 时发生意外错误：{} (工具ID: {})", uploadUrl, tool.getId(), e);
            throw new BusinessException("验证 GitHub URL 时发生意外错误：" + e.getMessage(), e);
        }
    }

    @Override
    public ToolStatus getNextStatus() {
        return ToolStatus.DEPLOYING;
    }
}