# 项目技术亮点总结（面试简历用）

> 项目名称：**GTA V AI Assistant** — 基于 Spring Boot + Spring AI 的 RAG 知识库 & ReAct Agent 系统
>
> 技术栈：Java 21 / Spring Boot 3.5.9 / Spring AI 1.1.2 / Milvus 向量数据库 / PostgreSQL / Vue.js 3 / Docker Compose
>
> 本文档从代码实现层面逐一分析项目中值得写进简历的技术亮点，每个亮点均精确到源码文件和核心逻辑。

---

## 亮点 1：三层抽象的 ReAct Agent 架构（模板方法 + 状态机）

**涉及文件**：
- `src/main/java/com/mercemay/aiagent/agent/BaseAgent.java`
- `src/main/java/com/mercemay/aiagent/agent/ReActAgent.java`
- `src/main/java/com/mercemay/aiagent/agent/ToolCallAgent.java`
- `src/main/java/com/mercemay/aiagent/agent/AIAgent.java`
- `src/main/java/com/mercemay/aiagent/agent/model/AgentState.java`

**问题背景**：
ReAct（Reasoning + Acting）是当前 LLM Agent 的主流范式，但如何把"思考→行动→观察"这个循环用 Java 面向对象的方式优雅地实现，是一个架构设计问题。如果不做抽象，所有逻辑堆在一个类里，既难以测试，也无法扩展出不同类型的 Agent。

**实现细节**：

项目采用了**三层继承**的模板方法模式：

```
BaseAgent（状态机 + 迭代循环 + 循环检测）
  └── ReActAgent（定义 think/act 抽象方法，step 中编排 think→act 流程）
        └── ToolCallAgent（实现 think/act：think 调 LLM 决策，act 执行工具调用）
              └── AIAgent（具体实例：注入工具集、系统提示词、最大迭代次数）
```

核心设计点：

1. **BaseAgent** 实现了完整的迭代控制逻辑：
   - `AgentState` 四态状态机（IDLE → PROCESSING → COMPLETED / ERROR）
   - `maxIterations` 最大迭代次数限制，防止无限循环
   - **连续重复输出检测**（`maxConsecutiveLoops`）：连续 3 次相同输出自动终止，防止 LLM 陷入死循环
   - `cleanUp()` 方法在 finally 块中重置状态，保证可复用

2. **ReActAgent** 定义了 `think()` 和 `act()` 的抽象接口，`step()` 方法编排二者的调用顺序

3. **ToolCallAgent** 实现了核心的 think/act 逻辑：
   - `think()`: 把完整的对话历史（`messageList`）+ 系统提示词发给 LLM，LLM 返回文本 + 工具调用列表
   - `act()`: 使用 Spring AI 的 `ToolCallingManager` 执行工具调用，**显式禁用了自动工具执行**（`internalToolExecutionEnabled(false)`），手动控制工具执行流程
   - 在 `act()` 中检测 `terminateAgent` 工具是否被调用来主动结束循环

4. **ToolCallAgent.step()** 重写了 ReActAgent 的 step()，对 LLM 输出做后处理：用正则清理 `Thought:` / `Action:` 标记，只返回思考过程的文本给用户

```java
// ToolCallAgent.java - 手动控制工具执行，而非让框架自动执行
this.chatOptions = ToolCallingChatOptions.builder()
        .toolCallbacks(availableTools)
        .internalToolExecutionEnabled(false) // 关键：手动控制
        .build();
```

```java
// BaseAgent.java - 连续重复输出检测
if (stepOutput != null && stepOutput.equals(lastStepOutput)) {
    consecutiveLoopCount++;
    if (consecutiveLoopCount >= maxConsecutiveLoops) {
        // 终止循环，防止 LLM 陷入死循环
    }
}
```

**技术价值**：
- 体现了**面向对象设计能力**：模板方法模式让每一层各司其职，遵循开闭原则
- 体现了**对 LLM Agent 工程的深入理解**：不仅仅调用 API，而是手动控制工具执行流程、添加循环检测、状态管理等工程化保障
- 面试时可以谈论：Agent 循环为什么要设置最大步数？为什么要做重复输出检测？为什么要手动控制工具执行而非自动？

---

