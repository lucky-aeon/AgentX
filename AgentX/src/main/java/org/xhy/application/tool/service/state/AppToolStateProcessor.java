package org.xhy.application.tool.service.state;

import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;

/** 应用层工具状态处理器接口
 * 
 * 与领域层 ToolStateProcessor 的区别： 1. 可以依赖基础设施层服务（如MCPGatewayService、GitHubService等） 2. 可以依赖其他应用层服务 3. 处理需要外部集成的复杂业务场景 4.
 * 支持依赖注入，便于测试和扩展 */
public interface AppToolStateProcessor {

    /** 获取处理器对应的状态
     * 
     * @return 工具状态 */
    ToolStatus getStatus();

    /** 处理工具状态
     * 
     * 此方法可以： 1. 调用基础设施层服务 2. 修改工具实体的属性 3. 执行复杂的业务逻辑 4. 抛出 BusinessException 表示处理失败
     * 
     * @param tool 工具实体
     * @throws org.xhy.infrastructure.exception.BusinessException 处理失败时抛出 */
    void process(ToolEntity tool);

    /** 获取处理成功后的下一个状态
     * 
     * @return 下一个状态，如果没有自动下一状态返回null */
    ToolStatus getNextStatus();
}