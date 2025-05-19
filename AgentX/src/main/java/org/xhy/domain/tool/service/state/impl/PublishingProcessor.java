package org.xhy.domain.tool.service.state.impl;

import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value; // Spring的Value注解，如果不用Spring则需移除或替换
import org.springframework.stereotype.Service;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.model.dto.GitHubRepoInfo;
import org.xhy.domain.tool.service.state.ToolStateProcessor;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.external_services.GitHubService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List; // 确保导入
import java.util.UUID;

/**
 * "发布中"状态处理器。
 * 负责从源GitHub下载工具内容，并将其发布到目标GitHub仓库。
 */
@Service
public class PublishingProcessor implements ToolStateProcessor {
    private static final Logger logger = LoggerFactory.getLogger(PublishingProcessor.class);

    private final GitHubService gitHubService;

    /**
     * 构造函数。
     * 
     * @param gitHubService GitHub服务实例，用于与GitHub API交互。
     */
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

        logger.info("工具ID: {} 进入PUBLISHING状态，开始发布流程。", tool.getId());

        try {
            String sourceGitHubUrl = tool.getUploadUrl();
            if (sourceGitHubUrl == null || sourceGitHubUrl.trim().isEmpty()) {
                throw new BusinessException("工具 " + tool.getName() + " 的源GitHub URL为空，无法发布。");
            }

            // 1. 解析源仓库信息，如果URL没有指定版本，则获取最新commit SHA作为版本
            GitHubRepoInfo sourceRepoInfo = gitHubService.resolveSourceRepoInfoWithLatestCommitIfNoRef(sourceGitHubUrl);
            String version = sourceRepoInfo.getRef();
            if (version == null || version.trim().isEmpty()) {
                throw new BusinessException("无法确定源GitHub仓库的版本号(ref/commit SHA)用于发布。");
            }
            // 清理版本名中的非法字符，确保可以用作目录名
            String sanitizedVersion = version.replaceAll("[^a-zA-Z0-9_.-]", "_");
            logger.info("将使用源版本 '{}' (清理后为 '{}') 进行发布。", version, sanitizedVersion);

            // 2. 下载源仓库归档
            tempDownloadPath = gitHubService.downloadRepositoryArchive(sourceRepoInfo);

            // 3. 创建临时解压目录
            tempUnzipPath = Files.createTempDirectory("unzip-" + UUID.randomUUID().toString().substring(0, 8));
            logger.info("源仓库内容将解压到临时目录: {}", tempUnzipPath.toString());

            // 4. 解压归档
            try (ZipFile zipFile = new ZipFile(tempDownloadPath.toFile())) {
                zipFile.extractAll(tempUnzipPath.toString());
            }
            logger.info("源仓库归档文件解压完成。");

            Path actualContentRoot = findActualContentRoot(tempUnzipPath, sourceRepoInfo.getRepoName());
            if (actualContentRoot == null) {
                throw new BusinessException("无法在解压的归档中找到实际内容根目录。");
            }
            logger.info("找到实际内容根目录: {}", actualContentRoot);

            Path sourcePathToPublish = actualContentRoot;
            if (sourceRepoInfo.getPathInRepo() != null && !sourceRepoInfo.getPathInRepo().isEmpty()) {
                sourcePathToPublish = actualContentRoot.resolve(sourceRepoInfo.getPathInRepo());
                if (!Files.exists(sourcePathToPublish) || !Files.isDirectory(sourcePathToPublish)) {
                    throw new BusinessException("源URL中指定的路径 '" + sourceRepoInfo.getPathInRepo() +
                            "' 在下载的内容中不存在或不是一个目录。");
                }
                logger.info("将从指定子路径发布: {}", sourcePathToPublish);
            }

            // 5. 定义目标仓库中的路径结构
            // 目标仓库根目录下 -> {工具名}-{源仓库作者名} -> {版本号} -> {工具内容}
            String toolIdentifierInTarget = tool.getName() + "-" + sourceRepoInfo.getOwner();
            String targetPathInInternalRepo = toolIdentifierInTarget + "/" + sanitizedVersion;
            // logger.info("内容将发布到目标仓库 '{}' 的路径 '{}' 下", this.targetInternalRepoName,
            // targetPathInInternalRepo); // Log updated
            logger.info("内容将发布到目标仓库的路径 '{}' 下", targetPathInInternalRepo);

            // 6. 提交并推送到目标GitHub仓库
            String commitMessage = String.format("Publish tool: %s, Version: %s (Source: %s@%s)",
                    tool.getName(), version, sourceRepoInfo.getFullName(), sourceRepoInfo.getRef());
            // Use the overload that doesn't require targetRepoName, let GitHubService use
            // its config
            gitHubService.commitAndPushToTargetRepo(sourcePathToPublish, targetPathInInternalRepo, commitMessage);

            // 7. 如果发布成功，ToolStateService会将状态设置为PUBLISHED
            // logger.info("工具 {} 版本 {} 成功发布到内部仓库 {} 的路径 {} 下",
            // tool.getName(), version, this.targetInternalRepoName,
            // targetPathInInternalRepo); // Log updated
            logger.info("工具 {} 版本 {} 成功发布到目标仓库的路径 {} 下",
                    tool.getName(), version, targetPathInInternalRepo);
            // 此处理器不直接修改状态，由ToolStateService在调用此process后根据是否抛异常来决定下一个状态

        } catch (BusinessException | IOException | GitAPIException e) { // 更具体地捕获已知异常
            logger.error("发布工具 {} (ID: {}) 失败: {}", tool.getName(), tool.getId(), e.getMessage(), e);
            // 抛出异常，ToolStateService会捕获并设置状态为PUBLISH_FAILED
            throw new BusinessException("发布工具到目标仓库时失败: " + e.getMessage(), e);
        } finally {
            // 清理临时文件和目录
            cleanupTemporaryFiles(tempDownloadPath, tempUnzipPath);
        }
    }

    @Override
    public ToolStatus getNextStatus() {
        // 这个方法在当前设计中可能不会被 ToolStateService 直接使用来决定下一个状态，
        // 因为成功/失败是在 process 方法内部通过是否抛出异常来隐式确定的。
        // ToolStateService 会在调用 process 后，如果无异常，则设置为 PUBLISHED，
        // 如果捕获到异常，则设置为 PUBLISH_FAILED。
        // 为符合接口，可以返回一个理论上的成功状态，但实际控制权在ToolStateService。
        return ToolStatus.APPROVED;
    }

    /**
     * 清理临时下载和解压的文件/目录。
     */
    private void cleanupTemporaryFiles(Path tempDownloadPath, Path tempUnzipPath) {
        try {
            if (tempDownloadPath != null && Files.exists(tempDownloadPath)) {
                Files.delete(tempDownloadPath);
                logger.info("已删除临时下载文件: {}", tempDownloadPath);
            }
            if (tempUnzipPath != null && Files.exists(tempUnzipPath)) {
                FileUtils.deleteDirectory(tempUnzipPath.toFile());
                logger.info("已删除临时解压目录: {}", tempUnzipPath);
            }
        } catch (IOException e) {
            logger.warn("清理发布过程中的临时文件失败: {}", e.getMessage());
        }
    }

    /**
     * 查找解压后ZIP文件的实际内容根目录。
     * GitHub下载的ZIP通常会有一个顶层目录，例如 repo-name-commitsha/ 或 repo-name-tag/
     * 
     * @param unzipDir     解压操作的根目录
     * @param repoNameHint 源仓库的名称，用于辅助查找
     * @return 实际内容所在的Path对象，如果无法确定则返回unzipDir本身
     * @throws IOException 如果列出目录内容时发生IO错误
     */
    private Path findActualContentRoot(Path unzipDir, String repoNameHint) throws IOException {
        List<Path> subDirs;
        try (var stream = Files.list(unzipDir)) {
            subDirs = stream.filter(Files::isDirectory).toList();
        }

        if (subDirs.size() == 1) {
            // 如果解压后只有一个子目录，通常这就是内容根目录
            logger.debug("找到唯一子目录作为内容根: {}", subDirs.get(0));
            return subDirs.get(0);
        }

        // 如果有多个子目录，尝试基于仓库名提示进行匹配
        if (repoNameHint != null && !repoNameHint.isEmpty()) {
            for (Path subDir : subDirs) {
                if (subDir.getFileName().toString().toLowerCase().contains(repoNameHint.toLowerCase())) {
                    logger.debug("通过仓库名提示找到内容根: {}", subDir);
                    return subDir;
                }
            }
        }

        // 如果无法通过上述方式确定，记录警告并返回解压目录本身
        logger.warn("无法精确找到解压后的实际内容根目录于 {}，将使用该目录作为根。请检查ZIP结构。", unzipDir);
        return unzipDir;
    }
}