## 亮点 2：完整的 RAG ETL 管线（加载 → 分割 → 富化 → 向量化）

**涉及文件**：
- `src/main/java/com/mercemay/aiagent/rag/AppDocumentLoader.java`
- `src/main/java/com/mercemay/aiagent/rag/AppCustomTextSplitter.java`
- `src/main/java/com/mercemay/aiagent/rag/AppDocumentKeywordEnricher.java`
- `src/main/java/com/mercemay/aiagent/rag/AppVectorStoreConfig.java`

**问题背景**：
RAG 系统中，知识入库的质量直接决定检索效果。简单地把文档一股脑塞进向量库，会导致 chunk 过大或过小、缺少元数据、检索不精准。需要一条完整的 ETL（Extract-Transform-Load）管线来保证数据质量。

**实现细节**：

`AppVectorStoreConfig.databaseInitializer()` 编排了完整的 ETL 流程：

```java
// Step 1: 文档加载
List<Document> documents = appDocumentLoader.loadDocuments();
// Step 2: 文本分割
List<Document> splitDocuments = appCustomTextSplitter.customizedSplitter(documents);
// Step 3: 关键词富化
List<Document> enrichedDocuments = appDocumentKeywordEnricher.enrichDocuments(splitDocuments);
// Step 4: 写入向量库
vectorStore.add(enrichedDocuments);
```

各环节的设计：

1. **文档加载**（`AppDocumentLoader`）：
   - 使用 `ResourcePatternResolver` 批量扫描 `classpath:/documents/*.md`
   - 配置了 `MarkdownDocumentReaderConfig`，跳过代码块和引用块，避免噪声
   - 为每个文档注入 **自定义元数据**（`status: active`，`game: gta_v`），后续检索时可以用元数据做过滤

2. **文本分割**（`AppCustomTextSplitter`）：
   - 使用 `TokenTextSplitter` 基于 Token 数而非字符数分割（更适合 LLM 场景）
   - 定制参数：chunk 最大 1000 token，最小 100 字符，单文档最多 10000 chunk
   - `keepSeparator: true` 保留分隔符上下文

3. **关键词富化**（`AppDocumentKeywordEnricher`）：
   - 使用 LLM（ChatModel）为每个 chunk 自动提取 **top-5 关键词**并写入元数据
   - 这是**很多 RAG 教程不会讲到的高级技巧**：关键词元数据可以增强向量检索的语义理解

4. **向量存储配置**：
   - Milvus 向量库，使用 `ivf_flat` 索引 + `cosine` 相似度度量
   - 4096 维向量（对应 `qwen3-embedding:8b` 模型的嵌入维度）

```java
// AppDocumentLoader.java - 注入自定义元数据
MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
        .withHorizontalRuleCreateDocument(false)
        .withIncludeCodeBlock(false)
        .withAdditionalMetadata("status", "active")
        .withAdditionalMetadata("game", "gta_v")
        .build();
```

**技术价值**：
- 体现了**对 RAG 系统端到端流程的理解**，不是只做检索，而是从数据入库开始就精心设计
- 关键词富化是区分"入门级"和"有深度"RAG 实现的重要标志
- 面试时可以谈论：为什么选 Token-based 分割而非字符分割？关键词元数据对检索有什么帮助？chunk size 怎么调优？

---

## 亮点 3：多路查询扩展 + 自定义检索器（Pre-Retrieval & Post-Retrieval 优化）

**涉及文件**：
- `src/main/java/com/mercemay/aiagent/rag/AppMultiQueryExpander.java`
- `src/main/java/com/mercemay/aiagent/rag/AppQueryRewriter.java`
- `src/main/java/com/mercemay/aiagent/rag/AppRagCustomAdvisorFactory.java`
- `src/main/java/com/mercemay/aiagent/rag/AppContextualQueryAugmenterFactory.java`

**问题背景**：
用户的查询往往不够精确，直接拿原始 query 做向量检索可能错过相关文档。例如用户问"三个主角最后怎么样了"，向量检索可能匹配不到"结局选择"相关的 chunk。需要在检索前后做优化。

**实现细节**：

项目实现了一条完整的查询优化链：**查询改写 → 多路扩展 → 并行检索 → 去重 → 上下文增强**。

