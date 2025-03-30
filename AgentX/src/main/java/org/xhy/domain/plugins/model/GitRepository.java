package org.xhy.domain.plugins.model;

import org.xhy.infrastructure.exception.BusinessException;
import java.util.regex.Pattern;

/**
 * Git仓库值对象
 */
public class GitRepository {
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://|git@)([\\w.-]+)(/|:)([\\w.-]+)/([\\w.-]+)(\\.git)?$"
    );

    private final String url;

    private GitRepository(String url) {
        this.url = url;
    }

    public static GitRepository from(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new BusinessException("仓库地址不能为空");
        }
        if (!URL_PATTERN.matcher(url).matches()) {
            throw new BusinessException("无效的Git仓库地址");
        }
        return new GitRepository(url.trim());
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitRepository that = (GitRepository) o;
        return url.equals(that.url);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}
