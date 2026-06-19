# Spring AI、大模型 API 与 RAG 说明

## 接入方式

项目通过 Spring Boot 组织后端模块，并在 `pom.xml` 中引入 Spring AI BOM、`spring-ai-client-chat` 和 `spring-ai-model`，用于满足课程技术栈要求和后续扩展。

当前运行时的大模型调用采用 OpenAI-compatible REST 接口：

- 配置入口：左下角设置面板。
- 配置项：`baseUrl`、`apiKey`、`chatModel`。
- 模型列表：调用 `/models` 获取，前端以下拉框展示。
- 对话与作文：使用当前保存的模型配置。

这样做的原因是演示环境可能使用 OpenAI、通义、智谱、Ollama、LM Studio 等兼容 OpenAI 协议的服务。自定义 `OpenAiCompatibleClient` 可以让前端动态切换 URL 和模型名，不需要重启服务。

## RAG 流程

当前 RAG 是轻量级关键词检索版本，适合课程大作业演示：

1. 知识文档写入 `knowledge_document`。
2. 文档由 `DocumentChunkService` 切分为 `knowledge_chunk`。
3. `RetrievalService` 根据用户问题或意图关键词检索片段。
4. `AgentOrchestrator` 在主 Agent Loop 中调用 `RetrievalService`。
5. `AgentPromptFactory` 将召回片段注入系统提示词。
6. `WebAnswerService` 调用当前配置的大模型生成回答。
7. 如果模型不可用，Agent 返回本地兜底回答，并保留 trace。

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