1. **查询改写**（`AppQueryRewriter`）：
   - 使用 `RewriteQueryTransformer` 将用户原始 query 通过 LLM 改写为更适合检索的形式
   - 例：「三个主角最后怎么样了」→「GTA V 三个主角 Michael Trevor Franklin 结局选择 outcomes」

2. **多路查询扩展**（`AppMultiQueryExpander`）：
   - 将一个 query 通过 LLM 扩展为 3 个不同角度的查询
   - 增加检索的召回率，避免单一 query 视角的盲区

3. **自定义检索器**（`AppRagCustomAdvisorFactory`）：
   - **这是最大的亮点**：没有使用 Spring AI 的默认检索器，而是用 Lambda 表达式自定义了一个 `DocumentRetriever`
   - 检索逻辑：对每个扩展 query 并行执行向量检索（top-5），然后**合并去重**
   - 使用**元数据过滤**：只检索 `status = "active"` 的文档
   - 设置了**相似度阈值**（0.5），过滤低质量结果

```java
// AppRagCustomAdvisorFactory.java - 自定义多路检索器
DocumentRetriever multiQueryRetriever = (query) -> {
    List<Query> expandedQueries = expander.expandQuery(query.text(), 3);
    return expandedQueries.stream()
            .flatMap(q -> vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(q.text())
                            .topK(5)
                            .filterExpression(expression) // 元数据过滤
                            .similarityThreshold(0.5)     // 相似度阈值
                            .build())
                    .stream())
            .distinct()  // 去重
            .toList();
};
```

4. **空上下文处理**（`AppContextualQueryAugmenterFactory`）：
   - 当检索不到任何相关文档时，不让 LLM 瞎编，而是用预定义的提示词模板告知用户"知识库中没有相关信息"
   - `allowEmptyContext(false)` 禁止 LLM 在没有上下文的情况下自行回答

**技术价值**：
- 体现了**对 RAG 检索质量优化的系统性思考**：Pre-Retrieval（查询改写 + 多路扩展）+ Retrieval（元数据过滤 + 相似度阈值）+ Post-Retrieval（去重 + 空上下文兜底）
- 自定义 `DocumentRetriever` 而非使用默认实现，展示了对框架内部机制的深入理解
- 面试时可以谈论：为什么需要多路查询扩展？去重用了什么策略？空上下文为什么要特殊处理？

---

## 亮点 4：自定义 Spring AI Advisor 链（Re-Reading + 日志 + 记忆管理）

**涉及文件**：
- `src/main/java/com/mercemay/aiagent/advisor/ReReadingAdvisor.java`
- `src/main/java/com/mercemay/aiagent/advisor/MyLoggerAdvisor.java`
- `src/main/java/com/mercemay/aiagent/app/App.java`

**问题背景**：
Spring AI 的 Advisor 机制类似于 Spring MVC 的 Interceptor / AOP，可以在 LLM 调用前后插入自定义逻辑。项目需要解决几个横切关注点：请求/响应日志、多轮对话记忆、以及提高 LLM 回答准确性。

**实现细节**：

1. **ReReadingAdvisor**（Re2 论文的工程实现）：
   - 实现了 `BaseAdvisor` 接口的 `before()` 方法
   - 核心思想来自 NLP 论文 **Re2（Re-Reading）**：让 LLM 重复阅读问题可以提高理解准确性
   - 把用户原始问题 `{query}` 改写为 `{query}\nRead the question again: {query}`
   - 使用 `PromptTemplate` 做模板渲染，用 `chatClientRequest.mutate()` 修改请求

```java
// ReReadingAdvisor.java - Re2 论文的工程实现
String augmentedUserText = PromptTemplate.builder()
        .template("""
                {re2_input_query}
                Read the question again: {re2_input_query}
                """)
        .variables(Map.of("re2_input_query", chatClientRequest.prompt().getUserMessage().getText()))
        .build()
        .render();
return chatClientRequest.mutate()
        .prompt(chatClientRequest.prompt().augmentUserMessage(augmentedUserText))
        .build();
```

