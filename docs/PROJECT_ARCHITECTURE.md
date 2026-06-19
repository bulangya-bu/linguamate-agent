# English Learning Agent 项目架构设计

## 1. 项目题目

项目名称：LinguaMate Agent，英语学习陪练与阅读助手。

具体场景：用户正在准备英语考试或提升日常英语能力，希望在一个系统里完成英语对话练习、单词背诵、错词复习、作文批改润色和英语新闻精读。Agent 会根据用户的学习记录、错词情况、作文问题和阅读偏好，给出个性化学习建议，并在需要时调用工具完成评分、词典查询、复习计划生成和新闻推荐。

项目目标：完成一个能演示、能解释、能评测的小型英语学习 Agent，覆盖 Spring Boot、MyBatis-Plus、Spring AI、RAG、大模型 API 接入和 skills/工具调用能力。

核心能力：

- 英语对话：用户选择日常、考试、商务等场景，Agent 用英文对话并给出中文解释和改进建议。
- 单词记忆：根据词库生成背词任务，支持例句、音标、中文释义、熟悉度标记。
- 错词复习：根据答错次数和掌握程度生成复习队列。
- 作文批改与润色：分析语法、词汇、结构、逻辑和表达，输出评分与润色版本。
- 英语新闻阅读：推荐适合用户水平的英语新闻，阅读时鼠标悬停单词显示中文释义和音标。

## 2. 与大作业要求的对应关系

技术栈：

- Spring Boot：后端 API、页面路由、配置管理、静态资源托管。
- MyBatis-Plus：用户学习记录、单词、错词、作文、新闻、会话等数据的 CRUD。
- Spring AI：大模型对话、Prompt 编排、Embedding、RAG 检索、工具调用。
- RAG：接入英语词库、作文评分标准、常见语法错误、新闻阅读材料和对话场景库。
- 大模型 API 接入：通过配置项接入兼容 OpenAI 风格的大模型接口。
- Skills/工具：词典查询工具、作文评分工具、复习计划工具、新闻推荐工具。

图片要求拆解：

| 阶段 | 项目中对应任务 |
| --- | --- |
| Backlog：选题与用户故事 | 明确 LinguaMate Agent 场景、定义用户故事、拆出英语学习 10 个典型任务 |
| Backlog：定义 10 个典型任务 | 对话、背词、错词复习、作文批改、作文润色、新闻推荐、悬停查词、学习计划、RAG 问答、学习报告 |
| Backlog：列出工具和数据源 | 工具：词典、评分、复习计划、新闻推荐；数据源：词库、新闻、作文标准、语法规则 |
| Building：实现最小 Agent Loop | Observe、Think、Act、Answer 四步闭环 |
| Building：接入 1 个知识库 | 接入英语学习知识库，包含词库、作文评分标准、语法错误库、新闻材料 |
| Building：接入 2 个工具调用 | 至少接入词典查询工具和作文评分工具，扩展复习计划与新闻推荐 |
| Testing：准备 20 条测试样例 | 覆盖对话、背词、错词、作文、新闻阅读和工具调用 |
| Testing：记录失败案例 | 记录误判释义、作文评分偏差、新闻难度不匹配等失败案例 |
| Testing：优化提示词和工具描述 | 根据失败案例优化 Prompt、工具参数和返回格式 |

## 3. 产品信息架构

主界面：英语学习工作台。

一级页面：

- Dashboard：学习概览，看板展示 Backlog、Learning、Review 三类学习任务。
- Chat：英语对话练习，支持场景选择、英文回复、中文解释、表达建议。
- Vocabulary：单词记忆，支持背词、测验、掌握度标记。
- Review：错词复习，根据错误次数和复习间隔生成复习队列。
- Writing：作文批改与润色，支持评分、逐句建议、润色对比。
- News：英语新闻阅读，支持新闻推荐、难度标签、单词悬停释义和音标。
- Report：学习报告，展示练习次数、错词变化、作文分数趋势和阅读记录。

页面间关系：

```text
Dashboard
  -> Chat
  -> Vocabulary
  -> Review
  -> Writing
  -> News
  -> Report
```

## 4. 后端分层架构

