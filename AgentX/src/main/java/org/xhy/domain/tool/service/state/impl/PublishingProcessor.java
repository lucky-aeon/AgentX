package org.xhy.domain.tool.service.state.impl;

import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.model.dto.GitHubRepoInfo;
import org.xhy.domain.tool.service.state.ToolStateProcessor;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.github.GitHubService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
public class PublishingProcessor implements ToolStateProcessor {
    private static final Logger logger = LoggerFactory.getLogger(PublishingProcessor.class);
    private final GitHubService gitHubService;

    public PublishingProcessor(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @Override
    public ToolStatus getStatus() {
        return ToolStatus.PUBLISHING;
    }

    @Override
    public void process(ToolEntity tool) {
        Path tempDownloadPath = null;
        Path tempUnzipPath = null;

        try {
            String sourceGitHubUrl = tool.getUploadUrl();
            if (sourceGitHubUrl == null || sourceGitHubUrl.trim().isEmpty()) {
                throw new BusinessException("工具 " + tool.getName() + " 的源 GitHub URL 为空，无法发布");
            }

            // 这里做的是“平台侧正式发布”：
            // 把源仓库某个确定版本的内容同步进平台目标仓库，形成受控产物
            GitHubRepoInfo sourceRepoInfo = gitHubService.resolveSourceRepoInfoWithLatestCommitIfNoRef(sourceGitHubUrl);
            String version = sourceRepoInfo.getRef();
            if (version == null || version.trim().isEmpty()) {
                throw new BusinessException("无法确定源 GitHub 仓库的版本号(ref/commit SHA)用于发布");
            }
            String sanitizedVersion = version.replaceAll("[^a-zA-Z0-9_.-]", "_");

            tempDownloadPath = gitHubService.downloadRepositoryArchive(sourceRepoInfo);
            tempUnzipPath = Files.createTempDirectory("unzip-" + UUID.randomUUID().toString().substring(0, 8));
            try (ZipFile zipFile = new ZipFile(tempDownloadPath.toFile())) {
                zipFile.extractAll(tempUnzipPath.toString());
            }

            Path actualContentRoot = findActualContentRoot(tempUnzipPath, sourceRepoInfo.getRepoName());
            if (actualContentRoot == null) {
                throw new BusinessException("无法在解压后的归档中找到实际内容根目录");
            }

            Path sourcePathToPublish = actualContentRoot;
            if (sourceRepoInfo.getPathInRepo() != null && !sourceRepoInfo.getPathInRepo().isEmpty()) {
                sourcePathToPublish = actualContentRoot.resolve(sourceRepoInfo.getPathInRepo());
                if (!Files.exists(sourcePathToPublish) || !Files.isDirectory(sourcePathToPublish)) {
                    throw new BusinessException(
                            "源 URL 中指定的路径 '" + sourceRepoInfo.getPathInRepo() + "' 在下载内容中不存在或不是目录");
                }
            }

            String toolIdentifierInTarget = tool.getName() + "-" + sourceRepoInfo.getOwner();
            String targetPathInInternalRepo = toolIdentifierInTarget + "/" + sanitizedVersion;
            String commitMessage = String.format("Publish tool: %s, Version: %s (Source: %s@%s)", tool.getName(),
                    version, sourceRepoInfo.getFullName(), sourceRepoInfo.getRef());
            gitHubService.commitAndPushToTargetRepo(sourcePathToPublish, targetPathInInternalRepo, commitMessage);

        } catch (BusinessException | IOException | GitAPIException e) {
            throw new BusinessException("发布工具到目标仓库时失败: " + e.getMessage(), e);
        } finally {
            cleanupTemporaryFiles(tempDownloadPath, tempUnzipPath);
        }
    }

    @Override
    public ToolStatus getNextStatus() {
        return ToolStatus.APPROVED;
    }

    private void cleanupTemporaryFiles(Path tempDownloadPath, Path tempUnzipPath) {
        try {
            if (tempDownloadPath != null && Files.exists(tempDownloadPath)) {
                Files.delete(tempDownloadPath);
            }
            if (tempUnzipPath != null && Files.exists(tempUnzipPath)) {
                FileUtils.deleteDirectory(tempUnzipPath.toFile());
            }
        } catch (IOException e) {
            logger.warn("清理发布过程中的临时文件失败: {}", e.getMessage());
        }
    }

    private Path findActualContentRoot(Path unzipDir, String repoNameHint) throws IOException {
        List<Path> subDirs;
        try (var stream = Files.list(unzipDir)) {
            subDirs = stream.filter(Files::isDirectory).toList();
        }

        if (subDirs.size() == 1) {
            return subDirs.get(0);
        }

        if (repoNameHint != null && !repoNameHint.isEmpty()) {
            for (Path subDir : subDirs) {
                if (subDir.getFileName().toString().toLowerCase().contains(repoNameHint.toLowerCase())) {
                    return subDir;
                }
            }
        }

        return unzipDir;
    }
}
