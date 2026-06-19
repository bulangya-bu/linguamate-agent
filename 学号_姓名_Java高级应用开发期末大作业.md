**广东东软学院**

**实践报告**

**课程名称 ：Java高级应用开发**

**项目名称 ： 智能体开发**

**班 级 ： 23软件2,3班**

**专 业 ： 软件工程**

**任课教师 ： 黄淳笙**

**学 号：**

**姓 名：**

广东东软学院教务部 制

**姓名** **实践报告成绩**

**评语：**

**指导教师（签名）**

**年 月 日**

说明：指导教师评分后，实验报告交院系办公室保存。

实践报告撰写要求

1.  实践报告按实践项目填写，每个学生做完项目必须撰写并提交实践报告。本科学生实践报告正文不少于2000字。
2.  实践报告内容标题由指导老师根据项目实际情况向学生做出具体规定，可参考如下内容：实践目的和任务、实践内容、结论与建议。
3.  实践报告排版要求：
4.  统一用A4（21 X 29.7cm）格式。
5.  标题用小二号黑体加粗，正文用四号宋体。行距为固定值20磅。
6.  页面上边距2.54cm，下边距2.54 cm，左边距3cm，右边距2.2cm；
7.  实践报告页码从正文页面起计算。页码字号，选用小四号粗黑体并居中。

正文

## 一、实践目的和任务

本次期末大作业以"智能体开发"为主题，设计并实现一个面向具体应用场景的智能体应用系统。需结合大语言模型、提示词工程、知识库检索、工具调用、任务编排、结果评测等内容，完成一个具有实际功能的智能体项目。具体任务如下：

（1）**设计一个智能体应用场景**，明确系统的服务对象和核心任务。本次选题为"LinguaMate 英语学习助手"，面向在校大学生和英语自学者，提供对话练习、词汇学习、作文批改、英语新闻阅读等一体化学习服务。

（2）**完成智能体系统架构设计**。系统采用分层架构，包含用户交互层、智能体调度层、大语言模型调用层、工具调用层、知识库检索层、数据存储层和日志记录模块，并明确各模块之间的数据流转关系。

（3）**实现大语言模型调用功能**。系统能够接收用户输入，将用户问题、系统提示词、历史上下文和知识库检索结果发送给大语言模型，并返回模型生成的结果。

（4）**设计提示词模板**。为不同对话场景（日常、面试、考试、旅行）设计系统提示词，明确智能体角色、任务范围、回答格式、限制条件和异常处理方式。

（5）**实现知识库检索功能**。将课程资料、写作评分标准、常见语法错误等文档作为知识来源，完成文档切分、关键词检索，并将检索结果注入模型提示词。

（6）**实现至少2个工具调用功能**。本次实现了4个工具：词典查询工具、作文评分工具、新闻推荐工具和复习规划工具。

（7）**实现智能体交互页面**。采用 Web 单页应用作为交互入口，展示用户输入、智能体回答、知识检索结果和工具调用结果。

（8）**实现运行日志与异常处理**。记录用户输入、模型响应、工具调用参数、工具返回结果、异常信息等内容，便于调试和后续分析。

（9）**完成系统测试与效果评估**。设计8个自动化集成测试用例，对智能体的回答准确性、知识检索效果、工具调用正确性进行测试。

通过本次实践，旨在培养学生综合运用 Java 高级应用开发相关技术完成 AI 应用项目的能力，掌握后端接口设计、数据处理、系统集成、日志记录和测试评估等工程化开发方法。

## 二、实践内容

### （一）场景设计与需求分析

**智能体名称**：LinguaMate（英语学习助手）

**应用场景**：LinguaMate 是一款面向英语学习者的智能体应用系统。传统的英语学习工具往往功能单一——背单词的只提供单词库，练口语的只提供对话模板，写作文的只提供批改，用户需要在多个平台之间切换，学习数据也无法打通。LinguaMate 将以上功能整合到一个平台中，借助大语言模型的自然语言理解和生成能力，为用户提供沉浸式的英语学习体验。

**目标用户**：在校大学生，尤其是正在备考 CET-4 和 CET-6 的学生。也包括有一定英语基础、希望通过日常练习保持语感的英语自学者。

**核心功能需求**：

