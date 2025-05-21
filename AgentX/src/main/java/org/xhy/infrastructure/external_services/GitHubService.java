package org.xhy.infrastructure.external_services;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.tool.service.state.impl.GithubUrlValidateProcessor;
import org.xhy.domain.tool.model.dto.GitHubRepoInfo;
import org.xhy.infrastructure.config.GitHubProperties;
import org.xhy.infrastructure.exception.BusinessException;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** 与 GitHub API 交互的服务。 负责从源GitHub仓库下载内容，以及将内容推送到目标GitHub仓库。 */
@Service
public class GitHubService {
    private static final Logger logger = LoggerFactory.getLogger(GitHubService.class);

    private final GitHubProperties gitHubProperties;
    private final GitHub github;

    /** 构造函数，初始化GitHub API客户端。
     * 
     * @param gitHubProperties GitHub配置属性
     * @throws IOException 如果GitHub客户端初始化失败 */
    public GitHubService(GitHubProperties gitHubProperties) throws IOException {
        this.gitHubProperties = gitHubProperties;
        // 初始化 GitHub API 客户端 (可配置匿名访问或使用PAT以提高速率限制)
        // 对于读取公共仓库信息，匿名访问通常足够
        // 对于需要认证的操作（如访问私有仓库或执行写操作），需要配置认证
        this.github = new GitHubBuilder().build();
    }

    /** 初始化时验证配置 */
    @PostConstruct
    public void init() {
        if (gitHubProperties.getTarget().getUsername() == null
                || gitHubProperties.getTarget().getUsername().trim().isEmpty()) {
            logger.warn("目标GitHub仓库的用户名未配置 (github.target.username)");
        }

        if (gitHubProperties.getTarget().getRepoName() == null
                || gitHubProperties.getTarget().getRepoName().trim().isEmpty()) {
            logger.warn("目标GitHub仓库的名称未配置 (github.target.repo-name)");
        }

        if (gitHubProperties.getTarget().getToken() == null
                || gitHubProperties.getTarget().getToken().trim().isEmpty()) {
            logger.warn("目标GitHub仓库的访问令牌未配置 (github.target.token)");
        }

        logger.info("GitHub服务已初始化，目标仓库: {}/{}", gitHubProperties.getTarget().getUsername(),
                gitHubProperties.getTarget().getRepoName());
    }

    /** 解析源GitHub URL。如果URL中未指定ref (分支/标签/commit)， 则获取该仓库默认分支的最新commit SHA作为ref。
     * 
     * @param sourceGithubUrl 源GitHub仓库的URL
     * @return GitHubRepoInfo 包含解析后的仓库所有者、名称、ref和仓库内路径
     * @throws IOException 如果与GitHub API通信时发生错误
     * @throws org.xhy.infrastructure.exception.BusinessException 如果URL无效或仓库不可访问 */
    public GitHubRepoInfo resolveSourceRepoInfoWithLatestCommitIfNoRef(String sourceGithubUrl) throws IOException {
        // 复用 GithubUrlValidateProcessor 中的解析和基础验证逻辑
        // 注意：GithubUrlValidateProcessor.parseAndValidateGithubUrl 可能会抛出
        // BusinessException，这里声明throws IOException，具体调用处处理
        GitHubRepoInfo basicInfo = GithubUrlValidateProcessor.parseAndValidateGithubUrl(sourceGithubUrl);

        if (basicInfo.getRef() == null || basicInfo.getRef().trim().isEmpty()) {
            logger.info("源URL {} 未指定ref，将获取仓库 {}/{} 的默认分支最新commit SHA", sourceGithubUrl, basicInfo.getOwner(),
                    basicInfo.getRepoName());
            GHRepository repository = github.getRepository(basicInfo.getFullName());
            String defaultBranch = repository.getDefaultBranch();
            if (defaultBranch == null || defaultBranch.trim().isEmpty()) {
                throw new org.xhy.infrastructure.exception.BusinessException(
                        "无法获取仓库 " + basicInfo.getFullName() + " 的默认分支。");
            }
            String latestCommitSha = repository.getRef("heads/" + defaultBranch).getObject().getSha();
            logger.info("仓库 {}/{} 的默认分支 {} 最新commit SHA为: {}", basicInfo.getOwner(), basicInfo.getRepoName(),
                    defaultBranch, latestCommitSha);
            return new GitHubRepoInfo(basicInfo.getOwner(), basicInfo.getRepoName(), latestCommitSha,
                    basicInfo.getPathInRepo());
        }
        return basicInfo;
    }