```text
com.example.javabigwork
├── JavaBigWorkApplication.java
├── common
│   ├── ApiResponse.java
│   ├── PageResponse.java
│   ├── ErrorCode.java
│   └── GlobalExceptionHandler.java
├── config
│   ├── AiConfig.java
│   ├── MybatisPlusConfig.java
│   ├── WebConfig.java
│   └── RagConfig.java
├── agent
│   ├── LinguaMateAgent.java
│   ├── AgentOrchestrator.java
│   ├── AgentMemoryService.java
│   ├── AgentPromptFactory.java
│   └── AgentTraceRecorder.java
├── chat
│   ├── controller
│   │   └── ChatController.java
│   ├── dto
│   │   ├── ChatRequest.java
│   │   ├── ChatResponse.java
│   │   └── ConversationView.java
│   ├── entity
│   │   ├── Conversation.java
│   │   └── Message.java
│   ├── mapper
│   │   ├── ConversationMapper.java
│   │   └── MessageMapper.java
│   └── service
│       ├── ChatService.java
│       └── ConversationService.java
├── vocabulary
│   ├── controller
│   │   └── VocabularyController.java
│   ├── dto
│   │   ├── WordCardView.java
│   │   ├── WordQuizRequest.java
│   │   └── WordQuizResult.java
│   ├── entity
│   │   ├── Word.java
│   │   ├── UserWordProgress.java
│   │   └── WordReviewLog.java
│   ├── mapper
│   │   ├── WordMapper.java
│   │   ├── UserWordProgressMapper.java
│   │   └── WordReviewLogMapper.java
│   └── service
│       ├── VocabularyService.java
│       └── ReviewScheduleService.java
├── writing
│   ├── controller
│   │   └── WritingController.java
│   ├── dto
│   │   ├── EssayReviewRequest.java
│   │   ├── EssayReviewResponse.java
│   │   └── SentenceFeedbackView.java
│   ├── entity
│   │   ├── EssaySubmission.java
│   │   └── EssayFeedback.java
│   ├── mapper
│   │   ├── EssaySubmissionMapper.java
│   │   └── EssayFeedbackMapper.java
│   └── service
│       ├── WritingService.java
│       └── EssayRubricService.java
├── news
│   ├── controller
│   │   └── NewsController.java
│   ├── dto
│   │   ├── NewsArticleView.java
│   │   ├── NewsRecommendRequest.java
│   │   └── HoverWordResponse.java
│   ├── entity
│   │   ├── NewsArticle.java
│   │   └── ReadingRecord.java
│   ├── mapper
│   │   ├── NewsArticleMapper.java
│   │   └── ReadingRecordMapper.java
│   └── service
│       ├── NewsService.java
│       └── ReadingAssistService.java
├── knowledge
│   ├── controller
│   │   └── KnowledgeController.java
│   ├── dto
│   │   ├── KnowledgeUploadRequest.java
│   │   └── KnowledgeSearchResult.java
│   ├── entity
│   │   ├── KnowledgeDocument.java
│   │   └── KnowledgeChunk.java
│   ├── mapper
│   │   ├── KnowledgeDocumentMapper.java
│   │   └── KnowledgeChunkMapper.java
│   └── service
│       ├── KnowledgeService.java
│       ├── DocumentChunkService.java
│       └── RetrievalService.java
├── tool
│   ├── AgentTools.java
│   ├── DictionaryTool.java
│   ├── EssayScoringTool.java
│   ├── ReviewPlannerTool.java
│   └── NewsRecommendTool.java
├── report
│   ├── controller
│   │   └── ReportController.java
│   ├── dto
│   │   └── LearningReportView.java
│   └── service
│       └── ReportService.java
└── ui
    └── PageController.java
```

## 5. Agent Loop 设计

最小闭环：

```text
用户输入
  -> ChatController / 业务 Controller
  -> AgentOrchestrator
  -> Observe：读取用户意图、学习记录、当前页面上下文
  -> Think：判断任务类型
      -> 英语对话
      -> 单词学习
      -> 错词复习
      -> 作文批改
      -> 新闻阅读
      -> 学习建议
  -> Act：调用 RAG 或工具
      -> DictionaryTool
      -> EssayScoringTool
      -> ReviewPlannerTool
      -> NewsRecommendTool
      -> RetrievalService
  -> Answer：返回回答、评分、释义、计划或推荐结果
  -> 保存记录并返回前端
```

典型示例：作文批改。

1. Observe：用户提交作文和目标类型，例如 CET-4、IELTS、考研英语。
2. Think：判断需要作文评分标准、语法错误库和润色能力。
3. Act：调用 RAG 检索评分标准，调用 EssayScoringTool 生成结构化评分。
4. Answer：输出总分、分项评分、问题列表、逐句建议和润色版本。

典型示例：新闻阅读悬停查词。