1. **智能对话练习**：支持日常对话、面试模拟、考试辅导、旅行英语四种场景。用户可以用中文或英文与智能体交流，智能体会纠正语法错误、提供更地道的表达方式，并维持对话上下文。

2. **词汇学习系统**：每日推荐若干高频单词，每个单词配有音标（美式/英式）、中文释义、中英双语例句。用户可通过选择题测验加深记忆，系统基于间隔重复算法安排复习计划。

3. **作文批改功能**：用户提交英语作文后，系统从语法准确性、词汇丰富度、结构逻辑性、内容完整性四个维度评分（0-100分），给出详细的分段反馈，并提供润色版本供参考。

4. **英语新闻阅读**：提供分级英语新闻（简单/中等/高级），用户阅读时可一键查词，查看中文翻译，系统自动记录阅读进度。

5. **学习报告**：汇总学习数据（已学单词数、复习次数、作文篇数、阅读篇数），分析薄弱环节并给出下一步学习建议。

6. **四级/六级词库浏览**：内置 CET-4 和 CET-6 词汇库，支持按分类浏览、关键词搜索、收藏和批量学习。

> **[截图1：系统功能模块图]** 此处插入用 Draw.io 绘制的系统功能模块图，展示 LinguaMate 的七大核心模块及其关系。

### （二）智能体总体架构设计

本系统采用基于 Spring Boot 框架的分层架构，结合 Spring AI 实现大语言模型集成，使用 H2 数据库和 MyBatis-Plus 进行数据持久化。整体架构自顶向下分为7个层次：

**1. 用户交互层**
采用纯前端单页应用（SPA），使用原生 HTML5 + CSS3 + JavaScript 实现。页面包含仪表盘、对话、词汇、复习、写作、新闻、词库、设置等8个主要视图。通过 REST API 与后端通信，使用 Fetch API 发送异步请求，JSON 格式交换数据。

**2. 智能体调度层**
核心为 LinguaMateAgent 和 AgentOrchestrator 两个组件。LinguaMateAgent 是智能体的入口服务，接收用户请求后交给 AgentOrchestrator。AgentOrchestrator 负责意图识别（通过关键词匹配判断用户是想查词、练习写作、推荐新闻还是普通对话），然后路由到对应的工具或大语言模型。执行过程中记录每个步骤的追踪信息（Observe → Think → Act → Answer），保证智能体的行为可解释、可追溯。

**3. 大语言模型调用层**
OpenAiCompatibleClient 封装了对 OpenAI 兼容 API 的 REST 调用。支持通过前端页面动态配置 API 地址、API Key 和模型名称，无需修改代码即可切换不同的大语言模型服务商（如 DeepSeek、智谱AI、通义千问等）。

**4. 工具调用层**
实现为独立的 Spring Bean，包括 DictionaryTool（词典查询）、EssayScoringTool（作文评分）、NewsRecommendTool（新闻推荐）、ReviewPlannerTool（复习规划）。每个工具都有清晰的输入参数和返回结果，由 AgentOrchestrator 根据意图路由调用。

**5. 知识库检索层**
包含 KnowledgeService（知识文档管理）、DocumentChunkService（文档切分）、RetrievalService（关键词检索）。用户上传的文档被切分为 420 字符的片段，通过 SQL LIKE 匹配内容或关键词进行检索，检索结果注入到 LLM 提示词中作为参考信息。

**6. 数据存储层**
使用 H2 文件数据库（MySQL 兼容模式），通过 MyBatis-Plus 进行 ORM 映射。共设计 11 张表，涵盖对话记录（conversation、message）、词汇学习（word、user_word_progress、word_review_log）、作文批改（essay_submission、essay_feedback）、新闻阅读（news_article、reading_record）、知识库（knowledge_document、knowledge_chunk）、AI 设置（ai_setting）和用户等级（user_level）。

**7. 日志记录层**
使用 Logback 框架配置 4 个 Appender：控制台输出（Console）、全量日志文件（保留30天滚动）、错误日志文件（仅 ERROR 级别）、智能体追踪日志文件（记录完整的调用链和决策过程，保留14天滚动）。

