package org.xhy.interfaces.api.portal.agent;

import org.springframework.web.bind.annotation.*;
import org.xhy.application.rag.service.RagQaDatasetAppService;
import org.xhy.application.rag.dto.RagQaDatasetDTO;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;

import java.util.List;

/** Agent知识库管理控制器 用于Agent配置中的知识库选择和管理 */
@RestController
@RequestMapping("/agents/knowledge-bases")
public class AgentKnowledgeBaseController {

    private final RagQaDatasetAppService ragQaDatasetAppService;

    public AgentKnowledgeBaseController(RagQaDatasetAppService ragQaDatasetAppService) {
        this.ragQaDatasetAppService = ragQaDatasetAppService;
    }

    /** 获取用户可用的知识库列表（用于Agent配置） 只返回已安装的知识库（用户创建的知识库会自动安装）
     * @return 知识库列表 */
    @GetMapping("/available")
    public Result<List<RagQaDatasetDTO>> getAvailableKnowledgeBases() {
        String userId = UserContext.getCurrentUserId();
        List<RagQaDatasetDTO> datasets = ragQaDatasetAppService.getUserAvailableDatasets(userId);
        return Result.success(datasets);
    }

    /** 获取知识库详情（用于Agent配置时显示知识库信息） 优先查找用户已安装的知识库快照，如果没有则查找原始知识库
     * @param knowledgeBaseId 知识库ID（可能是originalRagId或userRagId）
     * @return 知识库详情 */
    @GetMapping("/{knowledgeBaseId}")
    public Result<RagQaDatasetDTO> getKnowledgeBaseDetail(@PathVariable String knowledgeBaseId) {
        String userId = UserContext.getCurrentUserId();
        RagQaDatasetDTO dataset = ragQaDatasetAppService.getAvailableDatasetById(knowledgeBaseId, userId);
        return Result.success(dataset);
    }

    /** 批量获取知识库详情（用于Agent编辑时回显已选择的知识库）
     * @param knowledgeBaseIds 知识库ID列表，用逗号分隔
     * @return 知识库详情列表 */
    @GetMapping("/batch")
    public Result<List<RagQaDatasetDTO>> getKnowledgeBasesBatch(@RequestParam String knowledgeBaseIds) {
        String userId = UserContext.getCurrentUserId();
        String[] ids = knowledgeBaseIds.split(",");
        List<RagQaDatasetDTO> datasets = ragQaDatasetAppService.getDatasetsByIds(List.of(ids), userId);
        return Result.success(datasets);
    }
}