2. **MyLoggerAdvisor**（同时支持同步和流式调用的日志拦截器）：
   - 同时实现了 `CallAdvisor`（同步）和 `StreamAdvisor`（流式）两个接口
   - 流式场景使用 `ChatClientMessageAggregator` 聚合流式响应后再记录日志，避免碎片化日志
   - 日志级别使用 `DEBUG`，生产环境可通过配置控制

3. **Advisor 编排**（`App.java`）：
   - 按照责任链模式组装 Advisor：`MessageChatMemoryAdvisor` → `MyLoggerAdvisor` → `ReReadingAdvisor`（可选）
   - `MessageChatMemoryAdvisor` 基于 JDBC 持久化会话历史，窗口大小 20 条

```java
// App.java - Advisor 链编排
this.chatClient = ChatClient.builder(chatModel)
        .defaultSystem(this.systemPromptContent)
        .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                new MyLoggerAdvisor()
        )
        .build();
```

**技术价值**：
- **ReReadingAdvisor** 展示了将学术论文（Re2）落地到工程中的能力，这种"有理论依据的工程实现"非常加分
- **MyLoggerAdvisor** 同时支持 Call/Stream 两种模式，体现了对响应式编程和观察者模式的理解
- 面试时可以谈论：Spring AI Advisor 的责任链模式与 Spring MVC Interceptor 的异同？流式日志聚合怎么做的？

---

## 亮点 5：Agent SSE 流式响应 + 异步执行（SseEmitter + CompletableFuture）

**涉及文件**：
- `src/main/java/com/mercemay/aiagent/agent/BaseAgent.java`（`runStream` 方法）
- `src/main/java/com/mercemay/aiagent/controller/AIController.java`
- `src/main/java/com/mercemay/aiagent/app/App.java`（`chatStream` 方法）
- `front-end/src/views/ChatView.vue`
- `front-end/src/views/AgentView.vue`

**问题背景**：
LLM 的推理过程通常需要几秒到几十秒，如果使用传统的 HTTP 请求-响应模式，用户需要长时间等待。尤其是 ReAct Agent 需要多轮迭代，总耗时可能更长。需要实时地把每一步思考过程推送给用户。

**实现细节**：

项目实现了两套流式响应方案：

**方案 A：Reactor Flux（用于 RAG Chat 模式）**

```java
// App.java - 使用 Spring AI 原生的 Reactor Flux 流式
public Flux<String> chatStream(String message, String chatId) {
    return chatClient.prompt()
            .user(message)
            .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
            .stream()
            .content();
}

// AIController.java - 三种 SSE 端点供不同场景使用
@GetMapping(value = "/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> chatSse(String message, String chatId) { ... }

@GetMapping(value = "/chat/sse/generic")
public Flux<ServerSentEvent<String>> chatSseGeneric(String message, String chatId) { ... }

@GetMapping("/chat/sse/emitter")
public SseEmitter chatSseEmitter(String message, String chatId) { ... }
```

**方案 B：SseEmitter + CompletableFuture（用于 Agent 模式）**

Agent 的多步迭代不适合 Flux 模式（因为每一步是同步阻塞的 LLM 调用），所以使用了 `SseEmitter` + `CompletableFuture.runAsync()`：

```java
// BaseAgent.java - 异步推送每一步的执行结果
public SseEmitter runStream(String userPrompt) {
    SseEmitter sseEmitter = new SseEmitter(10 * 60 * 1000L); // 10分钟超时
    CompletableFuture.runAsync(() -> {
        // 每一步迭代的输出都通过 sseEmitter.send() 推送
        for (currentIteration = 1; ...) {
            String stepOutput = this.step();
            sseEmitter.send(stepOutput);
        }
        sseEmitter.complete();
    });
    // 注册超时和完成回调
    sseEmitter.onTimeout(() -> { ... });
    sseEmitter.onCompletion(() -> { ... });
    return sseEmitter;
}
```

**前端 SSE 解析**（`ChatView.vue` / `AgentView.vue`）：
- 使用原生 `fetch` + `ReadableStream` 而非 `EventSource`（因为 `EventSource` 不支持自定义 Header）
- 手动解析 SSE 协议：按 `\n\n` 分割事件，解析 `data:` 前缀
- 处理了不完整事件的 buffer 拼接、`\n` 转义、AbortController 取消等边界场景