1. Observe：用户鼠标悬停在新闻单词上。
2. Think：判断需要查询词典和用户词汇掌握记录。
3. Act：调用 DictionaryTool，必要时保存为生词。
4. Answer：返回中文释义、音标、词性、例句和是否已加入错词本。

## 6. RAG 知识库设计

知识库名称：English Learning Knowledge Base。

数据源：

- CET-4 / CET-6 / IELTS 高频词表。
- 单词音标、中文释义、词性、例句、同义词。
- 常见语法错误库，例如时态、主谓一致、冠词、介词、从句。
- 作文评分标准，例如内容完整性、组织结构、词汇丰富度、语法准确性。
- 英语新闻材料，包含标题、正文、难度等级、主题标签和来源。
- 对话场景库，例如日常交流、旅游、面试、课堂讨论、商务邮件。

RAG 流程：

1. 管理员或系统导入词库、评分标准、语法规则、新闻文本。
2. DocumentChunkService 将长文本切分为 chunk。
3. Spring AI Embedding 生成向量，保存到向量存储或本地可替代字段。
4. RetrievalService 根据用户问题或作文内容召回 Top K 知识片段。
5. AgentPromptFactory 将召回片段拼入 Prompt。
6. 大模型基于检索内容输出可解释答案，并返回引用来源。

开发期简化方案：

- 单词与新闻使用普通数据库表直接查询。
- 评分标准、语法错误、对话场景先放入 knowledge_document / knowledge_chunk。
- 如果向量库配置复杂，第一版使用关键词匹配和标签筛选完成可演示版本，后续替换 RetrievalService 内部实现。

## 7. 工具调用设计

DictionaryTool：

- 输入：word。
- 输出：音标、中文释义、词性、英文例句、中文例句翻译。
- 用途：新闻悬停查词、背单词、错词复习、对话中的生词解释。

EssayScoringTool：

- 输入：essayText、examType、targetLevel。
- 输出：总分、语法分、词汇分、结构分、逻辑分、错误列表、润色版本。
- 用途：作文批改与润色。

ReviewPlannerTool：

- 输入：用户单词掌握记录、错词次数、最近复习时间。
- 输出：今日复习队列、复习优先级、推荐学习数量。
- 用途：错词复习和单词记忆。

NewsRecommendTool：

- 输入：用户水平、兴趣主题、已读记录、错词情况。
- 输出：推荐新闻列表、难度等级、推荐理由。
- 用途：英语新闻推荐与阅读训练。

## 8. 数据库表设计

conversation：

- id：会话 ID。
- scene：对话场景，例如 daily、exam、business、reading。
- title：标题。
- created_at：创建时间。
- updated_at：更新时间。

message：

- id：消息 ID。
- conversation_id：所属会话。
- role：user / assistant / system / tool。
- content：消息内容。
- metadata：工具调用、引用来源、学习建议等扩展信息。
- created_at：创建时间。

word：

- id：单词 ID。
- word：英文单词。
- phonetic_us：美式音标。
- phonetic_uk：英式音标。
- meaning_cn：中文释义。
- part_of_speech：词性。
- example_en：英文例句。
- example_cn：例句翻译。
- level_tag：CET4 / CET6 / IELTS / TOEFL。

user_word_progress：

- id：记录 ID。
- word_id：单词 ID。
- familiarity：熟悉度，0 到 5。
- wrong_count：答错次数。
- review_count：复习次数。
- next_review_at：下次复习时间。
- last_review_at：最近复习时间。

word_review_log：

- id：复习记录 ID。
- word_id：单词 ID。
- answer：用户答案。
- correct：是否正确。
- created_at：创建时间。

essay_submission：

- id：作文 ID。
- title：标题。
- exam_type：作文类型。
- original_text：原文。
- polished_text：润色后文本。
- total_score：总分。
- created_at：创建时间。

essay_feedback：

- id：反馈 ID。
- essay_id：作文 ID。
- category：grammar / vocabulary / structure / logic。
- original_sentence：原句。
- suggestion：修改建议。
- corrected_sentence：修改后句子。

news_article：

- id：新闻 ID。
- title：标题。
- summary：摘要。
- content：正文。
- source_name：来源名称。
- difficulty：easy / medium / hard。
- topic_tags：主题标签。
- created_at：创建时间。

reading_record：

- id：阅读记录 ID。
- article_id：新闻 ID。
- read_progress：阅读进度。
- looked_up_words：查词数量。
- finished：是否读完。
- created_at：创建时间。