**数据流转过程**：
```
用户在前端输入问题 → 发送 REST 请求到 Controller
→ LinguaMateAgent 接收请求 → AgentOrchestrator 分析意图
→ 如需知识检索则调用 RetrievalService → 获取相关片段
→ 如需工具调用则执行对应工具 → 获取工具结果
→ AgentPromptFactory 组装 System Prompt（含场景角色、RAG片段、历史上下文）
→ OpenAiCompatibleClient 调用 LLM API → 获取模型回答
→ 格式化为 AgentResult（含回答文本 + 追踪链 + 建议）
→ 返回前端展示 → 同时记录日志到文件和数据库
```

> **[截图2：系统架构图]** 此处插入分层架构图，标注7个层次及其之间的数据流向，建议使用 ProcessOn 或 Draw.io 绘制。

### （三）大语言模型调用实现

**使用的LLM服务**：本系统采用 OpenAI 兼容 API 协议，支持对接以下服务商：
- OpenAI（GPT-4o-mini 等）
- DeepSeek（DeepSeek-V2 等）
- 智谱AI（GLM-4 等）
- 通义千问（Qwen 系列）
- Moonshot（Kimi 系列）

用户可在前端设置页面自由切换服务商，默认配置为 `https://api.openai.com/v1`，模型为 `gpt-4o-mini`。

**API 调用方式**：通过 `OpenAiCompatibleClient` 类实现，内部使用 Spring 的 `RestClient` 发送 HTTP POST 请求到 `/v1/chat/completions` 端点。请求和响应使用 Java Record 定义数据结构，通过 Jackson 自动序列化/反序列化。

**请求参数结构**：
- `model`：模型名称，可在运行时动态指定
- `messages`：消息列表，包含 system（系统提示词）、user（用户输入）、assistant（历史模型回复）
- `temperature`：设为 0.7，在创造性和准确性之间取得平衡
- `max_tokens`：限制最大生成长度

**响应处理**：从 JSON 响应中提取 `choices[0].message.content` 作为模型生成的文本。如果 API 调用失败（网络异常、认证失败、限流等），会抛出异常并由上层统一处理，确保系统不会崩溃。

**核心代码**（简化）：
```java
// OpenAiCompatibleClient.chat() 方法
public String chat(AiRuntimeSettings settings, String systemPrompt, List<OpenAiChatMessage> messages) {
    List<OpenAiChatMessage> fullMessages = new ArrayList<>();
    fullMessages.add(new OpenAiChatMessage("system", systemPrompt));
    fullMessages.addAll(messages);
    
    var request = new OpenAiChatCompletionRequest(settings.chatModel(), fullMessages, 0.7);
    
    var response = restClient.post()
        .uri(settings.baseUrl() + "/chat/completions")
        .header("Authorization", "Bearer " + settings.apiKey())
        .body(request)
        .retrieve()
        .body(OpenAiChatCompletionResponse.class);
    
    return response != null ? response.choices().get(0).message().content() : "";
}
```

**API Key 安全处理**：API Key 通过环境变量 `LINGUAMATE_AI_API_KEY` 注入，不在代码中硬编码。配置文件设置默认值为空字符串：
```properties
linguamate.ai.api-key=${LINGUAMATE_AI_API_KEY:}
```
当 API Key 为空时，系统不会发送 Authorization 头，避免无效认证请求。用户也可在前端设置页面临时修改 API Key，更改仅保存在数据库中，不会输出到日志文件。

> **[截图3：AI设置页面]** 截取前端 Settings 页面，显示 Provider URL、API Key、Model 三个输入框的界面。
> **[截图4：OpenAiCompatibleClient核心代码]** 截取 IntelliJ IDEA 中 OpenAiCompatibleClient.java 的 chat 方法代码。

### （四）提示词设计

提示词是智能体系统的灵魂，直接决定了大语言模型的输出质量和稳定性。本系统通过 `AgentPromptFactory` 类集中管理所有提示词模板。

**提示词设计原则**：
1. **角色明确**：每个场景为智能体分配一个清晰的角色身份
2. **任务范围清晰**：规定智能体应该做什么、不应该做什么
3. **输出格式可预测**：固定回答的结构和格式
4. **异常处理规则**：遇到不确定的问题时的应对策略
5. **安全边界**：防止越权操作和敏感信息泄露

**四个场景的系统提示词**：