    /** 下载指定GitHub仓库特定ref的内容为ZIP归档文件。
     * 
     * @param repoInfo 包含仓库所有者、名称和ref的GitHubRepoInfo对象
     * @return 下载的ZIP文件的本地临时路径
     * @throws IOException 如果下载或文件操作失败 */
    public Path downloadRepositoryArchive(GitHubRepoInfo repoInfo) throws IOException {
        logger.info("开始下载仓库归档: {}/{}, ref: {}", repoInfo.getOwner(), repoInfo.getRepoName(), repoInfo.getRef());

        // 构建直接下载ZIP的URL
        // GitHub提供了两种归档格式的下载URL：
        // 1. zipball: https://github.com/{owner}/{repo}/zipball/{ref}
        // 2. tarball: https://github.com/{owner}/{repo}/tarball/{ref}
        // 我们使用zipball，与之前的GHArchiveFormat.ZIPBALL等效
        String archiveUrlString = String.format("https://github.com/%s/%s/zipball/%s", repoInfo.getOwner(),
                repoInfo.getRepoName(), repoInfo.getRef());
        URL archiveUrl = new URL(archiveUrlString);

        Path tempZipFile = Files.createTempFile(
                "source-repo-" + repoInfo.getRepoName() + "-" + UUID.randomUUID().toString().substring(0, 8), ".zip");
        // 使用Apache Commons IO进行文件下载，包含超时设置
        FileUtils.copyURLToFile(archiveUrl, tempZipFile.toFile(), 30000, 60000); // 30秒连接超时, 60秒读取超时

        logger.info("源仓库归档已下载到: {}", tempZipFile);
        return tempZipFile;
    }

    /** 将指定目录的内容提交并推送到目标GitHub仓库的指定路径下。
     * 
     * @param sourceDirectoryPath 本地源文件目录的Path对象
     * @param targetPathInRepo 内容在目标仓库中的存放路径 (例如: "tools/MyTool-author/v1.0.0")
     * @param commitMessage Git提交信息
     * @throws IOException 如果本地文件操作或网络IO失败
     * @throws GitAPIException 如果Git操作失败 */
    public void commitAndPushToTargetRepo(Path sourceDirectoryPath, String targetPathInRepo, String commitMessage)
            throws IOException, GitAPIException {

        String targetUsername = gitHubProperties.getTarget().getUsername();
        String targetToken = gitHubProperties.getTarget().getToken();
        String targetRepoName = gitHubProperties.getTarget().getRepoName();

        if (targetUsername == null || targetUsername.trim().isEmpty()) {
            throw new BusinessException("目标GitHub仓库的用户名未配置 (github.target.username)");
        }
        if (targetToken == null || targetToken.trim().isEmpty()) {
            // 在实际生产中，应有更安全的处理方式，例如启动时检查配置
            throw new BusinessException("目标GitHub仓库的Token未配置或为空 (github.target.token)");
        }
        if (targetRepoName == null || targetRepoName.trim().isEmpty()) {
            throw new BusinessException("目标GitHub仓库的名称未配置 (github.target.repo-name)");
        }

        String targetRepoFullName = targetUsername + "/" + targetRepoName;
        String targetRemoteUrl = "https://github.com/" + targetRepoFullName + ".git";
        logger.info("准备提交到目标仓库: {}, 目标路径: {}, 操作用户: {}", targetRemoteUrl, targetPathInRepo, targetUsername);

        // 创建一个临时目录用于克隆目标仓库
        Path tempCloneDir = Files
                .createTempDirectory("target-repo-clone-" + UUID.randomUUID().toString().substring(0, 8));
        Git git = null;

        try {
            // 1. 克隆目标仓库
            logger.info("克隆目标仓库 {} 到临时目录 {}", targetRemoteUrl, tempCloneDir);
            git = Git.cloneRepository().setURI(targetRemoteUrl).setDirectory(tempCloneDir.toFile())
                    // 使用提供的Token进行认证。GitHub PAT可以作为密码使用。
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(targetUsername, targetToken))
                    .call();

            // 2. 准备目标仓库中的路径
            Path fullTargetPathInClone = tempCloneDir.resolve(targetPathInRepo);
            if (Files.exists(fullTargetPathInClone)) {
                logger.info("目标路径 {} 在克隆仓库中已存在，将被清理。", fullTargetPathInClone);
                FileUtils.deleteDirectory(fullTargetPathInClone.toFile()); // 删除目录及其内容
            }
            Files.createDirectories(fullTargetPathInClone); // 重新创建目录结构

            // 3. 复制源文件到克隆仓库中的目标路径
            logger.info("复制文件从源路径 {} 到克隆仓库的目标路径 {}", sourceDirectoryPath, fullTargetPathInClone);
            FileUtils.copyDirectory(sourceDirectoryPath.toFile(), fullTargetPathInClone.toFile());

            // 4. Git Add - 将目标路径下的所有变更添加到暂存区
            // addFilepattern 使用相对于仓库根的路径
            logger.info("执行 git add {} (相对于仓库根)", targetPathInRepo);
            git.add().addFilepattern(targetPathInRepo).call();

            // 5. Git Commit
            logger.info("执行 git commit -m \"{}\"", commitMessage);
            git.commit().setMessage(commitMessage).call();

            // 6. Git Push
            logger.info("执行 git push 到远程仓库 {}", targetRemoteUrl);
            PushCommand pushCommand = git.push();
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(targetUsername, targetToken));
            pushCommand.call();