knowledge_document：

- id：知识文档 ID。
- title：文档标题。
- source_type：wordbook / rubric / grammar / dialogue / news。
- tags：标签。
- content：原文。
- created_at：创建时间。

knowledge_chunk：

- id：片段 ID。
- document_id：所属文档。
- chunk_index：片段序号。
- content：片段内容。
- keywords：关键词。
- embedding_ref：向量引用或向量 JSON。
- created_at：创建时间。

agent_trace：

- id：轨迹 ID。
- trace_type：chat / writing / vocabulary / news。
- observe_text：观察内容。
- think_text：决策内容。
- act_text：工具调用内容。
- answer_text：最终回答。
- created_at：创建时间。

## 9. API 设计

页面接口：

- GET `/`：英语学习工作台。
- GET `/chat`：英语对话练习页。
- GET `/vocabulary`：单词记忆页。
- GET `/review`：错词复习页。
- GET `/writing`：作文批改页。
- GET `/news`：新闻阅读页。
- GET `/report`：学习报告页。

业务接口：

- POST `/api/chat`：发送英语对话消息。
- GET `/api/conversations`：查询会话列表。
- GET `/api/conversations/{id}/messages`：查询会话消息。
- GET `/api/words/today`：获取今日单词。
- POST `/api/words/{id}/quiz`：提交单词测验结果。
- GET `/api/reviews/today`：获取今日错词复习队列。
- POST `/api/reviews/{wordId}`：提交复习结果。
- POST `/api/writing/review`：提交作文批改与润色。
- GET `/api/writing/history`：查看作文历史。
- GET `/api/news/recommendations`：获取推荐新闻。
- GET `/api/news/{id}`：获取新闻详情。
- GET `/api/dictionary/lookup?word=`：查询单词音标和中文释义。
- POST `/api/knowledge/search`：RAG 知识库检索。
- GET `/api/report/summary`：获取学习报告。

## 10. 前端视觉与交互架构

视觉 thesis：清爽、专注、像一本带智能批注的英语学习手账；浅色网格背景对应作业图，深墨绿用于主要文字，青色用于学习中状态，黄色用于测试与反馈，珊瑚色用于重点提醒。

内容 plan：

- 首屏不是营销页，而是可操作的英语学习工作台。
- 中央展示今日学习任务和 Agent 对话入口。
- 右侧展示最近错词、作文评分和新闻推荐。
- 下方或独立页面展示背词、写作、阅读三个核心工作流。

交互 thesis：

- 任务卡进入时轻微上移淡入，形成清晰节奏。
- Chat / Writing 的 Agent 处理过程用 Observe、Think、Act、Answer 时间线点亮。
- 新闻阅读中单词 hover 弹出释义浮层，浮层跟随鼠标附近出现并有 120ms 淡入。

页面组件：

- 左侧导航：Dashboard、Chat、Words、Review、Writing、News、Report。
- 顶部状态栏：今日单词数、待复习错词、作文平均分、阅读篇数。
- 学习看板：Backlog、Learning、Testing 三列，贴合作业图片。
- Agent 面板：输入框、模式选择、工具调用轨迹、引用来源。
- 新闻阅读器：正文、词汇高亮、hover 释义卡、阅读进度。
- 作文批改器：左侧原文、右侧评分报告、底部润色对比。

新闻 hover 查词交互：

```text
用户鼠标移入单词
  -> 前端读取 data-word
  -> 调用 /api/dictionary/lookup?word=example
  -> 展示浮层：example /ɪɡˈzæmpəl/ 示例；例子
  -> 可点击加入错词本
```

动画要求：

- 页面进入：标题、状态条、看板列按 80ms 间隔淡入。
- 单词卡：翻卡动画展示释义，答题后使用颜色反馈正确或错误。
- 错词复习：进入复习队列时有平滑位移，完成后滑出。
- 作文批改：评分维度依次填充，句子建议逐条展开。
- 新闻阅读：hover 释义卡淡入，已查词单词出现轻量下划线。
- 工具时间线：Observe、Think、Act、Answer 逐项点亮。

响应式要求：

- 桌面端使用左侧导航 + 主工作区 + 右侧上下文面板。
- 平板端收起右侧上下文面板，工具轨迹移到底部。
- 手机端导航变为底部 tab，新闻 hover 降级为点击查词。

## 11. 10 个典型任务

