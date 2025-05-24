package org.xhy.infrastructure.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.domain.tool.model.dto.GitHubRepoInfo;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** GitHub URL 解析工具类。 负责将 GitHub URL 字符串解析成 GitHubRepoInfo 对象，不涉及任何 API 调用。 支持灵活的 URL 格式，包括不带分支/Tag或指向子路径的URL。 */
public class GitHubUrlParser {

    private static final Logger logger = LoggerFactory.getLogger(GitHubUrlParser.class);

    // GitHub URL 正则表达式，允许 ref (分支/Tag/Commit SHA) 和 path 部分可选。
    // group(1): owner
    // group(2): repoName
    // group(3): tree 或 blob (可选)
    // group(4): ref (分支/Tag/Commit SHA，与 tree/blob 配合使用，可选)
    // group(5): 仓库内路径 (可选)
    private static final Pattern GITHUB_URL_PATTERN = Pattern
            .compile("^https://github\\.com/([\\w.-]+)/([\\w.-]+)(?:/(tree|blob)/([\\w.-]+)(/(.*))?)?$");

    // 私有构造函数，防止工具类被实例化
    private GitHubUrlParser() {
    }

    /** 解析 GitHub URL 的格式（正则表达式）。 支持灵活的 URL 格式，允许 URL 不包含分支或 Tag 信息，或指向仓库内的子路径。 如果解析成功，返回 GitHubRepoInfo 对象。如果失败，抛出
     * BusinessException。 此方法不进行 GitHub API 调用，只关注 URL 结构。
     *
     * @param githubUrl 待解析的 GitHub URL
     * @return 包含解析信息的 GitHubRepoInfo 对象
     * @throws BusinessException 如果 URL 为空或基本格式不正确 */
    public static GitHubRepoInfo parseGithubUrl(String githubUrl) {
        if (githubUrl == null || githubUrl.trim().isEmpty()) {
            throw new BusinessException("GitHub URL 不能为空。");
        }

        Matcher matcher = GITHUB_URL_PATTERN.matcher(githubUrl);
        if (!matcher.matches()) {
            throw new BusinessException("无效的 GitHub URL 格式：" + githubUrl);
        }

        String owner = matcher.group(1);
        String repoName = matcher.group(2);
        // String type = matcher.group(3); // "tree" 或 "blob"，这里暂时不直接使用
        String ref = matcher.group(4); // 如果没有 ref，则为 null
        String pathInRepoWithLeadingSlash = matcher.group(5); // 仓库内的路径，例如 "/src" 或 "/file.txt"，如果 URL 指向 ref 根目录则为 null
        String pathInRepo = (pathInRepoWithLeadingSlash != null && pathInRepoWithLeadingSlash.startsWith("/"))
                ? pathInRepoWithLeadingSlash.substring(1)
                : pathInRepoWithLeadingSlash;

        logger.info("解析 GitHub URL：owner={}，repo={}，引用（Tag/分支）={}，仓库内路径={}", owner, repoName, ref, pathInRepo);
        return new GitHubRepoInfo(owner, repoName, ref, pathInRepo);
    }
}