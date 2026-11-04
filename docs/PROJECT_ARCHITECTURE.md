# 语伴智学（LinguaMate）项目架构设计

当前版本采用 Vue 3 / Vite 桌面网页前端与 Spring Boot 后端，包含两个精编主题任务、个人模型配置和可回看的成长证据。本文保留课程设计中的测试样例与实施规划；其中“规划”“优化方向”不代表已完成能力，具体校验结果见 [测试报告](TEST_REPORT.md)。

## 1. 项目题目

项目名称：语伴智学（LinguaMate），基于 AI Agent 的个性化英语学习平台。

具体场景：用户正在准备英语考试或提升日常英语能力，希望在一个系统里完成英语对话练习、单词背诵、错词复习、作文批改润色和英语新闻精读。Agent 会根据用户的学习记录、错词情况、作文问题和阅读偏好，给出个性化学习建议，并在需要时调用工具完成评分、词典查询、复习计划生成和新闻推荐。

项目目标：完成一个能演示、能解释、能评测的小型英语学习 Agent，覆盖 Spring Boot、MyBatis-Plus、Spring AI、RAG、大模型 API 接入和 skills/工具调用能力。

核心能力：

- 英语对话：用户选择日常、考试、商务等场景，Agent 用英文对话并给出中文解释和改进建议。
- 单词记忆：根据词库生成背词任务，支持例句、音标、中文释义、熟悉度标记。
- 主题任务：校园节水讲解与迎新校园向导，连接阅读、三个主题词、三轮情境表达与个人复盘，保存提示使用和再次表达。
- 温故知新：今日到期按 `next_review_at` 安排，错词强化保留主动巩固入口，二者不混用。
- 作文批改与润色：分析语法、词汇、结构、逻辑和表达，输出评分与润色版本。
- 英语新闻阅读：推荐适合用户水平的英语新闻，阅读时鼠标悬停单词显示中文释义和音标。
- 成长档案：展示实际练词、阅读、写作和登录记录，成长树连接原始作答与改写证据；统计不等同于学习效果证明。

## 2. 与大作业要求的对应关系

技术栈：

- Spring Boot：后端 API、页面路由、配置管理、静态资源托管。
- MyBatis-Plus：用户学习记录、单词、错词、作文、新闻、会话等数据的 CRUD。
- Spring AI：保留相关依赖与扩展接口；当前在线调用通过自定义 `OpenAiCompatibleClient` 完成，未启用向量 Embedding 检索。
- RAG：对已导入的知识文档切块后按关键词检索，词库与阅读内容另由业务表查询。
- 大模型 API 接入：通过配置项接入兼容 OpenAI 风格的大模型接口。
- Skills/工具：词典查询工具、作文评分工具、复习计划工具、新闻推荐工具。

图片要求拆解：

| 阶段 | 项目中对应任务 |
| --- | --- |
| Backlog：选题与用户故事 | 明确语伴智学（LinguaMate）场景、定义用户故事、拆出英语学习 10 个典型任务 |
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

- Dashboard（`/`）：学习概览，选择今日主题、续做任务，并查看当前账号学习入口与记录。
- Mission（`/mission`）：校园节水讲解、迎新校园向导两套原创虚构教学情境，包含阅读、词语、表达和复盘。
- Chat（`/chat`）：对话练习与学习助手，支持日常、考试、商务和阅读讨论，展示逐轮反馈及实际处理轨迹。
- Vocabulary（`/vocabulary`）：单词练习，作答后显示完整词性释义，可收藏并按需展开深度解析。
- Review（`/review`）：温故知新，区分今日到期和错词强化，通过英文拼写与中文释义主动回忆。
- Writing（`/writing`）：写作工坊，支持四维参考评分、逐句批注、润色对照与学习者再次提交。
- News（`/news`）：阅读空间，支持难度/主题筛选、即时查词、句子聚焦、全文翻译和携带文章继续对话。
- Wordbank（`/wordbank`）：我的词库，统一管理全部词、待复习、错词、收藏词和已掌握状态。
- Report（`/report`）：学习档案，展示个人统计、学习日历、手绘成长树和可回看的成长证据。
- Admin（`/admin`）：管理后台，维护用户、词库、知识文档及阅读内容，模型配置与学习者账号分开。

页面间关系：