1. 用户选择 daily conversation，Agent 用英文对话并给出中文解释。
2. 用户输入一句英文，Agent 纠正表达并给出更自然说法。
3. 用户开始今日背词，系统展示单词、音标、释义和例句。
4. 用户完成单词测验，系统记录正确率和熟悉度。
5. 用户进入错词复习，ReviewPlannerTool 生成今日复习队列。
6. 用户提交英语作文，EssayScoringTool 输出评分和问题清单。
7. 用户要求润色作文，Agent 输出更自然的英文版本并解释修改原因。
8. 用户请求推荐英语新闻，NewsRecommendTool 根据水平和兴趣返回文章。
9. 用户阅读新闻时悬停单词，DictionaryTool 返回中文释义和音标。
10. 用户查看学习报告，Agent 总结近期弱点并生成下一步学习建议。

## 12. 20 条测试样例规划

对话练习：

1. 日常问候对话是否能持续 3 轮。
2. 面试场景是否能提出追问。
3. 用户语法错误是否能被温和纠正。
4. 对话结束后是否能总结 3 个可改进表达。

单词记忆：

5. 今日单词接口是否返回指定数量单词。
6. 单词卡是否包含音标、中文释义、词性和例句。
7. 答错单词是否增加 wrong_count。
8. 答对单词是否提高 familiarity。

错词复习：

9. 错词是否进入复习队列。
10. 到期复习单词是否优先出现。
11. 多次答错单词是否提高复习优先级。
12. 复习完成后是否更新 next_review_at。

作文批改：

13. 作文批改是否返回总分和分项分数。
14. 语法错误是否能归类。
15. 润色版本是否保留原意。
16. 批改记录是否能保存并查询。

新闻阅读：

17. 新闻推荐是否匹配用户难度。
18. 新闻详情是否能正常展示正文。
19. hover 单词是否能返回中文释义和音标。
20. 点击加入错词本是否能保存用户单词记录。

## 13. 失败案例记录方向

- 单词释义不符合上下文，例如 bank 误判为银行而不是河岸。
- 音标缺失或返回格式不统一。
- 作文评分过高或过低，和评分标准不一致。
- 润色后改变用户原意。
- 新闻推荐难度过高，生词密度超过用户水平。
- hover 查词请求过多导致页面卡顿。
- Agent 在对话中中英混杂比例不符合用户选择。
- RAG 引用内容和回答结论不一致。

## 14. Prompt 与工具描述优化方向

系统 Prompt：

- 明确 Agent 身份是英语学习陪练，不替用户完成考试作弊。
- 输出应兼顾英文训练和中文解释。
- 批改作文时必须按评分维度输出。
- 新闻阅读时解释应简短，避免打断阅读。

工具描述：

- DictionaryTool 必须说明返回音标、中文释义、词性、例句。
- EssayScoringTool 必须说明返回结构化 JSON，包含分项评分和润色结果。
- ReviewPlannerTool 必须说明根据 wrong_count、familiarity、last_review_at 排序。
- NewsRecommendTool 必须说明根据 difficulty、topic_tags、reading_record 推荐。

## 15. 实施顺序

第一阶段：项目基础与页面骨架。

- 补充 Web、MyBatis-Plus、Spring AI、数据库依赖。
- 创建 common、config、ui 基础结构。
- 完成 Dashboard、Chat、Vocabulary、Writing、News 基础页面。

第二阶段：基础数据能力。

- 实现单词、用户单词进度、作文、新闻、会话的实体和 Mapper。
- 完成单词列表、新闻列表、作文记录的基础 CRUD。

第三阶段：Agent 最小闭环。

- 实现 AgentOrchestrator。
- 实现 Observe、Think、Act、Answer 轨迹记录。
- 完成英语对话和学习建议两个最小能力。

第四阶段：RAG 与大模型接入。

- 接入 Spring AI ChatClient。
- 接入英语学习知识库。
- 实现 RetrievalService 并把检索片段注入 Prompt。

第五阶段：工具调用。

- 实现 DictionaryTool 和 EssayScoringTool。
- 扩展 ReviewPlannerTool 和 NewsRecommendTool。
- 在前端展示工具调用时间线。

第六阶段：前端美化与动画。

- 完成清爽网格背景和三列学习看板。
- 完成单词翻卡、作文评分展开、新闻 hover 查词动画。
- 保证页面整洁、美观、清晰，并支持移动端降级交互。

第七阶段：测试与优化。

- 准备 20 条测试样例。
- 记录失败案例。
- 根据测试结果优化 Prompt、RAG 检索和工具描述。
