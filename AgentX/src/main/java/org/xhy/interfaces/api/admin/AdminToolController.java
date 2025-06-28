package org.xhy.interfaces.api.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.admin.tool.service.AdminToolAppService;
import org.xhy.application.tool.dto.ToolWithUserDTO;
import org.xhy.application.tool.dto.ToolStatisticsDTO;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.tool.request.CreateToolRequest;
import org.xhy.interfaces.dto.tool.request.QueryToolRequest;

import java.util.List;

/** 管理员Tool管理 */
@RestController
@RequestMapping("/admin/tools")
public class AdminToolController {

    private final AdminToolAppService adminToolAppService;

    public AdminToolController(AdminToolAppService adminToolAppService) {
        this.adminToolAppService = adminToolAppService;
    }

    /** 分页获取工具列表
     * 
     * @param queryToolRequest 查询参数
     * @return 工具分页列表 */
    @GetMapping
    public Result<Page<ToolWithUserDTO>> getTools(QueryToolRequest queryToolRequest) {
        return Result.success(adminToolAppService.getTools(queryToolRequest));
    }

    /** 获取工具统计信息
     * 
     * @return 工具统计数据 */
    @GetMapping("/statistics")
    public Result<ToolStatisticsDTO> getToolStatistics() {
        return Result.success(adminToolAppService.getToolStatistics());
    }

    /** 创建官方工具
     * 
     * @param request 工具创建请求
     * @return 创建结果 */
    @PostMapping("/official")
    public Result<String> createOfficialTool(@RequestBody @Validated CreateToolRequest request) {
        String userId = UserContext.getCurrentUserId();
        String toolId = adminToolAppService.createOfficialTool(request, userId);
        return Result.success(toolId);
    }

    /** 修改工具的状态
     * @param toolId 工具 id
     * @param status 工具状态
     * @param reason 如果审核未通过，则说明未通过原因
     * @return */
    @PostMapping("/{toolId}/status")
    public Result updateStatus(@PathVariable String toolId, ToolStatus status,
            @RequestParam(required = false) String reason) {
        if (status == ToolStatus.FAILED && (reason == null || reason.isEmpty())) {
            return Result.serverError("拒绝操作需要提供原因");
        }
        adminToolAppService.updateToolStatus(toolId, status, reason);
        return Result.success();
    }

    /** 更新工具全局状态
     * 
     * @param toolId 工具ID
     * @param request 更新请求
     * @return 操作结果 */
    @PutMapping("/{toolId}/global-status")
    public Result updateGlobalStatus(@PathVariable String toolId,
            @RequestBody @Validated UpdateGlobalStatusRequest request) {
        adminToolAppService.updateToolGlobalStatus(toolId, request.getIsGlobal());
        return Result.success();
    }

    /** 更新全局状态请求 */
    public static class UpdateGlobalStatusRequest {
        @NotNull(message = "全局状态不可为空")
        private Boolean isGlobal;

        public Boolean getIsGlobal() {
            return isGlobal;
        }

        public void setIsGlobal(Boolean isGlobal) {
            this.isGlobal = isGlobal;
        }
    }

}