```text
Dashboard
  -> Mission
  -> Chat
  -> Vocabulary
  -> Review
  -> Writing
  -> News
  -> Wordbank
  -> Report
```

## 4. 后端分层架构

以下为核心分层示意，省略部分实体、DTO 与服务。当前源码还包含 `auth/`、`admin/`、`dictionary/` 和 `mission/`，分别处理账号与权限、后台管理、词典和主题任务。前端源码位于 `frontend/src`，生产构建位于 `src/main/resources/static`。

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

典型示例：作文批改。写作由 `WritingService`、`EssayReviewAiService` 组织，保留自身处理轨迹，不要求每次都经过通用对话意图路由。

1. Observe：用户提交作文和目标类型，例如 CET-4、IELTS、考研英语。
2. Think：判断需要作文评分标准、语法错误库和润色能力。
3. 调用当前账号配置的模型生成结构化批改；在线不可用时使用本地规则并标明来源。
4. Answer：输出参考总分、分项评分、问题列表、逐句建议和润色版本，学习者可主动改写并再次提交。

典型示例：新闻阅读悬停查词。

1. Observe：用户鼠标悬停在新闻单词上。
2. Think：判断需要查询词典和用户词汇掌握记录。
3. Act：查询词典；需要时由学习者主动加入待复习。
4. Answer：返回中文释义、音标、词性和例句。来源句、文章与词形随个人记录保存。

## 6. RAG 知识库设计

知识库名称：English Learning Knowledge Base。

可维护的数据源：

- CET-4 / CET-6 / IELTS 高频词表。
- 单词音标、中文释义、词性与例句；词库查询不等同于知识文档 RAG。
- 常见语法错误库，例如时态、主谓一致、冠词、介词、从句。
- 作文评分标准，例如内容完整性、组织结构、词汇丰富度、语法准确性。
- 英语新闻材料，包含标题、正文、难度等级、主题标签和来源。
- 对话场景库，例如日常交流、旅游、面试、课堂讨论、商务邮件。

RAG 流程：

1. 管理员或系统导入词库、评分标准、语法规则、新闻文本。
2. DocumentChunkService 将长文本切分为 chunk。
3. 知识片段保存正文及关键词，当前不生成向量。
4. RetrievalService 根据用户问题和关键词召回 Top K 知识片段，未命中时如实记录。
5. AgentPromptFactory 将召回片段拼入 Prompt。
6. 在线模型结合召回片段、会话上下文和当前用户记录生成回答；不可用时明确标注本地降级来源。

当前实现与后续扩展边界：

- 单词与新闻使用普通数据库表直接查询。
- 评分标准、语法错误、对话场景先放入 knowledge_document / knowledge_chunk。
- 当前使用关键词检索；向量化属于后续扩展，可以在保持 `RetrievalService.search(query, limit)` 契约的前提下替换内部实现。
- 模型 URL、密钥和模型名按账号保存，管理员与学习者分别配置；模型列表获取成功不等于对话连接测试通过。

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

以下列出核心表的字段概览，完整定义以 `schema.sql` 和 `DatabaseMigration` 为准。用户数据通过 `user_id` 隔离；主题快照与作答保存在 `learning_mission_session`，登录日保存在 `user_login_day`。

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
- embedding_ref：为向量扩展保留的引用字段，当前关键词检索不使用实际向量索引。
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
- GET `/review`：到期复习与错词强化页。
- GET `/writing`：作文批改页。
- GET `/news`：新闻阅读页。
- GET `/wordbank`：个人词库页。
- GET `/mission`：每日主题任务页。
- GET `/report`：学习档案页。
- GET `/login`、`/register`、`/admin`：登录、注册与管理员入口。

业务接口：

