package org.xhy.interfaces.api.portal.rag;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.application.rag.dto.DocumentUnitDTO;
import org.xhy.application.rag.dto.RagSearchRequest;
import org.xhy.application.rag.dto.RagStreamChatRequest;
import org.xhy.application.rag.service.search.RAGSearchAppService;
import org.xhy.application.conversation.service.ConversationAppService;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;

import java.util.List;

/** RAG搜索控制器
 * @author shilong.zang
 * @date 2024-12-09 */
@RestController
@RequestMapping("/rag/search")
public class RagSearchController {

    private final RAGSearchAppService ragSearchAppService;
    private final ConversationAppService conversationAppService;

    public RagSearchController(RAGSearchAppService ragSearchAppService,
            ConversationAppService conversationAppService) {
        this.ragSearchAppService = ragSearchAppService;
        this.conversationAppService = conversationAppService;
    }

    /** RAG搜索文档
     * 
     * @param request RAG搜索请求
     * @return 搜索结果 */
    @PostMapping
    public Result<List<DocumentUnitDTO>> ragSearch(@RequestBody @Validated RagSearchRequest request) {
        String userId = UserContext.getCurrentUserId();
        List<DocumentUnitDTO> searchResults = ragSearchAppService.ragSearch(request, userId);
        return Result.success(searchResults);
    }

    /** RAG流式问答 - 使用统一架构
     * 
     * @param request 流式问答请求
     * @return 流式响应 */
    @PostMapping("/stream-chat")
    public SseEmitter ragStreamChat(@RequestBody @Validated RagStreamChatRequest request) {
        String userId = UserContext.getCurrentUserId();
        return ragSearchAppService.ragStreamChat(request, userId);
    }

    /** 基于已安装知识库的RAG搜索
     * 
     * @param userRagId 已安装的知识库ID
     * @param request RAG搜索请求
     * @return 搜索结果 */
    @PostMapping("/user-rag/{userRagId}")
    public Result<List<DocumentUnitDTO>> ragSearchByUserRag(@PathVariable String userRagId,
            @RequestBody @Validated RagSearchRequest request) {
        String userId = UserContext.getCurrentUserId();
        List<DocumentUnitDTO> searchResults = ragSearchAppService.ragSearchByUserRag(request, userRagId, userId);
        return Result.success(searchResults);
    }

    /** 基于已安装知识库的RAG流式问答 - 使用统一架构
     * 
     * @param userRagId 已安装的知识库ID
     * @param request 流式问答请求
     * @return 流式响应 */
    @PostMapping("/user-rag/{userRagId}/stream-chat")
    public SseEmitter ragStreamChatByUserRag(@PathVariable String userRagId,
            @RequestBody @Validated RagStreamChatRequest request) {
        String userId = UserContext.getCurrentUserId();
        return ragSearchAppService.ragStreamChatByUserRag(request, userRagId, userId);
    }

}