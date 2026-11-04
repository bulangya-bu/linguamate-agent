# 语伴智学（LinguaMate）：Spring AI、大模型 API 与 RAG 说明

## 接入方式

项目通过 Spring Boot 组织后端模块，并在 `pom.xml` 中引入 Spring AI BOM、`spring-ai-client-chat` 和 `spring-ai-model`，用于满足课程技术栈要求和后续扩展。

当前运行时的大模型调用采用 OpenAI-compatible REST 接口：

- 学习者配置入口：侧边栏左下角“模型配置”；管理员在管理后台“模型连接”中独立设置。
- 配置项：`baseUrl`、`apiKey`、`chatModel`。
- 模型列表：调用 `/models` 获取，前端以下拉框展示。
- 对话、主题情境、词汇解析与作文：使用当前账号的模型配置，未设置的字段回退到部署环境默认值。
- 连接测试：`POST /api/ai/test` 对当前表单执行真实 chat-completion 请求；获取模型列表成功不代表聊天可用，测试本身不保存配置。

这样做的原因是演示环境可能使用 OpenAI、通义、智谱、Ollama、LM Studio 等兼容 OpenAI 协议的服务。自定义 `OpenAiCompatibleClient` 可以让前端动态切换 URL 和模型名，不需要重启服务。

模型 URL、密钥和模型名按账号保存，管理员的修改不会覆盖学习者的设置。公开仓库、截图和演示材料不包含真实密钥。接入服务需要兼容相应 API；客户端保留 5 秒连接超时与 45 秒读取超时，失败时页面明确标注本地降级来源。

## 对话与学习记录

- 对话练习保留每轮原句、修改说明、短语与下一轮挑战，历史会话可重新打开对应反馈。
- 学习助手把当前问题、最近会话、今日学习进度及最多 12 个优先巩固词作为回答依据。累计错误、到期时间和掌握状态分别提供，不把最近复习时间解释成最近答错时间。
- 两套精编主题“校园节水讲解”“迎新校园向导”把角色、材料、目标词和已查看提示带入情境反馈，详见 [主题任务说明](DAILY_MISSIONS.md)。当前素材不是每日自动生成。
- 阅读讨论保存文章引用，后续追问仍能使用同一会话中的文章上下文。
- 处理记录展示系统实际执行的 Observe、Think、Act、Answer 步骤，不等同于模型内部推理链，也不保证每一轮都会命中知识片段。

## RAG 流程

当前 RAG 是轻量级关键词检索版本，适合课程大作业演示：

1. 知识文档写入 `knowledge_document`。
2. 文档由 `DocumentChunkService` 切分为 `knowledge_chunk`。
3. `RetrievalService` 根据用户问题或意图关键词检索片段。
4. `AgentOrchestrator` 在主 Agent Loop 中调用 `RetrievalService`。
5. `AgentPromptFactory` 将召回片段注入系统提示词。
6. `WebAnswerService` 调用当前配置的大模型生成回答。
7. 如果模型不可用，Agent 明确显示本地兜底来源，并保留 trace；未命中知识不会显示虚构来源。

当前没有启用向量数据库或语义相似度召回。配置中的 `embedding-model` 及 `embedding_ref` 字段属于扩展预留，不代表运行时已经生成或使用向量。

## Agent Loop 对应关系

| 阶段 | 项目实现 | 说明 |
| --- | --- | --- |
| Observe | `AgentTraceRecorder.start` | 记录用户输入 |
| Think | `AgentOrchestrator.decideIntent` | 判断单词、阅读、作文、对话等意图 |
| Act | `RetrievalService`、`DictionaryTool`、`NewsRecommendTool`、作文评分工具 | 调用知识库和业务工具 |
| Answer | `WebAnswerService` 或本地兜底 | 生成最终回复 |

## 知识库设计

当前种子知识库包含：

- `English Writing Rubric`：作文评分维度、证据、衔接词。
- `Common Grammar Errors`：主谓一致、时态、冠词、介词、代词指代等常见错误。

这些内容支持作文批改、语法解释、对话练习中的 RAG 召回。

## 可扩展方向

如果需要进一步提高 RAG 效果，可以把 `RetrievalService` 替换为向量检索：

- 使用 Spring AI EmbeddingModel 生成向量。
- 将 `knowledge_chunk.embedding_ref` 替换为真实向量索引 ID。
- 引入向量数据库，如 Redis Vector、Milvus、Qdrant 或 PGVector。
- 保留 `RetrievalService.search(query, limit)` 接口不变，Agent 主流程不用大改。

本版本选择关键词检索，是为了保证演示稳定、部署简单，并且能清楚解释 RAG 在 Agent 中的位置。
