# 后端开发规范
## 1. 代码规范
### 1.1 注释要求
+ 禁止使用 Lombok 注解
+ 每个类、方法、字段都必须包含完整的 Javadoc 注释
    - 注释需说明目的、参数、返回值及可能抛出的异常
    - 目的：保证代码开源质量，便于教学和维护

### 1.2 命名约定
+ 方法命名：
    - `getXxx()`：必须返回非空结果，否则抛出异常
    - `findXxx()`：允许返回 null
    - `checkXxxExist()`：执行存在性检查并抛出异常
    - `existXxx()`：返回布尔型存在性结果
+ 类命名：
    - 遵循 `XxxRequest`, `XxxDTO`, `XxxAggregate`, `XxxEntity` 模式
    - `XxxAppService``XxxDomainService` `XxxRepository`

## 2. 架构规范
### 2.1 分层架构
+ **API层**：
    - 使用 Restful 路由风格
    - 接收参数使用 `XxxRequestObject`
    - 必须使用 `@Validated` 进行基础数据校验
+ **Application层**：
    - 负责业务流程编排
    - 使用 `XxxDTO` 进行数据传输
    - 执行业务逻辑校验（数据存在性、业务规则等）
+ **Domain层**：
    - 使用 `XxxEntity` 和 `XxxAggregate` 表达领域模型
    - 负责核心业务逻辑和领域规则校验
    - 禁止跨领域直接调用

### 2.2 对象转换
+ 使用 `XxxAssembler` 专用类处理对象转换
+ 允许合理使用 `BeanUtils.copy` 进行简单属性复制
+ 转换规则：
    - API → Application：`RequestObject` → `DTO`
    - Application → Domain：`DTO` → `Entity`
    - Domain → Application：`Entity/Aggregate` → `DTO`

## 3. 数据库操作规范
### 3.1 查询操作
+ 必须明确区分严格查询和宽松查询：

```java
/**
 * 获取服务商信息（必须存在）
 * @param providerId 服务商ID
 * @param userId 用户ID
 * @return 服务商实体
 * @throws BusinessException 当服务商不存在时抛出
 */
public ProviderEntity getProvider(String providerId, String userId) {
    Wrapper<ProviderEntity> wrapper = Wrappers.<ProviderEntity>lambdaQuery()
        .eq(ProviderEntity::getId, providerId)
        .eq(ProviderEntity::getUserId, userId);
    ProviderEntity provider = providerRepository.selectOne(wrapper);
    if (provider == null) {
        throw new BusinessException("服务商不存在");
    }
    return provider;
}

/**
 * 查找服务商信息（允许不存在）
 * @param providerId 服务商ID
 * @param userId 用户ID
 * @return 服务商实体，可能为null
 */
public ProviderEntity findProvider(String providerId, String userId) {
    Wrapper<ProviderEntity> wrapper = Wrappers.<ProviderEntity>lambdaQuery()
        .eq(ProviderEntity::getId, providerId)
        .eq(ProviderEntity::getUserId, userId);
    return providerRepository.selectOne(wrapper);
}
```

### 3.2 更新操作
+ 必须包含用户ID作为操作上下文
+ 采用直接更新方式，避免"先查询后更新"模式
+ 更新失败应抛出具体异常

## 4. 校验规范
### 4.1 三层校验机制
1. **API层校验**：
    - 基础数据格式校验
    - 使用 `@Validated` 注解
    - 校验字段格式、必填项等
2. **Application层校验**：
    - 业务规则校验
    - 数据存在性检查
    - 操作权限验证
3. **Domain层校验**：
    - 实体状态校验
    - 领域规则验证
    - 不变条件维护

## 5. 文档管理
+ 所有SQL脚本放在 `docs/sql` 目录下
+ 设计文档放在 `docs/design` 目录下
+ API文档使用apifox plugin 生成，因此方法和对象的注释需要写清楚（更好的生成

## 6.安全规范
+ 所有涉及到密钥相关必须加密存储，并且不可以明文存储！
+ 所有的用户输入都要经过安全校验！

## 7. 开发规范
### entity json字段处理
entity 涉及到常量，json，需要使用对象进行包装，表字段通过 json 进行映射，并且通过 `org/xhy/infrastructure/converter`进行转换，还需要通过 `org/xhy/infrastructure/config/MyBatisTypeHandlerConfig.java` 进行注册



### entity 常量字段处理
entity 涉及到常量需要用常量表示，并且常量必须是大写，例如 tool_type 中的 MCP 和 FUNCTION_CALLING。使用大写可以表示是常量并非是字符串


### 事务注意事项
必须在 app 层写事务，禁止在 domain 层编写事务，因为这样会导致事务嵌套

## 9. 日志规范
1.日志打印使用中文

2.日志打印不允许打印密钥，如果需要则使用中间***代替

## 10.Git规范
目前分支管理分为三种类型分支：

master：稳定版

develop：开发版本（最新代码）

功能分支（由各自负责迭代的分支），命名要求：时间-功能说明，例如：2025-04-30-feat/xxxx


**分支开发要点**

**1.从 master checkout 进行开发**

**2.不可从 develop checkout 进行开发（因为后续推向 master 时会包含 dev 分支的代码）**

****

**也就是说：pr 需要 pr 到 master 和 dev 俩个分支**

## 11.修订记录
| 版本 | 日期 | 修改内容 | 修改人 |
| --- | --- | --- | --- |
| 1.0 | 2025-3-31 | 初始版本 | xhy |
| 1.1 | 2025-4-03 | 添加开发规范 entity json字段处理 | xhy |
| 1.1.2 | 2025-4-03 | 添加开发环境所需配置的环境变量 | liuscraft |
| 1.1.3 | 2025-4-14 | 添加 entity 常量处理方式 |  xhy |
| 1.1.4 | 2025-4-15 | 添加日志规范 | xhy |


本规范将随项目发展持续更新，所有开发人员有责任提出改进建议。

