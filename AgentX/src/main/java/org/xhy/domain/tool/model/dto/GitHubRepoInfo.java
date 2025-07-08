package org.xhy.domain.tool.model.dto;

/** 封装从 GitHub URL 解析出来的信息 */
public class GitHubRepoInfo {
    private String owner;
    private String repoName;
    private String ref; // 分支、标签或 commit SHA
    private String pathInRepo; // 仓库内的路径 (例如 /tree/main/src 中的 src)

    public GitHubRepoInfo(String owner, String repoName, String ref, String pathInRepo) {
        this.owner = owner;
        this.repoName = repoName;
        this.ref = ref;
        this.pathInRepo = pathInRepo;
    }

    public String getOwner() {
        return owner;
    }

    public String getRepoName() {
        return repoName;
    }

    public String getRef() {
        return ref;
    }

    public String getPathInRepo() {
        return pathInRepo;
    }

    /** 获取仓库的完整名称，格式为 "owner/repoName" */
    public String getFullName() {
        return owner + "/" + repoName;
    }
}