**（1）日常场景（daily）**：
```
你是一个友好的英语学习伙伴。你的任务是帮助用户在日常对话中练习英语。
- 用户用中文提问时，先用中文解释，再给出英文表达
- 指出用户句子中的语法错误，并给出修改建议
- 对用户的英文表达给出更地道的替换方案
- 如果用户问到英语学习以外的话题，礼貌引导回英语学习
- 不确定的知识要说明 "建议查证"，不要编造语法规则
```

**（2）面试场景（interview）**：
```
你是一个英语面试教练。你的任务是模拟英语面试场景。
- 用英语提问面试问题，要求用户用英语回答
- 面试结束后对用户的回答进行评估：语法、词汇、流利度
- 给出改进建议和参考答案
- 不要偏离面试主题
```

**（3）考试场景（exam）**：
```
你是一个英语考试辅导老师。你的任务是帮助用户准备 CET-4/CET-6 考试。
- 可以出模拟题（选词填空、阅读理解、翻译、写作）
- 批改用户答案并给出详细解析
- 解析要包含考点分析、常见错误和答题技巧
- 严格基于标准答案评分
```

**（4）旅行场景（travel）**：
```
你是一个旅行英语向导。你的任务是教用户在旅行中使用的实用英语。
- 模拟真实旅行场景（登机、入住酒店、点餐、问路等）
- 给出场景中的关键句型和词汇
- 纠正用户表达中的错误
- 补充文化差异提示
```

**RAG 知识注入**：当 `AgentOrchestrator` 从知识库检索到相关片段时，会将这些片段作为 additional context 注入到系统提示词的末尾，格式如下：
```
以下是知识库中与当前问题相关的内容，请优先参考这些信息回答：
--- 
{检索到的片段内容}
---
请基于以上内容回答，并在引用时标注来源。
```

> **[截图5：AgentPromptFactory提示词模板]** 截取 AgentPromptFactory.java 中 daily 场景的系统提示词代码。

### （五）知识库检索实现

**技术方案**：本系统实现了轻量级的 RAG（Retrieval-Augmented Generation）流程。考虑到教学演示场景的数据量规模，采用基于 SQL 关键词匹配的检索方案，无需额外部署向量数据库。

**知识来源**：
1. 英语写作评分标准（语法、词汇、结构、逻辑的评分细则）
2. 常见语法错误列表及修正说明
3. LinguaMate 系统使用指南
4. 工具调用说明（各工具的输入输出规范）
5. FAQ（常见学习问题与解答）

**文档处理流程**：

**步骤一：文档上传**
用户（或系统初始化时）通过 `POST /api/knowledge` 接口上传知识文档。每个文档包含标题、来源类型、标签列表和正文内容。初始化时系统自动插入种子知识文档。

**步骤二：文档切分**
`DocumentChunkService` 将长文档按 420 个字符切分为若干 Chunk。切分时尽量在句号、换行等自然边界处断开，避免在单词中间截断。每个 Chunk 记录其在原文档中的序号（chunk_index），以便检索时还原上下文。

**步骤三：关键词索引**
从文档的 tags 字段和内容中提取关键词，存入 keywords 字段作为检索索引。例如，一篇介绍写作评分标准的文档标签为 `writing,rubric,scoring,grammar`。

**步骤四：检索匹配**
`RetrievalService` 接收用户查询字符串，对 `knowledge_chunk` 表的 `content` 和 `keywords` 两个字段进行 LIKE 模糊匹配，按匹配度排序后返回 top N 个片段（默认为 3 个）。

**核心检索代码**（简化）：
```java
// RetrievalService.java
public List<KnowledgeSearchResult> search(String query, int limit) {
    String pattern = "%" + query + "%";
    return chunkMapper.searchByKeywordOrContent(pattern, limit);
}
```
```xml
<!-- KnowledgeChunkMapper.xml -->
<select id="searchByKeywordOrContent" resultType="...">
    SELECT * FROM knowledge_chunk
    WHERE content LIKE #{pattern} OR keywords LIKE #{pattern}
    ORDER BY LENGTH(content) ASC
    LIMIT #{limit}
</select>
```

**步骤五：RAG 注入**
检索到的片段由 `AgentOrchestrator` 注入到 LLM 的系统提示词中，并在响应追踪链（trace）中标注 "RetrievalService" 信息，使用户可以看到智能体参考了哪些知识库内容。

