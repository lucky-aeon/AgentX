# Agent 预先设置工具参数


**需求**

Agent 中携带工具，而有些工具的参数可以提前设置，这样，TA 人在使用的时候就可以省去自己配置的步骤，例如：



![](https://cdn.nlark.com/yuque/0/2025/png/29091062/1747923030511-98c0b9ee-ea7a-440f-baeb-5dd07a12fc52.png)



你开发了一个前端网站部署助手，涉及到俩个工具：file-system,surge（部署前端用的），而 surge 的使用需要账号密码，如果你将这个 Agent 发布出去给别人使用，那么别人在使用的时候还需要使用 surge 的账号密码，这对于使用者来说体验很差（你可能会想，我的密钥凭什么给别人用？但是确实有这种场景）



那么应该如何处理？如果将密钥设置到系统提示词中，是可以解决的，但是使用者可以通过提示词注入的方式拿到密钥



我们可以通过在调用工具的时候直接将参数传入来实现这个需求，这样的话，只需要告诉大模型，xxx 工具已经设置好参数，可以直接调用，但是 langchan4j 未能实现，因此我们自己实现即可





在 agent 中需要添加额外字段：预先设置工具参数字段，结构如下：

```java
[
    {
"<tool_name>": [
            {
                "<工具名称>": {
                    "参数":"value"
                }
            }
        ]
    }
]
```

**修改的源码**

```java
McpClient client = new DefaultMcpClient.Builder().transport(transport)
                   .build();
            client.presetParameters(Collections.singletonList(
                    new PresetParameter("surge_login","{\"email\": \"xxx@qq.com\", \"password\": \"xxx\"}")));
            mcpClients.add(client);
```

DefaultMcpClient

```java
private ToolExecutionRequest tryPresetParameter(ToolExecutionRequest executionRequest) {
        String name = executionRequest.name();
        if (presetParameterMap.containsKey(name)) {
            String presetParameter = presetParameterMap.get(name);
            ToolExecutionRequest.Builder builder = ToolExecutionRequest.builder().id(executionRequest.id()).name(name).arguments(presetParameter);
            return builder.build();
        }
        return executionRequest;
    }
```

效果能实现，但是问题是：这种的提前设置，大模型是不知道的，很可能会 “找用户索要信息”，那用户就不得不在系统提示词里面编写对应的策略。 那这样也会增加用户的心智负担

**解决方案**

只能通过修改框架 + 系统提示词拼接的方式即可



0 = {ProcessedSegment@2512} "ProcessedSegment{content='# Agent 预先设置工具参数\n\n## 需求\n\nAgent 中携带工具，而有些工具的参数可以提前设置，这样，TA 人在使用的时候就可以省去自己配置的步骤，例如：\n\n参数预设\n\n你开发了一个前端网站部署助手，涉及到俩个工具：file-system,surge（部署前端用的），而 surge 的使用需要账号密码，如果你将这个 Agent 发布出去给别人使用，那么别人在使用的时候还需要使用 surge 的账号密码，这对于使用者来说体验很差（你可能会想，我的密钥凭什么给别人用？但是确实有这种场景）\n\n那么应该如何处理？如果将密钥设置到系统提示词中，是可以解决的，但是使用者可以通过提示词注入的方式拿到密钥\n\n我们可以通过在调用工具的时候直接将参数传入来实现这个需求，这样的话，只需要告诉大模型，xxx 工具已经设置好参数，可以直接调用，但是 langchan4j 未能实现，因此我们自己实现即可\n\n在 agent 中需要添加额外字段：预先设置工具参数字段，结构如下：', type='section', order=0, metadata={}}"
1 = {ProcessedSegment@2513} "ProcessedSegment{content='```java\n[\n    {\n"<tool_name>": [\n            {\n                "<工具名称>": {\n                    "参数":"value"\n                }\n            }\n        ]\n    }\n]\n```', type='code', order=1, metadata={is_fenced=true, raw_content=[\n    {\n"<tool_name>": [\n            {\n                "<工具名称>": {\n                    "参数":"value"\n                }\n            }\n        ]\n    }\n], language=java, type=code, lines=11}}"
2 = {ProcessedSegment@2514} "ProcessedSegment{content='', type='section', order=2, metadata={}}"
3 = {ProcessedSegment@2515} "ProcessedSegment{content='## 修改的源码', type='section', order=3, metadata={}}"
4 = {ProcessedSegment@2516} "ProcessedSegment{content='```java\nMcpClient client = new DefaultMcpClient.Builder().transport(transport)\n               .build();\n        client.presetParameters(Collections.singletonList(\n                new PresetParameter("surge_login","{"email": "xxx@qq.com", "password": "xxx"}")));\n        mcpClients.add(client);\n```', type='code', order=4, metadata={is_fenced=true, raw_content=McpClient client = new DefaultMcpClient.Builder().transport(transport)\n               .build();\n        client.presetParameters(Collections.singletonList(\n                new PresetParameter("surge_login","{"email": "xxx@qq.com", "password": "xxx"}")));\n        mcpClients.add(client);, language=java, type=code, lines=5}}"
5 = {ProcessedSegment@2517} "ProcessedSegment{content='### DefaultMcpClient', type='section', order=5, metadata={}}"
6 = {ProcessedSegment@2518} "ProcessedSegment{content='```java\nprivate ToolExecutionRequest tryPresetParameter(ToolExecutionRequest executionRequest) {\n        String name = executionRequest.name();\n        if (presetParameterMap.containsKey(name)) {\n            String presetParameter = presetParameterMap.get(name);\n            ToolExecutionRequest.Builder builder = ToolExecutionRequest.builder().id(executionRequest.id()).name(name).arguments(presetParameter);\n            return builder.build();\n        }\n        return executionRequest;\n    }\n```', type='code', order=6, metadata={is_fenced=true, raw_content=private ToolExecutionRequest tryPresetParameter(ToolExecutionRequest executionRequest) {\n        String name = executionRequest.name();\n        if (presetParameterMap.containsKey(name)) {\n            String presetParameter = presetParameterMap.get(name);\n            ToolExecutionRequest.Builder builder = ToolExecutionRequest.builder().id(executionRequest.id()).name(name).arguments(presetParameter);\n           "
7 = {ProcessedSegment@2519} "ProcessedSegment{content='效果能实现，但是问题是：这种的提前设置，大模型是不知道的，很可能会 "找用户索要信息"，那用户就不得不在系统提示词里面编写对应的策略。 那这样也会增加用户的心智负担', type='section', order=7, metadata={}}"
8 = {ProcessedSegment@2520} "ProcessedSegment{content='## 解决方案\n\n只能通过修改框架 + 系统提示词拼接的方式即可', type='section', order=8, metadata={}}"