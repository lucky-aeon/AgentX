package org.xhy.infrastructure.github;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.tool.model.dto.GitHubRepoInfo;
import org.xhy.infrastructure.config.GitHubProperties;
import org.xhy.infrastructure.exception.BusinessException;

import jakarta.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** 与 GitHub API 交互的服务。 负责从源GitHub仓库下载内容，验证仓库信息，以及将内容推送到目标GitHub仓库。 */
@Service
public class GitHubService {
    private static final Logger logger = LoggerFactory.getLogger(GitHubService.class);

    private final GitHubProperties gitHubProperties;
    private final GitHub github;

    public GitHubService(GitHubProperties gitHubProperties) throws IOException {
        this.gitHubProperties = gitHubProperties;
        // 配置超时时间和连接设置
        GitHubBuilder builder = new GitHubBuilder();

        // 如果配置了访问令牌，使用认证访问，否则使用匿名访问
        if (gitHubProperties.getTarget().getToken() != null && 
            !gitHubProperties.getTarget().getToken().trim().isEmpty()) {
            logger.info("使用GitHub访问令牌进行认证访问");
            this.github = builder.withOAuthToken(gitHubProperties.getTarget().getToken()).build();
        } else {
            logger.warn("未配置GitHub访问令牌，使用匿名访问（可能受速率限制影响）");
            this.github = builder.build();
        }
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

    /** 验证 GitHub 仓库是否存在、是否公开，并验证指定的引用（分支/Tag）和路径是否有效。 如果 repoInfo 中的 ref 为空，则默认使用仓库的默认分支进行验证。
     *
     * @param repoInfo 包含仓库所有者、名称、引用（分支/Tag）和仓库内路径的 GitHubRepoInfo 对象。
     * @throws BusinessException 如果仓库不存在、不公开、指定的引用无效或路径不存在。
     * @throws IOException 如果与 GitHub API 通信时发生错误。 */
    public void validateGitHubRepoRefAndPath(GitHubRepoInfo repoInfo) throws IOException {
        String owner = repoInfo.getOwner();
        String repoName = repoInfo.getRepoName();
        String ref = repoInfo.getRef();
        String pathInRepo = repoInfo.getPathInRepo();

        logger.info("开始通过 GitHub API 验证仓库：{}，引用：{}，路径：{}", repoInfo.getFullName(), ref, pathInRepo);

        GHRepository repository;
        try {
            logger.debug("正在获取GitHub仓库信息：{}", repoInfo.getFullName());
            repository = github.getRepository(repoInfo.getFullName());
            logger.debug("成功获取GitHub仓库信息：{}", repoInfo.getFullName());
        } catch (IOException e) {
            logger.error("获取GitHub仓库信息失败：{}，错误：{}", repoInfo.getFullName(), e.getMessage());
            if (e.getMessage().contains("404") || e.getMessage().contains("Not Found")) {
                throw new BusinessException("GitHub 仓库不存在或无权访问：" + repoInfo.getFullName());
            } else if (e.getMessage().contains("403") || e.getMessage().contains("rate limit")) {
                throw new BusinessException("GitHub API 访问受限，请检查网络连接或配置访问令牌：" + e.getMessage());
            } else if (e.getMessage().contains("timeout")) {
                throw new BusinessException("GitHub API 请求超时，请检查网络连接：" + e.getMessage());
            }
            throw new BusinessException("访问 GitHub API 失败：" + e.getMessage(), e);
        }

        if (repository == null) {
            throw new BusinessException("GitHub 仓库不存在：" + repoInfo.getFullName());
        }
        if (repository.isPrivate()) {
            throw new BusinessException("GitHub 仓库必须是公开的：" + repoInfo.getFullName());
        }

        String effectiveRef = null;

        // 决定使用哪个 ref (分支/Tag) 进行验证
        if (ref == null || ref.trim().isEmpty()) {
            // 如果 URL 中未指定 ref，则使用仓库的默认分支
            effectiveRef = repository.getDefaultBranch();
            if (effectiveRef == null || effectiveRef.trim().isEmpty()) {
                throw new BusinessException("无法获取仓库 " + repoInfo.getFullName() + " 的默认分支。");
            }
            logger.info("URL 未指定引用，默认使用仓库的默认分支：'{}'", effectiveRef);
        } else {
            // 如果 URL 中指定了 ref，则验证它是一个有效的分支或 Tag
            boolean refFound = false;
            String potentialBranchRef = "heads/" + ref; // 分支的引用形式 (如 "heads/main")
            String potentialTagRef = "tags/" + ref; // Tag 的引用形式 (如 "tags/v1.0.0")

            // 1. 尝试作为分支验证 (直接使用 ref 名称)
            try {
                // getRef 接受 "heads/branchName" 形式
                GHRef branchRef = repository.getRef(potentialBranchRef);
                if (branchRef != null) {
                    effectiveRef = ref; // 实际使用时还是用原始的 ref 名称
                    refFound = true;
                    logger.info("引用 '{}' 已验证为有效的分支。", ref);
                }
            } catch (IOException e) {
                if (e instanceof FileNotFoundException) {
                    logger.debug("引用 '{}' 不是有效分支 ({})，尝试作为 Tag 验证。", ref, e.getMessage());
                } else {
                    throw e; // 抛出其他非404的IOException
                }
            }

            // 2. 如果不是分支，尝试作为 Tag 验证 (直接使用 ref 名称)
            if (!refFound) {
                try {
                    // getRef 接受 "tags/tagName" 形式
                    GHRef tagRef = repository.getRef(potentialTagRef);
                    if (tagRef != null) {
                        effectiveRef = ref; // 实际使用时还是用原始的 ref 名称
                        refFound = true;
                        logger.info("引用 '{}' 已验证为有效的 Tag。", ref);
                    }
                } catch (IOException e) {
                    if (e instanceof FileNotFoundException) {
                        logger.debug("引用 '{}' 也不是有效的 Tag ({})。", ref, e.getMessage());
                    } else {
                        throw e; // 抛出其他非404的IOException
                    }
                }
            }

            if (!refFound) {
                throw new BusinessException(
                        "指定的引用 '" + ref + "' 在 GitHub 仓库 '" + repoInfo.getFullName() + "' 中不是一个有效的分支也不是一个有效的 Tag。");
            }
        }

        // 验证仓库内的路径是否存在于 effectiveRef 下
        if (pathInRepo != null && !pathInRepo.isEmpty()) {
            // 构造实际用于 API 调用的 ref 字符串，优先尝试 Tag
            String apiRefForContent = effectiveRef;
            try {
                // 再次尝试获取 "tags/" 形式的引用，以确定它是 Tag 还是分支
                // 如果能成功获取到，说明 effectiveRef 对应的是一个 Tag
                repository.getRef("tags/" + effectiveRef);
                apiRefForContent = "tags/" + effectiveRef; // 确认为 Tag，使用带 tags/ 前缀的 ref
            } catch (IOException e) {
                // 如果不是 Tag，则保持 original effectiveRef 作为分支名 (e.g., "main")
            }

            boolean pathExists = false;
            try {
                // 1. 尝试作为目录获取内容
                List<GHContent> contents = repository.getDirectoryContent(pathInRepo, apiRefForContent);
                // 只要不抛出异常，就说明路径存在且是目录
                pathExists = true;
                logger.info("路径 '{}' 在引用 '{}' 中验证成功，它是一个目录。", pathInRepo, effectiveRef);
            } catch (IOException e) {
                // 如果是 FileNotFoundException，则继续尝试作为文件
                if (!(e instanceof FileNotFoundException)) {
                    throw e; // 抛出其他非404的IOException
                }
                logger.debug("路径 '{}' 在引用 '{}' 中不是目录，尝试作为文件验证。", pathInRepo, effectiveRef);
            }

            if (!pathExists) {
                try {
                    // 2. 尝试作为文件获取内容
                    GHContent fileContent = repository.getFileContent(pathInRepo, apiRefForContent);
                    // 只要不抛出异常且文件内容不为 null，就说明路径是文件且存在
                    if (fileContent != null) {
                        pathExists = true;
                        logger.info("路径 '{}' 在引用 '{}' 中验证成功，它是一个文件。", pathInRepo, effectiveRef);
                    }
                } catch (IOException e) {
                    // 如果是 FileNotFoundException，说明文件也不存在
                    if (!(e instanceof FileNotFoundException)) {
                        throw e; // 抛出其他非404的IOException
                    }
                    logger.debug("路径 '{}' 在引用 '{}' 中也不是文件。", pathInRepo, effectiveRef);
                }
            }

            if (!pathExists) {
                throw new BusinessException(
                        "指定路径 '" + pathInRepo + "' 在 GitHub 仓库的引用 '" + effectiveRef + "' 中不存在或无法访问。");
            }
        }
        logger.info("GitHub 仓库、引用和路径验证通过：{}", repoInfo.getFullName());
    }

    /** 解析源GitHub URL。如果URL中未指定ref (分支/标签/commit)， 则获取该仓库默认分支的最新commit SHA作为ref。
     * <p>
     * **注意：** 此方法将使用灵活的 URL 解析器 (`GitHubUrlParser.parseGithubUrl`)， 允许不带 Tag 或分支的 URL。
     *
     * @param sourceGithubUrl 源GitHub仓库的URL
     * @return GitHubRepoInfo 包含解析后的仓库所有者、名称、ref和仓库内路径
     * @throws IOException 如果与GitHub API通信时发生错误
     * @throws BusinessException 如果URL无效或仓库不可访问 */
    public GitHubRepoInfo resolveSourceRepoInfoWithLatestCommitIfNoRef(String sourceGithubUrl) throws IOException {
        GitHubRepoInfo basicInfo = GitHubUrlParser.parseGithubUrl(sourceGithubUrl);

        if (basicInfo.getRef() == null || basicInfo.getRef().trim().isEmpty()) {
            logger.info("源URL {} 未指定ref，将获取仓库 {}/{} 的默认分支最新commit SHA", sourceGithubUrl, basicInfo.getOwner(),
                    basicInfo.getRepoName());
            GHRepository repository = github.getRepository(basicInfo.getFullName());
            String defaultBranch = repository.getDefaultBranch();
            if (defaultBranch == null || defaultBranch.trim().isEmpty()) {
                throw new BusinessException("无法获取仓库 " + basicInfo.getFullName() + " 的默认分支。");
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

        String archiveUrlString = String.format("https://github.com/%s/%s/zipball/%s", repoInfo.getOwner(),
                repoInfo.getRepoName(), repoInfo.getRef());
        URL archiveUrl = new URL(archiveUrlString);

        Path tempZipFile = Files.createTempFile(
                "source-repo-" + repoInfo.getRepoName() + "-" + UUID.randomUUID().toString().substring(0, 8), ".zip");
        FileUtils.copyURLToFile(archiveUrl, tempZipFile.toFile(), 30000, 60000);

        logger.info("源仓库归档已下载到: {}", tempZipFile);
        return tempZipFile;
    }

    /** 将指定目录的内容提交并推送到目标GitHub仓库的指定路径下。
     *
     * @param sourceDirectoryPath 本地源文件目录的Path对象
     * @param targetPathInRepo 内容在目标仓库中的存放路径 (例如: "tools/MyTool-author/v1.0.0")
     * @param commitMessage Git提交信息
     * @throws IOException 如果本地文件操作或网络IO失败
     * @throws GitAPIException 如果Git操作失败
     * @throws BusinessException 如果目标仓库配置不完整 */
    public void commitAndPushToTargetRepo(Path sourceDirectoryPath, String targetPathInRepo, String commitMessage)
            throws IOException, GitAPIException {

        String targetUsername = gitHubProperties.getTarget().getUsername();
        String targetToken = gitHubProperties.getTarget().getToken();
        String targetRepoName = gitHubProperties.getTarget().getRepoName();

        if (targetUsername == null || targetUsername.trim().isEmpty()) {
            throw new BusinessException("目标GitHub仓库的用户名未配置 (github.target.username)");
        }
        if (targetToken == null || targetToken.trim().isEmpty()) {
            throw new BusinessException("目标GitHub仓库的Token未配置或为空 (github.target.token)");
        }
        if (targetRepoName == null || targetRepoName.trim().isEmpty()) {
            throw new BusinessException("目标GitHub仓库的名称未配置 (github.target.repo-name)");
        }

        String targetRepoFullName = targetUsername + "/" + targetRepoName;
        String targetRemoteUrl = "https://github.com/" + targetRepoFullName + ".git";
        logger.info("准备提交到目标仓库: {}，目标路径: {}，操作用户: {}", targetRemoteUrl, targetPathInRepo, targetUsername);

        Path tempCloneDir = Files
                .createTempDirectory("target-repo-clone-" + UUID.randomUUID().toString().substring(0, 8));
        Git git = null;

        try {
            logger.info("克隆目标仓库 {} 到临时目录 {}", targetRemoteUrl, tempCloneDir);
            git = Git.cloneRepository().setURI(targetRemoteUrl).setDirectory(tempCloneDir.toFile())
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(targetUsername, targetToken))
                    .call();

            Path fullTargetPathInClone = tempCloneDir.resolve(targetPathInRepo);
            if (Files.exists(fullTargetPathInClone)) {
                logger.info("目标路径 {} 在克隆仓库中已存在，将被清理。", fullTargetPathInClone);
                FileUtils.deleteDirectory(fullTargetPathInClone.toFile());
            }
            Files.createDirectories(fullTargetPathInClone);

            logger.info("复制文件从源路径 {} 到克隆仓库的目标路径 {}", sourceDirectoryPath, fullTargetPathInClone);
            FileUtils.copyDirectory(sourceDirectoryPath.toFile(), fullTargetPathInClone.toFile());

            logger.info("执行 git add {} (相对于仓库根)", targetPathInRepo);
            git.add().addFilepattern(targetPathInRepo).call();

            logger.info("执行 git commit -m \"{}\"", commitMessage);
            git.commit().setMessage(commitMessage).call();

            logger.info("执行 git push 到远程仓库 {}", targetRemoteUrl);
            PushCommand pushCommand = git.push();
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(targetUsername, targetToken));
            pushCommand.call();

            logger.info("成功提交并推送到目标GitHub仓库: {}", targetRepoFullName);

        } finally {
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
     * @throws GitAPIException 如果Git操作失败
     * @throws BusinessException 如果目标仓库配置不完整 */
    public void commitAndPushToTargetRepo(Path sourceDirectoryPath, String targetRepoName, String targetPathInRepo,
            String commitMessage) throws IOException, GitAPIException {

        if (targetRepoName == null || targetRepoName.trim().isEmpty()) {
            targetRepoName = gitHubProperties.getTarget().getRepoName();
        }

        String targetUsername = gitHubProperties.getTarget().getUsername();
        String targetToken = gitHubProperties.getTarget().getToken();

        if (targetUsername == null || targetUsername.trim().isEmpty()) {
            throw new BusinessException("目标GitHub仓库的用户名未配置 (github.target.username)");
        }
        if (targetToken == null || targetToken.trim().isEmpty()) {
            throw new BusinessException("目标GitHub仓库的Token未配置或为空 (github.target.token)");
        }

        String targetRepoFullName = targetUsername + "/" + targetRepoName;
        String targetRemoteUrl = "https://github.com/" + targetRepoFullName + ".git";
        logger.info("准备提交到目标仓库: {}，目标路径: {}，操作用户: {}", targetRemoteUrl, targetPathInRepo, targetUsername);

        Path tempCloneDir = Files
                .createTempDirectory("target-repo-clone-" + UUID.randomUUID().toString().substring(0, 8));
        Git git = null;

        try {
            logger.info("克隆目标仓库 {} 到临时目录 {}", targetRemoteUrl, tempCloneDir);
            git = Git.cloneRepository().setURI(targetRemoteUrl).setDirectory(tempCloneDir.toFile())
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(targetUsername, targetToken))
                    .call();

            Path fullTargetPathInClone = tempCloneDir.resolve(targetPathInRepo);
            if (Files.exists(fullTargetPathInClone)) {
                logger.info("目标路径 {} 在克隆仓库中已存在，将被清理。", fullTargetPathInClone);
                FileUtils.deleteDirectory(fullTargetPathInClone.toFile());
            }
            Files.createDirectories(fullTargetPathInClone);

            logger.info("复制文件从源路径 {} 到克隆仓库的目标路径 {}", sourceDirectoryPath, fullTargetPathInClone);
            FileUtils.copyDirectory(sourceDirectoryPath.toFile(), fullTargetPathInClone.toFile());

            logger.info("执行 git add {} (相对于仓库根)", targetPathInRepo);
            git.add().addFilepattern(targetPathInRepo).call();

            logger.info("执行 git commit -m \"{}\"", commitMessage);
            git.commit().setMessage(commitMessage).call();

            logger.info("执行 git push 到远程仓库 {}", targetRemoteUrl);
            PushCommand pushCommand = git.push();
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(targetUsername, targetToken));
            pushCommand.call();

            logger.info("成功提交并推送到目标GitHub仓库: {}", targetRepoFullName);

        } finally {
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