**配置参数**（`RagConfig.java`）：
```java
public class RagConfig {
    private int maxSnippets = 3;       // 最多检索片段数
    private int maxPromptSnippetChars = 260;  // 注入提示词的片段最大长度
}
```

> **[截图6：知识库上传接口]** 截取 Swagger/Postman 中 POST /api/knowledge 的请求与响应。
> **[截图7：RetrievalService检索代码]** 截取 RetrievalService.java 的 search 方法及对应的 Mapper XML。

### （六）工具调用实现

本系统实现了 4 个工具调用功能，远超作业要求的至少 2 个。每个工具封装为独立的 Spring Service，具有清晰的输入输出接口。

**工具一：词典查询工具（DictionaryTool）**

| 项目 | 说明 |
|------|------|
| 功能 | 查询单词的释义、音标、词性、例句，支持时态/复数归一化 |
| 输入 | word（字符串，要查询的单词） |
| 输出 | 单词信息（word, phoneticUs, phoneticUk, meaningCn, partOfSpeech, exampleEn, exampleCn） |
| 实现方式 | 先查本地 word 表，命中则直接返回；未命中则调用 dictionaryapi.dev 外部 API |

**工具二：作文评分工具（EssayScoringTool）**

| 项目 | 说明 |
|------|------|
| 功能 | 对用户提交的英语作文进行四维度评分，提供反馈和润色 |
| 输入 | essayText（作文正文）、examType（考试类型）、targetLevel（目标等级） |
| 输出 | totalScore（总分）、dimensionScores（各维度分数）、feedback（分段反馈）、polishedText（润色版本） |
| 评分维度 | Grammar（语法准确性）、Vocabulary（词汇丰富度）、Structure（结构逻辑性）、Content（内容完整性），各 0-100 分 |
| 实现方式 | 本地规则引擎：基于字数、平均句长、词汇多样性、连接词使用等指标计算分数 |

**工具三：新闻推荐工具（NewsRecommendTool）**

| 项目 | 说明 |
|------|------|
| 功能 | 按难度和主题筛选英语新闻文章 |
| 输入 | difficulty（难度：easy/medium/advanced）、topic（主题标签） |
| 输出 | 文章列表（title, summary, content, difficulty, topicTags） |
| 实现方式 | 查询 news_article 表，按 difficulty 和 topic_tags 过滤 |

**工具四：复习规划工具（ReviewPlannerTool）**

| 项目 | 说明 |
|------|------|
| 功能 | 基于间隔重复算法生成今日复习列表 |
| 输入 | 无（自动读取用户进度数据） |
| 输出 | 待复习单词列表（含当前熟悉度、错误次数、上次复习时间） |
| 实现方式 | 查询 user_word_progress 表，按 next_review_at 排序，优先复习错误次数多、到期未复习的单词 |

**智能体工具选择机制**：

`AgentOrchestrator` 的 `decideIntent()` 方法根据用户输入中的关键词判断用户意图：

```java
// 意图识别逻辑（简化）
if (message.contains("查词") || message.contains("什么意思") || ...) -> WORD
if (message.contains("作文") || message.contains("批改") || ...) -> ESSAY
if (message.contains("新闻") || message.contains("推荐") || ...) -> NEWS
if (message.contains("复习") || message.contains("计划") || ...) -> REVIEW
else -> CHAT (调用 LLM 生成回答)
```

识别意图后，Orchestrator 调用对应的工具方法，将工具返回的结构化数据传递给 LLM 生成面向用户的自然语言回答。

> **[截图8：AgentOrchestrator意图识别代码]** 截取 AgentOrchestrator.java 的 decideIntent 或 routeToTool 方法。
> **[截图9：DictionaryTool核心代码]** 截取 DictionaryTool.java 的 lookup 方法，展示本地查词 + 外部 API 调用的完整流程。

### （七）交互页面或接口实现

**前端技术选型**：本系统采用纯前端单页应用（SPA）架构，使用原生 HTML5、CSS3 和 Vanilla JavaScript 实现。无需 Node.js 或前端构建工具，所有静态资源由 Spring Boot 直接托管。

