package org.xhy.infrastructure.docker;

import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.command.PullImageResultCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 拉取镜像结果回调 */
public class CustomPullImageResultCallback extends com.github.dockerjava.core.command.PullImageResultCallback {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomPullImageResultCallback.class);

    @Override
    public void onNext(PullResponseItem item) {
        super.onNext(item);
        
        if (item.getStatus() != null) {
            if (item.getProgress() != null) {
                logger.debug("拉取镜像进度: {} - {}", item.getStatus(), item.getProgress());
            } else {
                logger.info("拉取镜像状态: {}", item.getStatus());
            }
        }
        
        if (item.getErrorDetail() != null) {
            logger.error("拉取镜像错误: {}", item.getErrorDetail().getMessage());
        }
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("拉取镜像失败", throwable);
        super.onError(throwable);
    }

    @Override
    public void onComplete() {
        logger.info("镜像拉取完成");
        super.onComplete();
    }
}