```javascript
// ChatView.vue - 手动 SSE 解析
const events = buffer.split('\n\n')
buffer = events.pop() // 最后一个可能不完整
for (const event of events) {
    // ... 解析 data: 行，拼接内容
    content = content.replace(/\\n/g, '\n') // 反转义
}
```

**技术价值**：
- 提供了**三种 SSE 端点**（Flux / ServerSentEvent / SseEmitter），展示了对 Spring WebFlux 和 Servlet 两种模型的理解
- Agent 模式使用 `CompletableFuture.runAsync` + `SseEmitter` 解决了"同步多步迭代 + 实时推送"的矛盾
- 前端手动解析 SSE 处理了多种边界情况，体现了全栈能力

---

## 亮点 6：工具注册中心 + 安全沙箱工作空间

**涉及文件**：
- `src/main/java/com/mercemay/aiagent/tools/ToolRegistration.java`
- `src/main/java/com/mercemay/aiagent/manager/WorkspaceManager.java`
- `src/main/java/com/mercemay/aiagent/tools/FileOperationTool.java`
- `src/main/java/com/mercemay/aiagent/tools/PdfGeneratorTool.java`
- `src/main/java/com/mercemay/aiagent/tools/ResourceDownloadTool.java`
- `src/main/java/com/mercemay/aiagent/tools/TerminateAgentTool.java`

**问题背景**：
AI Agent 能调用工具意味着它可以操作文件系统和执行命令。如果不做隔离，Agent 可能读写任意路径的文件，造成安全问题。同时，需要一个统一的工具注册机制来管理所有可用工具。

**实现细节**：

1. **工具注册中心**（`ToolRegistration`）：
   - 使用 `@Configuration` + `@Bean` 将所有工具统一注册为 `ToolCallback[]`
   - 使用 Spring AI 的 `ToolCallbacks.from()` 工厂方法，从 POJO 自动生成工具回调
   - 工具实例的依赖（如 API Key、WorkspaceManager）通过构造注入管理

```java
@Bean
public ToolCallback[] toolCallbacks(WorkspaceManager workspaceManager) {
    return ToolCallbacks.from(
            new FileOperationTool(workspaceManager),
            new PdfGeneratorTool(workspaceManager),
            new ResourceDownloadTool(workspaceManager),
            new TerminalOperationTool(),
            new TerminateAgentTool(),
            new WebSearchTool(apiKey, searchUrl),
            new WebCrawlerTool()
    );
}
```

2. **安全沙箱工作空间**（`WorkspaceManager`）：
   - 实现了 `DisposableBean`，在 Bean 销毁时自动清理临时目录
   - 每次启动生成**唯一的临时工作目录**（UUID 命名）
   - `resolve()` 方法使用 `FileUtil.isSub()` **防止路径穿越攻击**：确保所有文件操作都在沙箱内

```java
// WorkspaceManager.java - 路径穿越防护
public File resolve(String filename) {
    File targetFile = FileUtil.file(this.workspaceRoot, filename);
    if (!FileUtil.isSub(this.workspaceRoot, targetFile)) {
        throw new IllegalArgumentException("Filename resolves outside of workspace: " + filename);
    }
    return targetFile;
}
```

3. **TerminateAgentTool**：
   - 一个特殊的"元工具"，Agent 调用它表示任务完成
   - 在 `ToolCallAgent.act()` 中检测该工具被调用后设置状态为 `COMPLETED`
   - 这是一种**Agent 自主决定终止**的机制，比等待最大步数更高效

**技术价值**：
- **路径穿越防护**是一个重要的安全实践，展示了安全意识
- WorkspaceManager 的生命周期管理（创建→使用→清理）体现了对 Spring Bean 生命周期的深入理解
- TerminateAgentTool 的设计展示了"让 Agent 控制自身生命周期"的思想

---

## 亮点 7：MCP（Model Context Protocol）双传输模式集成

**涉及文件**：
- `ai-agent-mcp-server/src/main/java/com/mercemay/aiagent/mcpserver/AiAgentMcpServerApplication.java`
- `ai-agent-mcp-server/src/main/java/com/mercemay/aiagent/mcpserver/tools/ImageSearchTool.java`
- `ai-agent-mcp-server/src/main/resources/application-sse.yml`
- `ai-agent-mcp-server/src/main/resources/application-stdio.yml`
- `src/main/resources/application.yml`（MCP Client 配置）