**页面设计风格**：采用毛玻璃（Glassmorphism）设计风格，主色调为蓝紫色渐变。背景包含粒子动画效果，营造沉浸式学习氛围。页面整体响应式布局，适配桌面和移动端。

**各页面功能**：

**1. 仪表盘（Dashboard）**
展示用户的学习等级（Level）和经验值进度条。汇总关键学习数据：今日学习单词数、总词汇量、收藏单词数、作文批改篇数、阅读文章篇数。下方显示"学习报告"摘要，分析用户的强项和薄弱环节，并给出下一步学习建议。

**2. 对话（Chat）**
左侧显示对话历史列表，右侧为主对话区域。用户可在输入框中输入问题，选择对话场景（日常/面试/考试/旅行）。智能体的回答展示在对话气泡中，下方以折叠面板形式展示追踪信息（Trace），包含意图识别结果、知识检索来源、工具调用参数和返回结果，使智能体的决策过程透明可见。

**3. 词汇（Vocabulary）**
以卡片形式展示今日推荐单词，每个卡片包含单词、音标、中文释义和例句。用户可点击"播放"按钮听发音（通过 Web Speech API 实现），点击"收藏"按钮将单词加入收藏夹。

**4. 复习（Review）**
包含三个子视图：今日复习（按间隔重复算法生成的待复习单词列表）、收藏单词（用户标记为收藏的单词）、已掌握（熟悉度≥3的单词）。复习采用选择题形式，系统给出中文释义，用户从四个选项中选出正确的英文单词。

**5. 写作（Writing）**
用户提交作文标题、正文、选择考试类型和目标等级后，系统进行评分。结果页面展示总分、各维度分数（雷达图展示）、分段反馈（每段标注语法问题和改进建议）、润色版本（原文和修改后文本的对比）。

**6. 新闻（News）**
按难度（简单/中等/高级）和主题（学习/科技/文化/健康等）筛选英语新闻。点击文章进入阅读模式，用户可将鼠标悬停在任意单词上查看中文释义，点击"翻译"按钮查看整段中文翻译。

**7. 词库（Word Bank）**
按分类（CET-4 / CET-6）浏览单词，支持关键词搜索和分页查看。每页显示 20 个单词，包含音标、词性和中文释义。

**REST API 接口汇总**：

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | /api/chat | 发送聊天消息 |
| GET | /api/conversations | 获取对话列表 |
| GET | /api/words/today | 获取今日单词 |
| POST | /api/writing/review | 提交作文评分 |
| GET | /api/news/recommendations | 获取新闻推荐 |
| POST | /api/knowledge | 上传知识文档 |
| GET | /api/experience | 获取用户经验值 |
| GET | /api/report/summary | 获取学习报告 |
| PUT | /api/ai/settings | 更新AI设置 |

> **[截图10：Chat对话页面]** 截取 Chat 页面，展示用户提问"如何提高英语语法"后智能体的完整回答，包含追踪信息展开状态。
> **[截图11：Writing作文批改页面]** 截取 Writing 页面，展示一篇作文的评分结果（雷达图 + 分段反馈 + 润色对比）。
> **[截图12：Vocabulary词汇页面]** 截取 Vocabulary 页面，展示今日推荐单词的卡片列表。

### （八）系统测试与结果分析

**测试方法**：本系统采用 Spring Boot MockMvc 进行集成测试，模拟 HTTP 请求验证各接口的正确性。测试用例覆盖了核心业务功能。

**自动化测试用例及结果**：

| 编号 | 测试用例名称 | 测试内容 | 预期结果 | 实际结果 |
|------|-------------|---------|---------|---------|
| 1 | contextLoads | 验证 Spring 应用上下文能否正常加载 | 启动无异常 | ✅ 通过 |
| 2 | todayWordsReturnsEightLocalFastWords | 调用 GET /api/words/today?limit=8 | 返回8个local-fast单词 | ✅ 通过 |
| 3 | dictionaryLookupUsesFastLocalLemma | 查词"becomes" | 归一化为"become"，返回中文释义"变成" | ✅ 通过 |
| 4 | knowledgeSearchReturnsSeededGrammarChunks | 搜索知识库关键词"grammar" | 返回含"Common Grammar Errors"的结果 | ✅ 通过 |
| 5 | chatTraceIncludesRetrievalForGrammarQuestion | 对话询问语法问题 | 追踪链中包含"RetrievalService" | ✅ 通过 |
| 6 | chatTraceHitsRetrievalForChineseKnowledgeBaseQuestion | 中文提问"知识库有什么" | 追踪链显示命中RAG知识库 | ✅ 通过 |
| 7 | writingReviewReturnsLocalScoresAndFeedback | 提交作文评分 | 返回总分、反馈列表和评分来源 | ✅ 通过 |
| 8 | newsRecommendationsReturnSeedArticles | 按难度和主题获取新闻 | 返回非空的文章列表 | ✅ 通过 |