            logger.info("成功提交并推送到目标GitHub仓库: {}", targetRepoFullName);

        } finally {
            // 清理Git对象和临时克隆目录
            if (git != null) {
                git.close();
            }
            if (Files.exists(tempCloneDir)) {
                try {
                    FileUtils.deleteDirectory(tempCloneDir.toFile());
                    logger.info("临时克隆目录 {} 已成功清理。", tempCloneDir);
                } catch (IOException e) {
                    logger.error("清理临时克隆目录 {} 失败: {}", tempCloneDir, e.getMessage());
                }
            }
        }
    }

    /** 提供给其他服务使用的重载方法，支持指定目标仓库名
     * 
     * @param sourceDirectoryPath 本地源文件目录的Path对象
     * @param targetRepoName 目标仓库的名称 (不包含所有者/用户名)
     * @param targetPathInRepo 内容在目标仓库中的存放路径 (例如: "tools/MyTool-author/v1.0.0")
     * @param commitMessage Git提交信息
     * @throws IOException 如果本地文件操作或网络IO失败
     * @throws GitAPIException 如果Git操作失败 */
    public void commitAndPushToTargetRepo(Path sourceDirectoryPath, String targetRepoName, String targetPathInRepo,
            String commitMessage) throws IOException, GitAPIException {

        // 这是为了兼容已有代码，实际上应该统一使用配置中的仓库名
        if (targetRepoName == null || targetRepoName.trim().isEmpty()) {
            targetRepoName = gitHubProperties.getTarget().getRepoName();
        }

        String targetUsername = gitHubProperties.getTarget().getUsername();
        String targetToken = gitHubProperties.getTarget().getToken();

        if (targetUsername == null || targetUsername.trim().isEmpty()) {
            throw new BusinessException("目标GitHub仓库的用户名未配置 (github.target.username)");
        }
        if (targetToken == null || targetToken.trim().isEmpty()) {
            // 在实际生产中，应有更安全的处理方式，例如启动时检查配置
            throw new BusinessException("目标GitHub仓库的Token未配置或为空 (github.target.token)");
        }

        String targetRepoFullName = targetUsername + "/" + targetRepoName;
        String targetRemoteUrl = "https://github.com/" + targetRepoFullName + ".git";
        logger.info("准备提交到目标仓库: {}, 目标路径: {}, 操作用户: {}", targetRemoteUrl, targetPathInRepo, targetUsername);

        // 创建一个临时目录用于克隆目标仓库
        Path tempCloneDir = Files
                .createTempDirectory("target-repo-clone-" + UUID.randomUUID().toString().substring(0, 8));
        Git git = null;

        try {
            // 1. 克隆目标仓库
            logger.info("克隆目标仓库 {} 到临时目录 {}", targetRemoteUrl, tempCloneDir);
            git = Git.cloneRepository().setURI(targetRemoteUrl).setDirectory(tempCloneDir.toFile())
                    // 使用提供的Token进行认证。GitHub PAT可以作为密码使用。
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(targetUsername, targetToken))
                    .call();

            // 2. 准备目标仓库中的路径
            Path fullTargetPathInClone = tempCloneDir.resolve(targetPathInRepo);
            if (Files.exists(fullTargetPathInClone)) {
                logger.info("目标路径 {} 在克隆仓库中已存在，将被清理。", fullTargetPathInClone);
                FileUtils.deleteDirectory(fullTargetPathInClone.toFile()); // 删除目录及其内容
            }
            Files.createDirectories(fullTargetPathInClone); // 重新创建目录结构

            // 3. 复制源文件到克隆仓库中的目标路径
            logger.info("复制文件从源路径 {} 到克隆仓库的目标路径 {}", sourceDirectoryPath, fullTargetPathInClone);
            FileUtils.copyDirectory(sourceDirectoryPath.toFile(), fullTargetPathInClone.toFile());

            // 4. Git Add - 将目标路径下的所有变更添加到暂存区
            // addFilepattern 使用相对于仓库根的路径
            logger.info("执行 git add {} (相对于仓库根)", targetPathInRepo);
            git.add().addFilepattern(targetPathInRepo).call();

            // 5. Git Commit
            logger.info("执行 git commit -m \"{}\"", commitMessage);
            git.commit().setMessage(commitMessage).call();

            // 6. Git Push
            logger.info("执行 git push 到远程仓库 {}", targetRemoteUrl);
            PushCommand pushCommand = git.push();
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(targetUsername, targetToken));
            pushCommand.call();

            logger.info("成功提交并推送到目标GitHub仓库: {}", targetRepoFullName);

        } finally {
            // 清理Git对象和临时克隆目录
            if (git != null) {
                git.close();
            }
            if (Files.exists(tempCloneDir)) {
                try {
                    FileUtils.deleteDirectory(tempCloneDir.toFile());
                    logger.info("临时克隆目录 {} 已成功清理。", tempCloneDir);
                } catch (IOException e) {
                    logger.error("清理临时克隆目录 {} 失败: {}", tempCloneDir, e.getMessage());
                }
            }
        }
    }
}