**问题背景**：
MCP（Model Context Protocol）是 Anthropic 提出的标准化协议，用于 LLM 应用与外部工具/数据源的通信。项目需要同时支持两种传输模式（SSE 和 stdio），以适应不同部署场景。

**实现细节**：

1. **MCP Server**（独立 Spring Boot 模块）：
   - 使用 `spring-ai-starter-mcp-server-webmvc` 构建 MCP Server
   - 通过 Spring Profile 切换 SSE 和 stdio 两种传输模式
   - `ImageSearchTool` 集成 Pexels API，通过 MCP 协议暴露给主应用

2. **MCP Client**（主应用配置）：
   - SSE 模式：连接自建的 `ai-agent-mcp-server`（端口 8081）
   - stdio 模式：通过 Docker 运行 `mcp/google-maps` 容器，直接通过标准输入输出通信

```yaml
# application.yml - 双传输模式 MCP 配置
spring.ai.mcp.client:
  sse:
    connections:
      search-images:
        url: http://localhost:8081          # SSE 模式：自建 MCP Server
  stdio:
    connections:
      google-maps:
        command: docker                      # stdio 模式：Docker 容器
        args: [run, -i, --rm, -e, GOOGLE_MAPS_API_KEY, mcp/google-maps]
```

3. **工具合并**（`AIAgent.java`）：
   - 静态注册的工具（`ToolCallback[]`）和 MCP 动态发现的工具（`ToolCallbackProvider`）通过 `mergeTools()` 合并
   - Agent 同时拥有本地工具和远程 MCP 工具的调用能力

```java
// AIAgent.java - 合并本地工具和 MCP 工具
private static ToolCallback[] mergeTools(ToolCallback[] a, ToolCallback[] b) {
    return Stream.concat(
            a != null ? Arrays.stream(a) : Stream.empty(),
            b != null ? Arrays.stream(b) : Stream.empty()
    ).toArray(ToolCallback[]::new);
}
```

**技术价值**：
- MCP 是 2024-2025 年 AI 工程领域的热门协议，掌握 MCP 集成是加分项
- **双传输模式**（SSE + stdio）展示了对不同部署架构的理解
- 工具合并机制使系统具备**可扩展性**：添加新的 MCP Server 只需改配置

---

## 亮点 8：基于 JDBC 的持久化会话记忆 + 窗口控制

**涉及文件**：
- `src/main/java/com/mercemay/aiagent/config/ChatMemoryConfig.java`
- `src/main/java/com/mercemay/aiagent/app/App.java`
- `src/main/resources/application.yml`

**问题背景**：
多轮对话需要维护会话上下文，但 LLM 的上下文窗口有限（且 token 越多成本越高）。需要在"记忆完整性"和"上下文窗口限制"之间做权衡。同时，内存存储在服务重启后会丢失。

**实现细节**：

1. **JDBC 持久化**：
   - 使用 `spring-ai-starter-model-chat-memory-repository-jdbc` 将会话历史存入 PostgreSQL
   - 配置 `initialize-schema: always` 自动建表
   - 服务重启后会话不丢失

2. **滑动窗口控制**：
   - `MessageWindowChatMemory` 设置最大消息数为 20
   - 超过 20 条时自动丢弃最早的消息，控制上下文长度

3. **会话 ID 隔离**：
   - 前端在 `onMounted` 时生成随机 chatId（`chat-` + 随机字符串）
   - 通过 `ChatMemory.CONVERSATION_ID` 参数传递给 Advisor，实现多用户/多会话隔离

```java
// ChatMemoryConfig.java
@Bean
public ChatMemory chatMemory(ChatMemoryRepository repository) {
    return MessageWindowChatMemory.builder()
            .chatMemoryRepository(repository)
            .maxMessages(20)
            .build();
}

// App.java - 使用会话 ID 隔离上下文
chatClient.prompt()
    .user(message)
    .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
    .call();
```

**技术价值**：
- 展示了对 LLM 应用中**上下文管理**这一核心问题的工程化解决方案
- JDBC 持久化 + 窗口控制是生产环境的标准做法
- 面试时可以谈论：为什么选择滑动窗口而非摘要压缩？窗口大小怎么确定？