**测试结果分析**：

**（1）回答准确性**：基于大语言模型 + RAG 知识库增强，对于英语学习相关问题的回答基本准确。词典查询工具能够正确归一化单词的时态和复数形式（如 "becomes" → "become"）。

**（2）知识检索效果**：关键词 LIKE 匹配对于精确词条检索准确率高，但缺乏语义理解能力。例如搜索"语法"可以正确匹配到相关文档，但搜索"如何让句子更通顺"这类自然语言描述时可能无法返回最相关的结果。

**（3）工具调用正确性**：4 个工具均能正确执行。词典工具可以命中本地词库并返回结构化数据；作文评分工具能基于规则给出合理分数；新闻推荐工具按难度和主题正确过滤文章；复习规划工具正确计算到期复习的单词。

**（4）响应速度**：本地操作（词典查词、作文评分、新闻查询）响应时间在毫秒级。LLM 调用响应时间取决于外部 API，通常在 1-5 秒之间。系统整体可在 10 秒内返回完整回答，满足日常使用需求。

**（5）异常处理能力**：系统具备完善的异常处理机制。全局异常处理器捕获未处理异常并返回友好的错误信息；各服务层在调用外部 API 失败时提供降级方案（如词典查询失败时返回空结果而不是崩溃）。

**额外测试建议**（可补充至 10 条以上）：
- 测试 9：输入空字符串或超长文本时的异常处理
- 测试 10：AI 设置不可用时的系统降级行为（API Key 为空时应返回提示而非崩溃）

> **[截图13：测试运行结果]** 截取 IntelliJ IDEA 中运行所有测试的绿色通过结果。
> **[截图14：API测试截图]** 截取 Apifox 或 Postman 中调用 POST /api/chat 的结果，显示请求体和响应体。

### （九）总结与改进

**已完成功能总结**：

1. **智能体场景设计**：成功设计并实现了 LinguaMate 英语学习助手，覆盖对话练习、词汇学习、作文批改、新闻阅读四大学习场景。

2. **系统架构设计**：采用 7 层分层架构（交互层、调度层、LLM层、工具层、知识库层、数据层、日志层），模块间解耦清晰，便于独立开发和测试。

3. **大语言模型调用**：实现了通用的 OpenAI 兼容 API 客户端，支持运行时动态切换服务商和模型，用户可在前端自由配置。

4. **提示词工程**：为 4 个对话场景分别设计了系统提示词，通过 AgentPromptFactory 统一管理，保证了 LLM 输出的稳定性和可控性。

5. **知识库检索**：实现了从文档上传、切分、索引到检索、注入的完整 RAG 流程，有效提高了智能体回答的准确性。

6. **工具调用**：实现了 4 个工具（词典查询、作文评分、新闻推荐、复习规划），远超作业要求的 2 个。每个工具封装独立、接口清晰。

7. **交互页面**：实现了包含 8 个视图的完整 SPA 前端，28 个 REST API 端点，提供了直观的用户交互体验。

8. **日志与异常**：配置了 4 层日志系统，实现了全局异常处理和局部降级机制。

9. **系统测试**：编写了 8 个自动化集成测试用例，覆盖核心业务路径。

**存在的问题**：

1. 知识库检索目前使用关键词 LIKE 匹配，缺乏语义理解能力，对自然语言查询的检索效果有限。
2. 测试用例 8 个，距离要求的 10 条还有差距，缺少异常处理和性能测试方面的专项用例。
3. 前端为纯静态页面，对话体验上缺乏流式输出效果（打字机效果）。
4. GNews 的 API Key 已改为通过环境变量 `LINGUAMATE_GNEWS_API_KEY` 注入，公开仓库中不保留真实密钥；未配置时最新新闻模块会降级为空列表。
5. 系统目前为单用户模式，缺乏多用户登录、数据隔离和权限控制。

