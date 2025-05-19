package org.xhy.domain.tool.service.state.impl;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.service.state.ToolStateProcessor;
import org.xhy.infrastructure.exception.BusinessException;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub URL验证处理器
 */
public class GithubUrlValidateProcessor implements ToolStateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(GithubUrlValidateProcessor.class);

    // GitHub URL正则表达式验证
    private static final Pattern GITHUB_URL_PATTERN = 
            Pattern.compile("^https://github\\.com/([\\w-]+)/([\\w-]+)(/(tree|blob)/([\\w-./]+))?$");
    
    @Override
    public ToolStatus getStatus() {
        return ToolStatus.GITHUB_URL_VALIDATE;
    }
    
    @Override
    public void process(ToolEntity tool) {
        String uploadUrl = tool.getUploadUrl();
        
        // 验证URL不为空
        if (uploadUrl == null || uploadUrl.trim().isEmpty()) {
            throw new BusinessException("GitHub URL不能为空");
        }
        
        // 验证URL格式并提取仓库信息
        Matcher matcher = GITHUB_URL_PATTERN.matcher(uploadUrl);
        if (!matcher.matches()) {
            throw new BusinessException("无效的GitHub URL格式: " + uploadUrl);
        }
        
        String owner = matcher.group(1);
        String repoName = matcher.group(2);
        String path = matcher.group(5); // 可能为null，如果URL只是仓库根URL

        logger.info("开始验证GitHub仓库: {}/{}, 路径: {}", owner, repoName, path);
        
        try {
            // 连接到GitHub API
            GitHub github = new GitHubBuilder().build();
            
            // 获取仓库信息
            GHRepository repository = github.getRepository(owner + "/" + repoName);
            
            // 验证仓库是否存在且可访问
            if (repository == null) {
                throw new BusinessException("GitHub仓库不存在: " + owner + "/" + repoName);
            }
            
            // 检查仓库是否是公开的
            if (repository.isPrivate()) {
                throw new BusinessException("GitHub仓库必须是公开的，不能是私有仓库: " + owner + "/" + repoName);
            }
            
            logger.info("GitHub仓库验证成功: {}/{}", owner, repoName);
        } catch (IOException e) {
            logger.error("GitHub API验证失败", e);
            throw new BusinessException("验证GitHub URL失败: " + e.getMessage());
        }
    }
    
    @Override
    public ToolStatus getNextStatus() {
        return ToolStatus.DEPLOYING;
    }
} 