---

## 亮点 9：Agent 提示词工程（结构化 ReAct Prompt + 步进式引导）

**涉及文件**：
- `src/main/java/com/mercemay/aiagent/agent/AIAgent.java`
- `src/main/resources/prompts/system_prompt.st`

**问题背景**：
ReAct Agent 的行为完全由提示词驱动。如果提示词设计不当，LLM 可能不按 Thought → Action → Observation 的格式输出，导致解析失败或行为混乱。

**实现细节**：

项目设计了两个层次的提示词：

1. **系统提示词**（`SYSTEM_PROMPT`）：定义 Agent 的角色、能力、严格规则
   - 明确规定了 ReAct 工作流：`Thought → Action → Observation → Repeat`
   - 9 条严格规则：不猜测数据、使用正确的工具参数格式、支持并行工具调用
   - 明确定义了终止信号：`Final Answer: [answer]`

2. **步进提示词**（`NEXT_STEP_PROMPT`）：每一步迭代时注入的上下文
   - 包含当前步数 `{current_step}` 和最大步数 `{max_steps}`（让 LLM 有紧迫感）
   - 包含完整的对话历史 `{conversation_history}`
   - 用**结构化模板**引导 LLM 按 Thought → Action 格式输出

```java
// AIAgent.java - 步进提示词
String NEXT_STEP_PROMPT = """
    You are now at step {current_step} of {max_steps}.
    
    Current full conversation history and tool results:
    {conversation_history}
    
    Continue strictly following this structure:
    
    Thought:
    - What does the latest observation tell you?
    - What progress have you made?
    - What information is still needed?
    
    Action:
    - If a tool is needed, call it now.
    - If done, write: Final Answer: [response]
    """;
```

**技术价值**：
- 提示词工程是 AI 应用开发的核心能力，项目展示了**分层、结构化**的提示词设计
- 步进提示词中注入步数和历史，是 ReAct Agent 工程化的关键技巧
- 面试时可以展示对 Prompt Engineering 的深入理解

---

## 亮点 10：Docker Compose 全栈编排 + 多服务协调

**涉及文件**：
- `docker-compose.yml`
- `front-end/vite.config.js`（代理配置）
- `src/main/java/com/mercemay/aiagent/config/CORSConfig.java`

**问题背景**：
系统依赖多个中间件（PostgreSQL、Milvus、MinIO、etcd），加上前端、后端、MCP Server 共 3 个应用服务，本地开发和部署的复杂度很高。需要一键启动的能力。

**实现细节**：

1. **Docker Compose** 编排了 6 个服务，并处理了服务间依赖：
   - Milvus 依赖 etcd（配置存储）和 MinIO（对象存储），通过 `depends_on` + health check 确保启动顺序
   - Attu（Milvus Web UI）用于可视化向量数据，方便调试

2. **前后端通信**：
   - Vite 开发服务器配置了反向代理，将 `/api` 请求转发到后端的 `host.docker.internal:8080`
   - 后端 `CORSConfig` 配置了全局跨域支持

3. **环境变量管理**：
   - 敏感信息（API Key 等）通过环境变量注入：`${OPENAI_API_KEY}`、`${SERPER_API_KEY}`
   - MCP Server 的 Google Maps API Key 通过 Docker 环境变量传递

**技术价值**：
- 展示了**全栈工程化能力**和 DevOps 意识
- 面试时可以谈论微服务编排、健康检查、环境变量管理等话题

---

## 亮点 11：丰富的工具生态（Web 搜索 + 爬虫 + PDF 生成 + 终端执行）

**涉及文件**：
- `src/main/java/com/mercemay/aiagent/tools/WebSearchTool.java`
- `src/main/java/com/mercemay/aiagent/tools/WebCrawlerTool.java`
- `src/main/java/com/mercemay/aiagent/tools/PdfGeneratorTool.java`
- `src/main/java/com/mercemay/aiagent/tools/TerminalOperationTool.java`
- `src/main/java/com/mercemay/aiagent/tools/FileOperationTool.java`
- `src/main/java/com/mercemay/aiagent/tools/ResourceDownloadTool.java`