**后续改进方向**：

1. **知识库升级为向量检索**：引入向量数据库（如 pgvector、Chroma 或 Elasticsearch），使用 Embedding 模型将文档和查询转为向量，实现语义级别的相似度检索，大幅提高 RAG 效果。

2. **增加工具数量**：计划添加语音朗读工具（调用 TTS API 朗读单词和文章）、学习计划生成工具（根据用户目标生成每日学习计划）、成绩统计工具（可视化展示学习趋势）。

3. **改进前端交互**：引入 WebSocket 实现 LLM 响应的流式输出，增加打字机动画效果；引入 Markdown 渲染支持，优化代码块和表格的展示。

4. **多用户支持**：增加登录注册功能，实现用户数据隔离。可根据用户水平自动调整学习内容的难度。

5. **评测体系自动化**：建立标准化的智能体回答评测流水线，定期使用测试集评估模型在准确性、完整性、安全性等方面的表现。

6. **多模态扩展**：支持图片输入（如截图批改手写作文）、音频输入（口语练习和发音评测），扩展智能体的感知能力。

## 三、结论与建议

### 结论

本次 Java 高级应用开发期末大作业以"智能体开发"为主题，成功设计并实现了一款面向英语学习场景的智能体应用系统——LinguaMate。系统基于 Spring Boot 3 + Spring AI + H2 + MyBatis-Plus 技术栈，构建了包含用户交互、智能体调度、大语言模型调用、工具调用、知识库检索、数据持久化和日志记录在内的完整 7 层架构。

通过本项目的实践，达到了以下学习目标：

1. **掌握了智能体应用开发的基本流程**。从场景设计、架构搭建、功能实现到测试评估，完整经历了一个智能体系统的开发生命周期。

2. **理解了 LLM + RAG + Tool Calling 的核心协同机制**。大语言模型提供语言理解和生成能力，RAG 知识库提供外部知识支持减少幻觉，工具调用赋予智能体执行具体操作的能力，三者相互配合构成了智能体的核心能力。

3. **熟悉了 Spring AI 和 Spring Boot 的工程化开发方法**。包括 REST API 设计、依赖注入、数据持久化、配置管理、异常处理和日志记录等企业级开发实践。

4. **掌握了提示词工程的实用技巧**。通过场景化系统提示词约束模型行为，通过 few-shot 示例提高输出质量，通过 RAG 注入增强回答准确性。

### 建议

**对课程教学的建议**：

1. 建议在课程中增加 Spring AI 框架的专题讲解，该框架在本次开发中被证明能显著降低大语言模型集成的复杂度。

2. 建议提供更多关于提示词工程的最佳实践案例，特别是针对不同业务场景的提示词设计模式。

3. 建议在后续课程中增加向量数据库（如 Milvus、Chroma）的使用教学，使学生能够实现更完善的 RAG 系统。

**对 LinguaMate 后续开发的建议**：

1. **工程化提升**：引入 CI/CD 流水线，实现自动化测试、构建和部署。增加代码质量检查（SonarQube）和性能监控（Prometheus + Grafana）。

2. **用户体验优化**：增加移动端适配，考虑使用 React Native 或 Flutter 开发移动 App。引入 WebSocket 实现流式对话，提升交互流畅度。

3. **AI 能力增强**：支持多模态输入（图片、音频），拓展智能体的感知范围。引入多智能体协作架构，将不同任务分配给专门的子智能体处理。

4. **安全加固**：实现用户认证和授权（JWT + Spring Security），防止数据越权。所有 API Key 和敏感配置均通过环境变量或密钥管理服务管理，杜绝硬编码。

5. **数据驱动改进**：收集用户交互数据，分析高频问题和常见错误，持续优化知识库和提示词。建立 A/B 测试机制，对比不同提示词策略的效果。

通过本次期末大作业的实践，不仅巩固了 Java 高级应用开发的各项技术，更深入理解了智能体系统的设计原理和实现方法，为今后从事 AI 应用开发奠定了坚实的基础。