- POST `/api/chat`：发送英语对话消息。
- GET `/api/conversations`：查询会话列表。
- GET `/api/conversations/{id}/messages`：查询会话消息。
- GET `/api/words/today`：获取今日单词。
- POST `/api/words/{id}/quiz`：提交单词测验结果。
- GET `/api/reviews/today`：获取已到期复习队列。
- GET `/api/reviews/mistakes`：获取错词强化队列。
- POST `/api/reviews/{wordId}`：提交复习结果。
- POST `/api/writing/review`：提交作文批改与润色。
- GET `/api/writing/history`：查看作文历史。
- GET `/api/news/recommendations`：获取推荐新闻。
- GET `/api/news/{id}`：获取新闻详情。
- GET `/api/dictionary/lookup?word=`：查询单词音标和中文释义。
- GET `/api/news/{id}/lookup?word=`：在文章阅读中查词并记录查词行为。
- POST `/api/news/{id}/explain`、`/save-word`：请求语境解释、保存来源句与待复习词。
- POST `/api/knowledge/search`：RAG 知识库检索。
- GET `/api/report/summary`：获取学习报告。
- GET `/api/report/journal`、`/api/report/growth`：个人学习档案与成长证据。
- GET `/api/missions/today`、POST `/api/missions/{key}/start`：获取主题与开启/续做任务，其余作答接口见 [主题任务说明](DAILY_MISSIONS.md)。
- GET/PUT `/api/ai/settings`、POST `/api/ai/test`：当前账号模型配置与真实对话连接测试。

## 10. 前端视觉与交互架构

前端采用 Vue 3、Vite 和 Element Plus，源码位于 `frontend/src`。`npm run build` 输出到 `src/main/resources/static`，由 Spring Boot 托管。当前设计与校验聚焦电脑网页端。

视觉语言：保留草绿和浅蓝，结合纸感表面、分层排版与手绘插画。登录/注册、学生端、管理端、浏览器标签和对话署名统一显示“语伴智学（LinguaMate）”；品牌常量与共享字标分别位于 `frontend/src/brand.js` 和 `components/AppBrand.vue`。

页面组织：

- 左侧导航承载学习概览、对话、单词、复习、写作、阅读、词库与学习档案，左下角放置个人模型配置。
- 顶部栏展示当前模块、昼夜切换及账号菜单，用户等级与经验进度随账号信息呈现。
- 首页以今日主题和四阶段学习路线为中心，展示当前账号的实际完成状态。
- 对话页保留对话与逐轮教练反馈，学习助手提供基于个人记录的建议；处理记录呈现实际执行步骤。
- 单词卡答题后显示完整词性释义，深度解析默认收起，按需在右侧竖向展开。
- 阅读正文支持即时查词、句子聚焦与全文翻译，文章可携带到对话中继续讨论。
- 写作工坊用逐句批注和润色对照帮助理解修改，学习者主动提交第二稿。
- 学习档案的图表、日历和成长树汇总实际记录，节点可继续打开原始证据。

阅读查词交互：

```text
悬停或点选正文单词
  -> /api/news/{articleId}/lookup?word=example
  -> 显示词典释义与所在句
  -> 按需请求 /api/news/{articleId}/explain 获取模型语境解释
  -> 点击加入待复习，保存词形、来源句和文章引用
```

动画与可用性：

- 首页书本点击翻页，随机显示本地双语词语；这些发现操作不计为词汇测验。
- 对话、写作和阅读插画具有各自的循环动效及点击反馈，浮词悬停显示中文。
- 昼夜模式沿用同一套布局。隐藏页面、减少动画偏好与凭据输入期间的相应动画暂停机制保留。
- AI 等待状态显示实际耗时，不用模拟完成百分比；失败时保留草稿并显示真实来源或错误。
- 当前不宣称已完成手机/平板端专项适配或跨设备验收。

## 11. 10 个典型任务

1. 用户选择 daily conversation，Agent 用英文对话并给出中文解释。
2. 用户输入一句英文，Agent 纠正表达并给出更自然说法。
3. 用户开始今日背词，系统展示单词、音标、释义和例句。
4. 用户完成单词测验，系统记录正确率和熟悉度。
5. 用户进入温故知新，按到期时间复习或主动选择错词强化。
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
20. 点击加入待复习是否能保存个人单词记录及阅读来源。

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

## 15. 实施顺序（早期课程设计参考）

本节保留最初的分阶段规划，不作为当前功能完成清单。当前运行方式、主题与模型接入边界分别见 [部署说明](DEPLOYMENT.md)、[主题任务说明](DAILY_MISSIONS.md) 和 [AI 与 RAG 说明](SPRING_AI_AND_RAG_NOTES.md)。

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
- 保证页面整洁、美观、清晰；当前迭代优先验证桌面网页端。

第七阶段：测试与优化。

- 准备 20 条测试样例。
- 记录失败案例。
- 根据测试结果优化 Prompt、RAG 检索和工具描述。