**问题背景**：
Agent 的能力取决于它能调用的工具。项目需要让 Agent 能够搜索互联网、阅读网页、生成文档、下载资源、操作文件系统，形成一个完整的任务执行能力闭环。

**实现细节**：

每个工具都使用 `@Tool` 注解声明了**详细的描述**（包含 When to use、Capabilities、Limitations 等），这些描述会被 LLM 读取用于决策：

1. **WebSearchTool**：集成 Serper API（Google 搜索），解析 Knowledge Graph、Organic Results、People Also Ask 三种结果类型
2. **WebCrawlerTool**：使用 Jsoup 爬取网页文本内容
3. **PdfGeneratorTool**：使用 iTextPDF 9.5.0 生成 PDF，**支持中文字体**（配置了 SimSun、Microsoft YaHei 等字体族）
4. **TerminalOperationTool**：**自动检测操作系统**（Windows/Linux），选择对应的 Shell 执行命令
5. **ResourceDownloadTool**：HTTP 文件下载，返回人类可读的文件大小

```java
// WebSearchTool.java - 解析多种搜索结果类型
if (jsonObject.containsKey("knowledgeGraph")) { ... }  // 知识图谱
if (jsonObject.containsKey("organic")) { ... }          // 有机搜索结果
if (jsonObject.containsKey("peopleAlsoAsk")) { ... }    // 相关问题
```

```java
// PdfGeneratorTool.java - 中文字体支持
fontProvider.addSystemFonts();
document.setProperty(Property.FONT,
    new String[]{"Microsoft YaHei", "SimSun", "STSong", "Arial Unicode MS"});
```

**技术价值**：
- 展示了**多种第三方 API 和库的集成能力**
- 工具描述的质量直接影响 Agent 的决策准确性，详细的 `@Tool` 描述体现了对 LLM 工具调用机制的理解
- 跨平台命令执行展示了系统编程能力

---

## 可改进的地方

### 1. 缺少全局异常处理
- 没有 `@ControllerAdvice` 全局异常处理器，Controller 层的异常可能直接暴露堆栈信息给前端
- **建议**：添加 `GlobalExceptionHandler`，统一返回格式

### 2. Agent 线程安全问题
- `AIController.chatAgent()` 每次请求都 `new AIAgent(...)`，虽然避免了并发问题，但 `AIAgent` 标注了 `@Component`（单例），如果未来有人直接注入使用，`BaseAgent` 中的可变状态（`state`、`messageList`、`currentIteration`）会导致线程安全问题
- **建议**：将 Agent 改为原型作用域（`@Scope("prototype")`），或使用无状态设计

### 3. RAG 向量库初始化被注释掉
- `AppVectorStoreConfig.databaseInitializer()` 方法上的 `@Bean` 被注释掉了，意味着 ETL 管线不会自动执行
- **建议**：添加一个手动触发的 REST 端点，或使用条件注解控制是否在启动时加载

### 4. TerminalOperationTool 安全风险
- 允许 Agent 执行任意 Shell 命令，没有命令白名单或沙箱隔离
- **建议**：添加命令白名单过滤，或在 Docker 容器内执行命令

### 5. 缺少接口参数校验
- Controller 方法的 `message` 和 `chatId` 参数没有 `@RequestParam(required = true)` 或 `@Valid` 校验
- **建议**：添加参数校验和统一的错误返回格式

### 6. 前端 SSE 解析可以复用
- `ChatView.vue` 和 `AgentView.vue` 的 SSE 解析逻辑几乎完全相同
- **建议**：抽取为可复用的 composable 函数

### 7. 缺少限流和认证
- 所有接口没有认证和限流机制，LLM 调用成本较高
- **建议**：添加 API Key 认证或 JWT 认证，以及请求频率限制

### 8. 嵌入模型和聊天模型使用不同的 base-url
- 聊天模型使用 SiliconFlow 的 DeepSeek V3.2，嵌入模型使用本地 Ollama 的 qwen3-embedding
- 这是合理的设计（嵌入模型本地化降低成本），但文档中没有说明

### 9. 日志配置可以更细致
- `logging.level.org.springframework.ai: DEBUG` 在生产环境下可能产生过多日志
- **建议**：为不同环境（dev/prod）配置不同的日志级别
