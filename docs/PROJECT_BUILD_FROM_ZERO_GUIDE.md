# LinguaMate Agent 项目从 0 到 1 构建讲解

这份文档用于帮助你从小白视角理解整个项目，并准备面试或课程答辩。它重点回答这些问题：

- 这个项目是做什么的？
- 为什么选择这些技术？
- 项目是如何一步一步搭起来的？
- 前端、后端、数据库、大模型、RAG、Agent 分别怎么实现？
- 面试官问“你这个项目怎么做的”时应该怎么讲？
- 项目有哪些亮点？
- 哪些关键代码值得重点解释？

项目名称：LinguaMate Agent  
项目定位：一个面向英语学习场景的智能学习助手。

它支持：

- 登录、注册、用户权限
- 英语对话练习
- 单词学习和词库查询
- 错词复习
- 作文批改和润色
- 英语新闻阅读
- 阅读时悬停查词
- 学习报告统计
- 管理员后台维护用户、单词、新闻和知识库
- OpenAI-compatible 大模型接入
- 轻量 RAG 知识库检索
- 可解释 Agent Loop：Observe、Think、Act、Answer

---

## 1. 项目一句话介绍

如果面试官让你先简单介绍项目，可以这样说：

> 这是一个基于 Spring Boot 和 Vue 3 的英语学习智能助手系统。后端使用 Spring Boot 提供 REST API，MyBatis-Plus 操作 MySQL，前端使用 Vue 3、Vite 和 Element Plus 实现单页应用。项目接入了 OpenAI-compatible 大模型接口，并设计了一个可解释的 Agent Loop，可以根据用户输入判断意图，调用词典、新闻推荐、RAG 知识库、作文评分等工具，最后生成学习反馈。同时系统对大模型调用做了本地兜底，保证没有 API Key 或模型不可用时也能完成演示。

这段话里要突出四个关键词：

- Spring Boot 全栈项目
- 英语学习业务场景
- Agent + RAG + 工具调用
- 大模型不可用时仍能本地兜底

---

## 2. 从 0 开始构建项目的总路线

从零开始做这个项目，可以分成 12 步：

1. 明确业务需求：做一个英语学习助手。
2. 设计技术架构：Spring Boot 后端、Vue 前端、MySQL 数据库。
3. 初始化 Spring Boot 项目。
4. 添加 Maven 依赖。
5. 设计数据库表。
6. 搭建通用响应、异常处理、MyBatis-Plus 配置。
7. 实现登录注册和权限拦截。
8. 实现核心学习模块：聊天、单词、复习、写作、新闻、报告。
9. 实现 Agent Loop，把普通业务服务组织成智能助手。
10. 接入大模型和 RAG 知识库。
11. 实现 Vue 前端页面和 API 调用。
12. 写测试、支持 Docker 部署、整理文档。

下面按这个顺序详细讲。

---

## 3. 第一步：确定项目需求

项目不是一开始就写代码，而是先确定“要解决什么问题”。

这个项目的用户是正在学习英语的人。用户可能有这些需求：

- 想练英语对话，但缺少陪练。
- 想背单词，但需要词义、音标、例句和复习计划。
- 想复习错词，但不知道哪些词该优先复习。
- 想批改作文，但需要语法、词汇、结构、逻辑多维度反馈。
- 想读英语新闻，但遇到生词需要快速查词。
- 想知道自己最近学习情况，比如背了多少词、写了几篇作文、读了几篇文章。

所以项目设计了这些模块：

| 模块 | 解决的问题 |
| --- | --- |
| Auth 登录注册 | 区分不同用户，保护数据 |
| Chat 对话 | 英语陪练和学习问答 |
| Vocabulary 单词 | 背单词、查词、收藏、掌握度 |
| Review 复习 | 错词复习、严格判定 |
| Writing 写作 | 作文评分、反馈、润色 |
| News 新闻 | 英语阅读、主题推荐、进度记录 |
| Dictionary 词典 | 悬停查词、保存生词 |
| Knowledge 知识库 | 给 Agent 提供 RAG 上下文 |
| Report 报告 | 汇总学习数据 |
| Admin 管理后台 | 管理用户、词库、新闻、知识库 |

面试时可以说：

> 我先把英语学习拆成几个核心场景：对话、单词、复习、写作、阅读、报告。然后每个场景对应一个后端业务包和一组数据库表，最后通过 Agent 把它们串起来，让系统看起来像一个智能学习助手，而不是一堆孤立功能。

---

## 4. 第二步：技术选型

### 4.1 后端为什么用 Spring Boot

Spring Boot 适合这个项目，因为它能快速开发 Web 服务，生态成熟，配置简单。

项目里 Spring Boot 主要负责：

- 提供 REST API
- 管理 Controller、Service、Mapper 等 Bean
- 读取配置文件
- 托管前端静态资源
- 配置登录拦截器
- 统一异常处理
- 运行集成测试

相关入口：

```text
src/main/java/com/example/javabigwork/JavaBigWorkApplication.java
```

### 4.2 为什么用 MyBatis-Plus

项目有很多表，例如用户表、单词表、消息表、作文表、新闻表、知识库表。MyBatis-Plus 可以减少重复 CRUD 代码。

项目中常见写法：

```java
wordMapper.selectList(Wrappers.<Word>query()
        .eq("level_tag", category)
        .last("ORDER BY RAND() LIMIT " + safeLimit));
```

这个写法表示：

- 查 `word` 表
- 条件是 `level_tag = category`
- 随机取一定数量的单词

### 4.3 为什么用 MySQL

这个项目的数据是结构化的，例如：

- 用户
- 会话
- 消息
- 单词
- 复习记录
- 作文
- 新闻
- 知识库文档

这些都适合用关系型数据库保存。

数据库表定义在：

```text
src/main/resources/schema.sql
```

种子数据在：

```text
src/main/resources/data.sql
```

### 4.4 为什么前端用 Vue 3 + Vite + Element Plus

Vue 3 适合快速实现单页应用，Vite 启动快，Element Plus 提供表单、按钮、卡片、表格等常用组件。

前端工程在：

```text
frontend/
```

构建后输出到：

```text
src/main/resources/static/
```

这样最终可以由 Spring Boot 一个服务同时提供前端页面和后端 API。

### 4.5 为什么接入 OpenAI-compatible API

项目没有写死某一个模型厂商，而是使用 OpenAI-compatible 格式。

好处：

- 可以接 OpenAI。
- 可以接其他兼容 OpenAI 协议的平台。
- 可以接本地模型服务。
- 前端可以动态配置 baseUrl、apiKey、model。

核心代码：

```text
src/main/java/com/example/javabigwork/ai/service/OpenAiCompatibleClient.java
```

---

## 5. 第三步：初始化项目结构

项目主目录结构如下：

```text
java-big-work
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── frontend/
│   ├── src/
│   ├── package.json
│   └── vite.config.js
├── src/
│   ├── main/
│   │   ├── java/com/example/javabigwork/
│   │   └── resources/
│   └── test/
└── docs/
```

后端包结构如下：

```text
com.example.javabigwork
├── admin
├── agent
├── ai
├── auth
├── chat
├── common
├── config
├── dictionary
├── knowledge
├── news
├── report
├── tool
├── ui
├── vocabulary
└── writing
```

这种包结构的好处是：

- 每个业务模块边界清楚。
- Controller、Service、Entity、Mapper 分层明显。
- 面试讲项目时容易按模块介绍。
- 后续扩展功能不容易混乱。

---

## 6. 第四步：配置 Maven 依赖

主要依赖在 `pom.xml`。

核心依赖包括：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>

<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>${mybatis-plus.version}</version>
</dependency>

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

含义：

- `spring-boot-starter-web`：做 Web 接口。
- `spring-boot-starter-validation`：校验请求参数。
- `spring-boot-starter-jdbc`：数据库基础能力。
- `mybatis-plus-spring-boot3-starter`：简化数据库 CRUD。
- `mysql-connector-j`：连接 MySQL。

项目还引入了 Spring AI 相关依赖：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-client-chat</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-model</artifactId>
</dependency>
```

这里更多是为了保留 Spring AI 技术栈扩展能力。当前实际大模型调用主要通过自定义的 `OpenAiCompatibleClient` 完成。

前端构建也配置在 Maven profile 中：

```xml
<profile>
    <id>frontend-build</id>
    ...
</profile>
```

所以打包时可以执行：

```powershell
.\mvnw.cmd -Pfrontend-build package
```

---

## 7. 第五步：设计数据库

数据库是项目的基础，因为大部分业务都要落库。

### 7.1 用户相关表

```sql
CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64),
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

作用：

- 保存账号
- 保存加密后的密码
- 保存角色：`USER` 或 `ADMIN`
- 保存账号状态：`ACTIVE` 或 `DISABLED`

### 7.2 对话相关表

```sql
CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL DEFAULT 1,
    scene VARCHAR(64),
    title VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT,
    role VARCHAR(32),
    content TEXT,
    metadata TEXT,
    created_at TIMESTAMP
);
```

作用：

- `conversation` 保存一次对话会话。
- `message` 保存会话中的每条消息。
- `role` 可以是 `user`、`assistant`、`system` 等。

### 7.3 AI 配置表

```sql
CREATE TABLE IF NOT EXISTS ai_setting (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL DEFAULT 1,
    provider VARCHAR(64),
    base_url VARCHAR(255),
    api_key TEXT,
    chat_model VARCHAR(128),
    updated_at TIMESTAMP,
    UNIQUE KEY uk_ai_setting_user (user_id)
);
```

作用：

- 保存每个用户自己的模型配置。
- 支持不同用户使用不同 API Key 或模型。

### 7.4 单词和复习表

```sql
CREATE TABLE IF NOT EXISTS word (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(80) NOT NULL UNIQUE,
    phonetic_us VARCHAR(120),
    phonetic_uk VARCHAR(120),
    meaning_cn VARCHAR(255),
    part_of_speech VARCHAR(64),
    example_en TEXT,
    example_cn TEXT,
    level_tag VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS user_word_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL DEFAULT 1,
    word_id BIGINT NOT NULL,
    familiarity INT,
    wrong_count INT,
    review_count INT,
    favorite BOOLEAN DEFAULT FALSE,
    next_review_at TIMESTAMP,
    last_review_at TIMESTAMP,
    UNIQUE KEY uk_user_word_progress_user_word (user_id, word_id)
);
```

含义：

- `word` 是公共词库。
- `user_word_progress` 是某个用户对某个单词的掌握情况。
- `familiarity` 表示熟悉度。
- `wrong_count` 表示答错次数。
- `next_review_at` 用于安排复习。

### 7.5 作文相关表

```sql
CREATE TABLE IF NOT EXISTS essay_submission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL DEFAULT 1,
    title VARCHAR(255),
    exam_type VARCHAR(64),
    original_text TEXT,
    polished_text TEXT,
    total_score DOUBLE,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS essay_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    essay_id BIGINT,
    category VARCHAR(64),
    original_sentence TEXT,
    suggestion TEXT,
    corrected_sentence TEXT
);
```

含义：

- `essay_submission` 保存作文原文、润色后文本和总分。
- `essay_feedback` 保存逐条反馈。

### 7.6 新闻阅读表

```sql
CREATE TABLE IF NOT EXISTS news_article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    summary TEXT,
    content TEXT,
    source_name VARCHAR(120),
    difficulty VARCHAR(32),
    topic_tags VARCHAR(255),
    created_at TIMESTAMP,
    UNIQUE KEY uk_news (title, source_name)
);

CREATE TABLE IF NOT EXISTS reading_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL DEFAULT 1,
    article_id BIGINT,
    read_progress INT,
    looked_up_words INT,
    finished BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE KEY uk_reading_record_user_article (user_id, article_id)
);
```

作用：

- 保存新闻文章。
- 记录用户阅读进度。
- 记录用户查词次数。
- 进度达到一定比例后认为读完。

### 7.7 RAG 知识库表

```sql
CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    source_type VARCHAR(64),
    tags VARCHAR(255),
    content TEXT,
    created_at TIMESTAMP,
    UNIQUE KEY uk_doc (title, source_type)
);

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT,
    chunk_index INT,
    content TEXT,
    keywords VARCHAR(255),
    embedding_ref VARCHAR(255),
    created_at TIMESTAMP,
    UNIQUE KEY uk_chunk (document_id, chunk_index)
);
```

含义：

- `knowledge_document` 保存完整知识文档。
- `knowledge_chunk` 保存文档切片。
- 当前版本用关键词检索。
- `embedding_ref` 预留给后续向量检索。

---

## 8. 第六步：搭建后端通用基础设施

### 8.1 统一响应格式

所有接口尽量返回统一结构：

```json
{
  "success": true,
  "message": "OK",
  "data": {}
}
```

对应类：

```text
src/main/java/com/example/javabigwork/common/ApiResponse.java
```

好处：

- 前端处理简单。
- 成功和失败格式统一。
- 面试时能体现接口规范意识。

### 8.2 全局异常处理

对应类：

```text
src/main/java/com/example/javabigwork/common/GlobalExceptionHandler.java
```

它处理：

- 未登录：401
- 无权限：403
- 参数校验失败：400
- 业务参数错误：400
- 未知异常：500

面试可以说：

> Controller 不需要每个方法都 try-catch，统一交给 `@RestControllerAdvice` 处理异常，保证错误响应格式一致。

### 8.3 MyBatis-Plus 配置

MyBatis-Plus 用于实体和数据库字段映射。

配置中开启了下划线转驼峰：

```properties
mybatis-plus.configuration.map-underscore-to-camel-case=true
```

例如数据库字段：

```text
created_at
```

可以映射到 Java 字段：

```java
createdAt
```

---

## 9. 第七步：实现登录、注册和权限

### 9.1 为什么需要登录

项目里很多数据是用户私有的：

- 对话记录
- 单词掌握情况
- 作文历史
- 阅读记录
- AI 设置

所以必须知道当前请求是谁发的。

### 9.2 登录方式

项目使用的是 Session 方案，不是 JWT。

登录成功后，后端把用户 ID 放到 Session：

```java
servletRequest.getSession(true).setAttribute(SESSION_USER_ID, user.id);
```

前端请求使用：

```javascript
fetch(url, {
  credentials: "same-origin"
});
```

这样浏览器会自动带上同源 Cookie。

### 9.3 密码加密

项目没有明文保存密码，而是用 PBKDF2 加盐哈希。

核心文件：

```text
src/main/java/com/example/javabigwork/auth/PasswordHasher.java
```

关键逻辑：

```java
byte[] salt = new byte[SALT_LENGTH];
secureRandom.nextBytes(salt);
byte[] hash = pbkdf2(password, salt, ITERATIONS);
```

含义：

- 每个用户有不同 salt。
- 同样的密码也会生成不同 hash。
- 数据库泄露时不能直接看到用户密码。

### 9.4 请求拦截

核心文件：

```text
src/main/java/com/example/javabigwork/auth/AuthInterceptor.java
```

`WebConfig` 把拦截器加到所有 API 上：

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/**");
}
```

拦截器逻辑：

1. 如果是 `/api/auth/**`，允许访问，因为登录注册本来就不需要先登录。
2. 其他 `/api/**` 必须检查 Session。
3. 如果没登录，返回 401。
4. 如果访问管理员接口但不是 ADMIN，返回 403。
5. 登录成功的用户放入 `UserContext`。
6. 请求结束后清理 ThreadLocal。

### 9.5 UserContext 的作用

核心文件：

```text
src/main/java/com/example/javabigwork/auth/UserContext.java
```

它用 ThreadLocal 保存当前请求用户：

```java
private final ThreadLocal<UserAccount> current = new ThreadLocal<>();
```

业务层通过：

```java
userContext.userId()
```

拿当前用户 ID。

这很重要，因为前端不能自己传 `userId`。如果让前端传，用户可以伪造别人的 ID。现在由后端从 Session 中取，安全性更好。

面试可以这样说：

> 我们的用户数据隔离不是靠前端传 userId，而是通过 Session 和拦截器确定当前登录用户，再由业务层统一从 UserContext 获取 userId。

---

## 10. 第八步：实现聊天模块

聊天模块涉及三个核心表：

- `conversation`
- `message`
- `ai_setting`

### 10.1 Controller 层

接口包括：

```text
POST /api/chat
GET  /api/conversations
GET  /api/conversations/{id}/messages
```

对应文件：

```text
src/main/java/com/example/javabigwork/chat/controller/ChatController.java
```

### 10.2 Service 层流程

核心文件：

```text
src/main/java/com/example/javabigwork/chat/service/ChatService.java
```

核心流程：

```java
public ChatResponse chat(ChatRequest request) {
    Conversation conversation = conversationService.getOrCreate(
            request.conversationId(),
            request.scene(),
            request.message()
    );

    List<Message> history = messageMapper.selectList(... LIMIT 12);
    Collections.reverse(history);

    saveMessage(conversation.id, "user", request.message(), null);

    var result = agent.chat(conversation.scene, request.message(), history);

    saveMessage(conversation.id, "assistant", result.reply(), "trace=" + result.trace().size());
    traceRecorder.flush(result.trace(), String.valueOf(conversation.id));
    conversationService.touch(conversation.id);
    experienceService.addXp(5);

    return new ChatResponse(conversation.id, result.reply(), result.trace(), result.suggestions());
}
```

这段代码可以按步骤解释：

1. 获取或创建会话。
2. 查询最近 12 条历史消息，给大模型上下文。
3. 保存用户消息。
4. 调用 Agent。
5. 保存助手回复。
6. 输出 Agent Trace。
7. 更新会话时间。
8. 增加经验值。
9. 返回前端需要的数据。

### 10.3 这里的亮点

聊天不是简单 `前端 -> 模型 -> 回复`。

它多做了几件事：

- 保存会话历史。
- 保留最近消息作为上下文。
- 接入 Agent Loop。
- 返回 trace 让用户看到 AI 的处理过程。
- 模型失败时仍能返回本地兜底回复。

---

## 11. 第九步：实现 Agent Loop

Agent 是本项目最重要的亮点之一。

核心文件：

```text
src/main/java/com/example/javabigwork/agent/AgentOrchestrator.java
```

### 11.1 Agent 做什么

Agent 不是一个神秘东西，可以简单理解成：

> 它是一个调度器，负责判断用户想干什么，然后决定调用哪个工具或服务。

项目里的 Agent 有四个阶段：

| 阶段 | 含义 | 项目实现 |
| --- | --- | --- |
| Observe | 观察输入 | 记录用户消息 |
| Think | 思考意图 | 判断是单词、新闻、作文还是普通对话 |
| Act | 执行动作 | 调用 RAG、词典、新闻推荐、大模型 |
| Answer | 返回答案 | 生成最终回复并返回 trace |

### 11.2 Agent 主流程

简化后的代码：

```java
public AgentResult handle(String scene, String message, List<Message> history) {
    List<AgentTraceStep> trace = traceRecorder.start(message);

    String normalized = message.toLowerCase(Locale.ROOT);
    Intent intent = decideIntent(normalized);

    traceRecorder.add(trace, "Think", "判断任务类型", intent.label);

    List<KnowledgeSearchResult> snippets =
            retrieveKnowledge(intent, message, normalized, trace);

    String reply;

    if (intent == Intent.WORD) {
        var result = dictionaryTool.lookup(extractLastWord(message));
        reply = "...";
    } else if (intent == Intent.NEWS) {
        var articles = newsRecommendTool.recommend("easy", "", 2);
        reply = "...";
    } else if (intent == Intent.ESSAY) {
        reply = "...";
    } else {
        reply = webAnswerService.answer(scene, message, history, snippets)
                .orElseGet(() -> fallbackReply);
    }

    traceRecorder.add(trace, "Answer", "生成回复", reply);
    return new AgentResult(reply, trace, memoryService.defaultSuggestions());
}
```

这段代码说明：

- Agent 先判断任务类型。
- 然后根据类型选择不同工具。
- 普通对话才调用大模型。
- 大模型不可用时用本地回复。

### 11.3 意图识别

当前版本使用关键词规则：

```java
private Intent decideIntent(String normalized) {
    if (normalized.contains("word") || normalized.contains("单词") || normalized.contains("释义")) {
        return Intent.WORD;
    }
    if (normalized.contains("news") || normalized.contains("新闻") || normalized.contains("阅读")) {
        return Intent.NEWS;
    }
    if (normalized.contains("essay") || normalized.contains("作文") || normalized.contains("润色")) {
        return Intent.ESSAY;
    }
    return Intent.CHAT;
}
```

为什么用规则？

- 课程项目演示稳定。
- 逻辑容易解释。
- 不依赖模型函数调用。
- 测试更容易。

面试可以补充：

> 后续可以把关键词规则升级为模型意图识别，或者使用 function calling，但第一版为了稳定演示采用规则判断。

### 11.4 Trace 记录

核心文件：

```text
src/main/java/com/example/javabigwork/agent/AgentTraceRecorder.java
```

它会记录：

- Observe：收到什么输入
- Think：判断为什么任务
- Act：调用了什么工具
- Answer：生成了什么回复

前端拿到 trace 后，可以展示 AI 的处理步骤。

亮点：

> 这个项目不是黑盒 AI，而是让用户能看到 Agent 的决策过程。

---

## 12. 第十步：实现大模型接入

### 12.1 用户级 AI 配置

核心文件：

```text
src/main/java/com/example/javabigwork/ai/service/AiSettingsService.java
```

用户可以配置：

- baseUrl
- apiKey
- chatModel

保存到 `ai_setting` 表。

核心逻辑：

```java
public AiRuntimeSettings resolveCurrent() {
    return currentRuntime();
}
```

业务调用模型时，不直接读配置文件，而是通过 `AiSettingsService` 获取当前用户配置。

### 12.2 OpenAI-compatible 客户端

核心文件：

```text
src/main/java/com/example/javabigwork/ai/service/OpenAiCompatibleClient.java
```

发送聊天请求：

```java
OpenAiChatCompletionResponse response = restClient(settings)
        .post()
        .uri("/chat/completions")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(new OpenAiChatCompletionRequest(settings.chatModel(), messages, false))
        .retrieve()
        .body(OpenAiChatCompletionResponse.class);
```

这段代码做了什么：

1. 根据用户配置创建 RestClient。
2. 请求 `/chat/completions`。
3. 发送模型名和消息列表。
4. 解析模型返回结果。
5. 取第一条 choice 的 message content。

### 12.3 baseUrl 兼容处理

代码里会自动补 `/v1`：

```java
private String normalizeBaseUrl(String baseUrl) {
    String trimmed = baseUrl.trim().replaceAll("/+$", "");
    if (trimmed.matches(".*?/v\\d+$")) {
        return trimmed;
    }
    if (trimmed.contains("://") && !trimmed.endsWith("/v1")) {
        return trimmed + "/v1";
    }
    return trimmed;
}
```

这样用户输入：

```text
https://api.openai.com
```

后端可以自动转成：

```text
https://api.openai.com/v1
```

### 12.4 模型失败兜底

核心文件：

```text
src/main/java/com/example/javabigwork/ai/service/WebAnswerService.java
```

关键逻辑：

```java
try {
    String reply = client.chat(settings, promptFactory.systemPrompt(scene, ragSnippets), messages);
    return Optional.of(reply);
} catch (Exception exception) {
    log.error("LLM call failed", exception);
    return Optional.empty();
}
```

它不直接抛异常，而是返回 `Optional.empty()`。

上层 Agent 收到空结果后，使用本地 fallback。

这就是项目的稳定性设计。

---

## 13. 第十一步：实现 RAG 知识库

RAG 的全称是 Retrieval-Augmented Generation，中文可以理解为：

> 先检索知识，再让模型基于检索到的内容回答。

### 13.1 为什么需要 RAG

如果只靠模型本身回答，它可能：

- 忘记项目内置规则。
- 编造知识。
- 回答不稳定。

所以项目设计了知识库，把英语作文评分规则、语法错误、学习建议等文档存起来。

### 13.2 文档上传

核心文件：

```text
src/main/java/com/example/javabigwork/knowledge/service/KnowledgeService.java
```

上传时：

1. 保存完整文档到 `knowledge_document`。
2. 调用 `DocumentChunkService` 切片。
3. 保存切片到 `knowledge_chunk`。

关键代码：

```java
List<String> chunks = chunkService.split(request.content());
for (int i = 0; i < chunks.size(); i++) {
    KnowledgeChunk chunk = new KnowledgeChunk();
    chunk.documentId = document.id;
    chunk.chunkIndex = i;
    chunk.content = chunks.get(i);
    chunk.keywords = request.tags();
    chunk.embeddingRef = "demo-keyword-index";
    chunkMapper.insert(chunk);
}
```

### 13.3 文档切块

核心文件：

```text
src/main/java/com/example/javabigwork/knowledge/service/DocumentChunkService.java
```

当前版本每 420 个字符切一块：

```java
int chunkSize = 420;
for (int start = 0; start < normalized.length(); start += chunkSize) {
    int end = Math.min(normalized.length(), start + chunkSize);
    chunks.add(normalized.substring(start, end));
}
```

这个实现简单稳定，适合演示。

### 13.4 检索知识

核心文件：

```text
src/main/java/com/example/javabigwork/knowledge/service/RetrievalService.java
```

关键代码：

```java
List<KnowledgeChunk> chunks = chunkMapper.selectList(Wrappers.<KnowledgeChunk>query()
        .like(!keyword.isBlank(), "content", keyword)
        .or(!keyword.isBlank())
        .like(!keyword.isBlank(), "keywords", keyword)
        .orderByDesc("created_at")
        .last("LIMIT " + Math.max(1, limit)));
```

意思是：

- 如果用户问题里有关键词，就在 `content` 和 `keywords` 中查。
- 按创建时间排序。
- 只取前几条。

### 13.5 注入 Prompt

核心文件：

```text
src/main/java/com/example/javabigwork/agent/AgentPromptFactory.java
```

`AgentPromptFactory` 会把检索结果拼进系统提示词：

```java
ragSnippets.stream().limit(ragOptions.maxSnippets()).forEach(snippet -> sb
        .append("- ")
        .append(snippet.title())
        .append(" [")
        .append(snippet.sourceType())
        .append("]: ")
        .append(abbreviate(snippet.content(), ragOptions.maxPromptSnippetChars()))
        .append("\n"));
```

默认配置：

```java
return new RagRuntimeOptions(3, 260);
```

含义：

- 最多注入 3 条知识片段。
- 每条最多 260 个字符。

这样可以避免 prompt 太长。

### 13.6 RAG 面试讲法

可以这样说：

> 当前项目实现的是轻量关键词 RAG。管理员上传知识文档后，系统先存完整文档，再切成 chunk。用户提问时，RetrievalService 根据关键词查找相关 chunk，再由 AgentPromptFactory 把检索结果拼进系统提示词。这样模型回答时可以参考项目自己的知识库。后续如果要升级，可以保持 RetrievalService.search 接口不变，把内部实现换成向量数据库检索。

---

## 14. 第十二步：实现单词学习模块

单词模块是项目中业务逻辑较完整的一块。

核心文件：

```text
src/main/java/com/example/javabigwork/vocabulary/service/VocabularyService.java
src/main/java/com/example/javabigwork/vocabulary/service/WordBankService.java
src/main/java/com/example/javabigwork/vocabulary/service/LocalWordBankLoader.java
```

### 14.1 本地词库导入

项目有本地词库文件：

```text
src/main/resources/word-bank/CET4_3.json
src/main/resources/word-bank/CET6_3.json
src/main/resources/word-bank/KaoYan_3.json
src/main/resources/word-bank/IELTS_3.json
```

启动时 `LocalWordBankLoader` 会监听应用启动事件：

```java
@EventListener(ApplicationStartedEvent.class)
public void loadWordBank() {
    if (allLocalBanksLoaded()) {
        enrichExistingExamples();
        return;
    }
    ...
}
```

作用：

- 第一次启动时把 JSON 词库导入数据库。
- 如果已经导入过，就跳过重复导入。
- 如果有单词缺少例句，会补充例句。

### 14.2 今日单词

接口：

```text
GET /api/words/today
```

业务逻辑：

```java
public List<WordCardView> todayWords(int limit, String category) {
    int safeLimit = Math.max(1, Math.min(80, limit));
    return onlineWordService.randomWords(safeLimit, category)
            .stream()
            .map(this::toView)
            .toList();
}
```

注意：

- limit 做了范围保护。
- 可以按词库分类过滤。
- 返回的是 `WordCardView`，不是直接返回数据库实体。

### 14.3 单词测验

接口：

```text
POST /api/words/{id}/quiz
```

核心逻辑：

```java
boolean correct = isCorrect(word, request.answer());
UserWordProgress progress = findOrCreateProgress(wordId);
progress.reviewCount = value(progress.reviewCount) + 1;

if (correct) {
    progress.familiarity = Math.min(5, value(progress.familiarity) + 1);
    progress.nextReviewAt = LocalDateTime.now().plusDays(Math.max(1, progress.familiarity));
} else {
    progress.wrongCount = value(progress.wrongCount) + 1;
    progress.familiarity = Math.max(0, value(progress.familiarity) - 1);
    progress.nextReviewAt = LocalDateTime.now().plusHours(6);
}
```

这段逻辑说明：

- 答对：熟悉度增加，下次复习时间延后。
- 答错：错误次数增加，熟悉度下降，6 小时后再复习。

### 14.4 严格复习

错词复习不是简单选择题，而是要求：

- 拼写正确
- 中文含义正确

核心逻辑：

```java
boolean spellingCorrect = isSpellingCorrect(word, request.spellingAnswer());
boolean meaningCorrect = isMeaningCorrect(word.meaningCn, request.meaningAnswer());
boolean correct = spellingCorrect && meaningCorrect;
```

只有两个都对，才算通过。

亮点：

> 这比普通“选择一个选项”的背单词更严格，更符合真实记忆训练。

---

## 15. 第十三步：实现词典工具

核心文件：

```text
src/main/java/com/example/javabigwork/tool/DictionaryTool.java
```

词典工具用于：

- Agent 查单词。
- 新闻阅读悬停查词。
- 保存单词到复习本。

### 15.1 查词

关键逻辑：

```java
public HoverWordResponse lookup(String rawWord) {
    String normalized = normalize(rawWord);
    if (!StringUtils.hasText(normalized)) {
        throw new IllegalArgumentException("Please enter a word to look up.");
    }
    Word word = findFastWord(normalized);
    if (word == null || !isLocalWord(word)) {
        return new HoverWordResponse(normalized, "", "", "词库暂未收录", "", "", false);
    }
    return toHoverResponse(word);
}
```

说明：

- 先标准化输入，只保留英文字符。
- 优先查本地词库。
- 如果没有查到，返回未收录提示。

### 15.2 词形还原

项目不是只查原词，还做了一些简单词形处理：

```java
case "becomes", "became", "becoming" -> candidates.add("become");
case "writes", "wrote", "written", "writing" -> candidates.add("write");
case "reads", "reading" -> candidates.add("read");
```

还处理常见后缀：

```java
if (normalized.endsWith("ing")) {
    String stem = normalized.substring(0, normalized.length() - 3);
    candidates.add(stem);
    candidates.add(stem + "e");
}
```

面试可以说：

> 为了提升阅读查词体验，我做了一个轻量词形还原。例如用户在新闻里点到 `becomes`，系统可以尝试查 `become`。

---

## 16. 第十四步：实现作文批改模块

核心文件：

```text
src/main/java/com/example/javabigwork/writing/service/WritingService.java
src/main/java/com/example/javabigwork/writing/service/EssayReviewAiService.java
src/main/java/com/example/javabigwork/tool/EssayScoringTool.java
```

### 16.1 作文批改流程

接口：

```text
POST /api/writing/review
```

流程：

1. 用户提交作文。
2. `WritingService` 先尝试调用 AI 批改。
3. 如果 AI 成功，使用 AI 返回结果。
4. 如果 AI 失败，调用本地 `EssayScoringTool`。
5. 保存作文提交记录。
6. 保存逐条反馈。
7. 增加经验值。
8. 返回评分、润色文本、反馈列表和 trace。

核心代码：

```java
public EssayReviewResponse review(EssayReviewRequest request) {
    EssayReviewDraft draft = essayReviewAiService.review(request)
            .orElseGet(() -> localDraft(request));

    EssaySubmission submission = persistSubmission(request, draft);
    persistFeedback(submission.id, draft.feedback());

    int xp = 20;
    if (draft.totalScore() >= 80) xp += 15;
    experienceService.addXp(xp);

    return toResponse(submission.id, draft);
}
```

这段代码体现了一个非常重要的设计：

> AI 优先，本地兜底。

### 16.2 AI 批改

`EssayReviewAiService` 要求模型返回 JSON。

系统提示词里规定返回结构：

```json
{
  "totalScore": 0,
  "grammarScore": 0,
  "vocabularyScore": 0,
  "structureScore": 0,
  "logicScore": 0,
  "summary": "Chinese summary",
  "polishedText": "revised English essay",
  "feedback": [
    {
      "category": "grammar",
      "originalSentence": "original sentence",
      "suggestion": "Chinese suggestion",
      "correctedSentence": "corrected English sentence"
    }
  ]
}
```

为什么要求 JSON？

- 后端容易解析。
- 前端容易展示。
- 数据库容易保存。
- 比自然语言大段文本更稳定。

### 16.3 本地评分兜底

核心文件：

```text
src/main/java/com/example/javabigwork/tool/EssayScoringTool.java
```

本地评分根据这些维度：

- 字数
- 是否包含常见助动词
- 是否使用高级连接词
- 是否有基本段落结构
- 是否有逻辑连接词

核心代码：

```java
double grammarScore = essay.matches(".*\\b(is|are|was|were|have|has|had)\\b.*") ? 22 : 17;
double vocabularyScore = essay.matches(".*\\b(however|therefore|moreover|significant|essential)\\b.*") ? 23 : 18;
double structureScore = essay.contains(".") && essay.length() > 120 ? 22 : 17;
double logicScore = essay.matches("(?is).*(first|second|finally|in conclusion|because|so).*") ? 22 : 18;
```

这不是为了完全替代老师，而是保证：

- 没有模型也能演示。
- 自动测试不需要真实 API Key。
- 用户不会因为模型失败拿不到结果。

---

## 17. 第十五步：实现新闻阅读模块

核心文件：

```text
src/main/java/com/example/javabigwork/news/service/NewsService.java
src/main/java/com/example/javabigwork/news/service/ReadingAssistService.java
```

### 17.1 新闻推荐

接口：

```text
GET /api/news/recommendations
```

推荐时会考虑：

- 难度
- 主题
- 用户历史阅读记录
- 用户是否读完
- 用户查词次数
- 文章时间

核心逻辑：

```java
return recommendTool.recommend(difficulty, safeTopic, 100).stream()
        .sorted((left, right) -> compareRecommendations(left, right, records, topicWeights))
        .limit(10)
        .map(article -> toView(article, records.get(article.id)))
        .toList();
```

说明：

- 先从 `NewsRecommendTool` 拿候选文章。
- 再根据用户阅读记录排序。
- 最多返回 10 条。

### 17.2 阅读进度

接口：

```text
POST /api/news/{articleId}/progress
```

关键逻辑：

```java
int nextProgress = Math.max(value(record.readProgress), requestedProgress);
record.readProgress = Math.min(100, nextProgress);
record.finished = Boolean.TRUE.equals(record.finished)
        || manuallyFinished
        || record.readProgress >= FINISHED_THRESHOLD;
```

这里有两个设计点：

- 阅读进度不会倒退。
- 达到 90% 后认为完成阅读。

### 17.3 查词次数

用户在新闻里查词，会更新 `looked_up_words`：

```java
record.lookedUpWords = value(record.lookedUpWords) + 1;
```

这个数据后面会进入学习报告，也会影响推荐权重。

---

## 18. 第十六步：实现学习报告

核心文件：

```text
src/main/java/com/example/javabigwork/report/service/ReportService.java
```

学习报告会统计：

- 已学单词数
- 错词数
- 待复习词数
- 收藏词数
- 作文数量
- 阅读完成数量
- 查词次数

示例逻辑：

```java
long learnedWords = progressMapper.selectCount(Wrappers.<UserWordProgress>query()
        .eq("user_id", userId)
        .ge("familiarity", 3));
```

这表示：

- 当前用户
- 熟悉度大于等于 3
- 算作已学会的单词

面试可以说：

> 报告模块不是单独维护统计表，而是从单词进度、作文提交、阅读记录等业务表实时聚合，保证数据一致。

---

## 19. 第十七步：实现前端

前端工程在：

```text
frontend/
```

### 19.1 Vue Router

核心文件：

```text
frontend/src/router/index.js
```

路由包括：

```javascript
{ path: "/login", component: LoginView }
{ path: "/register", component: LoginView }
{ path: "/admin", component: AdminView }
{ path: "/", component: MainAppView }
{ path: "/chat", component: MainAppView }
{ path: "/vocabulary", component: MainAppView }
{ path: "/review", component: MainAppView }
{ path: "/writing", component: MainAppView }
{ path: "/news", component: MainAppView }
```

项目的大部分用户页面都在 `MainAppView.vue` 中，根据路由 meta 切换当前 section。

### 19.2 API 封装

核心文件：

```text
frontend/src/api/client.js
frontend/src/api/modules.js
```

底层请求封装：

```javascript
export async function api(url, options = {}, config = {}) {
  const response = await fetch(url, {
    credentials: "same-origin",
    ...options,
    headers
  });
  ...
}
```

这里的 `credentials: "same-origin"` 很关键，因为后端使用 Session 登录，前端请求必须带 Cookie。

业务 API 封装示例：

```javascript
export const learningApi = {
  report: () => api("/api/report/summary"),
  conversations: () => api("/api/conversations"),
  messages: (id) => api(`/api/conversations/${id}/messages`),
  chat: (payload) => api("/api/chat", { method: "POST", body: JSON.stringify(payload) }),
  todayWords: (params) => api(`/api/words/today${query(params)}`),
  writingReview: (payload) => api("/api/writing/review", { method: "POST", body: JSON.stringify(payload) })
};
```

这样 Vue 页面不用到处写 URL，只调用 `learningApi.chat()`、`learningApi.report()` 等方法。

### 19.3 Vite 构建

核心文件：

```text
frontend/vite.config.js
```

关键配置：

```javascript
build: {
  outDir: "../src/main/resources/static",
  emptyOutDir: true
}
```

含义：

- 前端构建结果直接输出到 Spring Boot 静态资源目录。
- 生产环境只需要启动 Spring Boot。

开发环境代理：

```javascript
server: {
  port: 5173,
  proxy: {
    "/api": {
      target: "http://localhost:4040",
      changeOrigin: true
    }
  }
}
```

含义：

- 前端开发时跑在 5173。
- API 请求代理到后端 4040。

### 19.4 Spring Boot 页面转发

核心文件：

```text
src/main/java/com/example/javabigwork/ui/PageController.java
```

代码：

```java
@GetMapping({
        "/",
        "/chat",
        "/vocabulary",
        "/review",
        "/writing",
        "/news",
        "/report",
        "/wordbank",
        "/login",
        "/register",
        "/admin"
})
public String app() {
    return "forward:/index.html";
}
```

作用：

- 用户访问 `/chat`、`/news` 等路径时，后端都转发到前端 `index.html`。
- 然后由 Vue Router 接管页面。

这是单页应用常见做法。

---

## 20. 第十八步：管理员后台

管理员接口在：

```text
src/main/java/com/example/javabigwork/admin/
```

主要功能：

- 查看系统汇总
- 查看用户列表
- 查看用户学习资料
- 启用或禁用用户
- 重置用户密码
- 管理单词
- 管理新闻
- 管理知识库

权限控制：

```java
private boolean requiresAdmin(String path) {
    return path.startsWith("/api/admin/")
            || path.equals("/api/knowledge")
            || path.startsWith("/api/knowledge/");
}
```

也就是说：

- 普通用户不能访问管理员接口。
- 知识库管理也需要管理员权限。

---

## 21. 第十九步：测试策略

测试文件：

```text
src/test/java/com/example/javabigwork/JavaBigWorkApplicationTests.java
```

测试使用：

- Spring Boot Test
- MockMvc
- JSONPath 断言

测试时故意把模型地址设置成不可用：

```java
@SpringBootTest(properties = {
        "linguamate.ai.base-url=http://127.0.0.1:9/v1",
        "linguamate.ai.api-key=test-key",
        "linguamate.ai.chat-model=test-model"
})
```

为什么这样做？

> 为了验证没有真实模型服务时，项目仍然可以通过本地兜底逻辑正常工作。

测试覆盖了：

- 今日单词
- 词库分类
- 单词测验
- 严格复习
- 词典查词
- 知识库检索
- 聊天 trace
- 作文批改兜底
- 新闻推荐
- 阅读进度
- 用户数据隔离
- 管理员权限

面试可以说：

> 我没有把测试依赖外部大模型，而是故意让模型不可用，重点验证系统的 fallback 路径和核心业务逻辑，这样测试更稳定。

---

## 22. 第二十步：构建和运行项目

### 22.1 本地准备

需要：

- JDK 17
- MySQL 8
- Node.js 和 npm

### 22.2 配置数据库

默认配置在：

```text
src/main/resources/application.properties
```

关键配置：

```properties
server.port=${SERVER_PORT:4040}

spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/linguamate?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf-8}
spring.datasource.username=${DB_USER:root}
spring.datasource.password=${DB_PASSWORD:}
spring.sql.init.mode=${DB_INIT_MODE:always}
```

含义：

- 默认端口 4040。
- 默认连接本机 MySQL。
- `createDatabaseIfNotExist=true` 会尝试自动创建数据库。
- `spring.sql.init.mode=always` 会执行 `schema.sql` 和 `data.sql`。

### 22.3 启动后端

```powershell
.\mvnw.cmd spring-boot:run
```

访问：

```text
http://localhost:4040
```

### 22.4 前端开发

```powershell
cd frontend
npm install
npm run dev
```

访问：

```text
http://localhost:5173
```

### 22.5 构建前端

```powershell
cd frontend
npm run build
```

构建结果会进入：

```text
src/main/resources/static
```

### 22.6 后端测试

```powershell
.\mvnw.cmd test
```

### 22.7 打包

```powershell
.\mvnw.cmd clean package
```

如果要同时构建前端：

```powershell
.\mvnw.cmd -Pfrontend-build package
```

---

## 23. Docker 部署

项目有：

```text
Dockerfile
docker-compose.yml
```

### 23.1 Dockerfile

Dockerfile 是多阶段构建：

第一阶段：

- 使用 JDK 镜像
- 安装 Node 和 npm
- 下载 Maven 依赖
- 构建前后端
- 生成 jar

第二阶段：

- 使用更小的 JRE 镜像
- 只复制 jar
- 使用非 root 用户运行
- 暴露 4040 端口

这种方式的好处：

- 构建环境和运行环境分离。
- 最终镜像更小。
- 运行时不需要 Maven 和 Node。

### 23.2 docker-compose

`docker-compose.yml` 同时启动：

- MySQL
- 应用服务

MySQL 会加载：

```text
src/main/resources/schema.sql
src/main/resources/data.sql
```

应用连接 Docker 网络中的 MySQL：

```text
jdbc:mysql://mysql:3306/linguamate
```

---

## 24. 项目亮点总结

面试时要重点讲这些亮点。

### 24.1 可解释 Agent Loop

项目没有只做简单 AI 对话，而是设计了：

```text
Observe -> Think -> Act -> Answer
```

用户可以看到系统处理步骤。

这是项目最容易打动面试官的亮点。

### 24.2 工具调用

Agent 能调用多个工具：

- `DictionaryTool`
- `EssayScoringTool`
- `ReviewPlannerTool`
- `NewsRecommendTool`
- `RetrievalService`

说明你不只是调用大模型，而是把模型和确定性业务工具结合起来。

### 24.3 RAG 可扩展设计

当前是关键词检索，但保留了清晰接口：

```java
retrievalService.search(query, limit)
```

未来可以换成向量数据库，不影响 Agent 主流程。

### 24.4 大模型本地兜底

模型失败时系统不会崩。

聊天和写作都做了 fallback。

这体现了稳定性设计。

### 24.5 用户级 AI 配置

每个用户可以配置自己的：

- baseUrl
- apiKey
- model

比全局写死 API Key 更灵活。

### 24.6 用户数据隔离

通过 Session + AuthInterceptor + UserContext 实现。

业务层统一使用当前登录用户，不信任前端传 userId。

### 24.7 本地大词库导入

项目支持 CET4、CET6、考研、IELTS 本地 JSON 词库导入。

启动时自动检测是否已经导入，避免重复插入。

### 24.8 学习闭环完整

项目不是单一聊天工具，而是完整学习闭环：

```text
学习单词 -> 测验 -> 错词复习 -> 阅读查词 -> 作文批改 -> 学习报告
```

### 24.9 前后端一体化部署

前端构建到 Spring Boot 静态目录，最终一个 jar 就能跑完整应用。

---

## 25. 面试问题和回答模板

### 问题 1：你这个项目是做什么的？

回答：

> 这是一个英语学习智能助手系统，主要帮助用户完成英语对话、背单词、错词复习、作文批改和英语新闻阅读。技术上后端使用 Spring Boot 和 MyBatis-Plus，数据库使用 MySQL，前端使用 Vue 3 和 Element Plus。同时项目接入了 OpenAI-compatible 大模型，并设计了 Agent Loop 和轻量 RAG，让系统可以根据用户意图调用不同工具，生成可解释的学习反馈。

### 问题 2：你负责了哪些模块？

如果你要表达自己负责整体，可以说：

> 我主要负责了项目整体架构设计、后端接口开发、数据库表设计、Agent 编排、大模型接入、RAG 知识库、词汇和写作核心业务，以及前端页面和接口联调。

如果想说得更具体：

> 其中我重点做了 AgentOrchestrator，它负责判断用户意图并调用词典、新闻推荐、知识库检索和大模型；还做了写作模块的 AI 批改和本地兜底，保证模型不可用时系统仍能返回评分和反馈。

### 问题 3：Agent 是怎么实现的？

回答：

> Agent 核心在 AgentOrchestrator。它收到用户输入后先记录 Observe，再通过关键词判断意图，进入 Think 阶段。然后根据意图选择 Act，比如查单词调用 DictionaryTool，新闻请求调用 NewsRecommendTool，普通对话调用 WebAnswerService，大模型调用前还会通过 RetrievalService 检索知识库。最后生成 Answer，并把整个 trace 返回前端展示。

### 问题 4：RAG 是怎么实现的？

回答：

> 项目里的 RAG 是轻量关键词检索版本。管理员上传知识文档后，KnowledgeService 会保存文档，并用 DocumentChunkService 把长文本切成 chunk，保存到 knowledge_chunk 表。用户提问时，RetrievalService 根据关键词在 content 和 keywords 中检索相关片段，再由 AgentPromptFactory 把这些片段拼进系统提示词，让模型基于知识库回答。后续可以把 RetrievalService 内部替换成向量检索。

### 问题 5：为什么不直接用大模型？

回答：

> 因为大模型调用不稳定，也可能没有 API Key。项目把确定性逻辑放在本地，比如单词测验、错词复习、阅读进度、学习报告、作文本地评分。大模型主要负责生成自然语言反馈。如果模型不可用，系统会自动走本地兜底，保证功能能演示、测试能通过。

### 问题 6：登录权限怎么做的？

回答：

> 项目使用 Session 鉴权。用户登录成功后，后端把用户 ID 放到 Session。所有 `/api/**` 请求都会经过 AuthInterceptor，拦截器从 Session 加载用户，并放到 UserContext。业务层通过 UserContext 获取当前用户 ID，而不是相信前端传入 userId。管理员接口还会额外检查角色是否为 ADMIN。

### 问题 7：作文批改怎么做的？

回答：

> 作文批改优先调用大模型。EssayReviewAiService 会构造系统提示词，要求模型返回固定 JSON，包括总分、语法分、词汇分、结构分、逻辑分、总结、润色文本和逐句反馈。后端解析 JSON 后保存到 essay_submission 和 essay_feedback。如果模型不可用，就调用本地 EssayScoringTool，根据字数、连接词、结构和语法特征给出规则评分。

### 问题 8：单词复习怎么做的？

回答：

> 单词表保存公共词库，user_word_progress 保存每个用户对每个单词的掌握度。用户答对后 familiarity 增加，下次复习时间延后；答错后 wrong_count 增加，熟悉度降低，并安排更近的复习时间。错词复习使用严格判定，拼写和中文释义都正确才算通过。

### 问题 9：项目有哪些难点？

可以回答：

> 难点主要有三个。第一是把多个学习功能组织成统一的 Agent 流程，而不是做成零散接口。第二是大模型不稳定，所以要设计本地兜底，保证系统可测试、可演示。第三是用户数据隔离，所有学习数据都必须按当前登录用户过滤，不能依赖前端传 userId。

### 问题 10：如果让你继续优化，会怎么做？

回答：

> 我会从四个方向优化。第一，把 RAG 从关键词检索升级成向量检索，比如 PGVector 或 Milvus。第二，把 Agent 意图识别从关键词规则升级成模型分类或 function calling。第三，引入 Spring Security，让鉴权更标准。第四，加强前端端到端测试和接口权限测试，提高项目稳定性。

---

## 26. 小白必须理解的几个概念

### 26.1 Controller 是什么

Controller 负责接收前端请求。

比如：

```text
POST /api/chat
```

对应到后端某个 Controller 方法。

### 26.2 Service 是什么

Service 负责业务逻辑。

例如：

- 保存消息
- 判断答题是否正确
- 调用 Agent
- 保存作文反馈

### 26.3 Mapper 是什么

Mapper 负责操作数据库。

例如：

```java
messageMapper.insert(message);
wordMapper.selectById(wordId);
```

### 26.4 DTO 是什么

DTO 是前后端传输数据的对象。

例如：

- `ChatRequest`
- `ChatResponse`
- `EssayReviewRequest`
- `WordQuizResult`

它们通常不是数据库实体，而是接口专用数据结构。

### 26.5 Entity 是什么

Entity 对应数据库表。

例如：

- `Word` 对应 `word` 表。
- `Message` 对应 `message` 表。
- `EssaySubmission` 对应 `essay_submission` 表。

### 26.6 RAG 是什么

RAG 就是：

```text
先查资料，再让模型回答
```

不是让模型凭空回答。

### 26.7 Agent 是什么

Agent 可以理解成：

```text
会判断任务，并会调用工具的调度器
```

在本项目中，Agent 负责决定：

- 要不要查词
- 要不要查知识库
- 要不要推荐新闻
- 要不要调用模型
- 要不要本地兜底

---

## 27. 推荐你重点看的代码顺序

如果你要复习代码，建议按这个顺序看：

1. `README.md`：先了解项目整体。
2. `pom.xml`：看用了哪些依赖。
3. `schema.sql`：看数据库表。
4. `AuthInterceptor.java`：理解登录和权限。
5. `ChatService.java`：理解聊天请求流程。
6. `AgentOrchestrator.java`：理解 Agent 核心。
7. `WebAnswerService.java`：理解大模型调用和兜底。
8. `OpenAiCompatibleClient.java`：理解 API 调用。
9. `RetrievalService.java`：理解 RAG 检索。
10. `VocabularyService.java`：理解单词测验和复习。
11. `WritingService.java`：理解作文批改。
12. `NewsService.java`：理解新闻推荐和阅读记录。
13. `frontend/src/api/modules.js`：理解前端怎么调用接口。
14. `frontend/src/views/MainAppView.vue`：理解主要页面。
15. `JavaBigWorkApplicationTests.java`：理解测试覆盖。

---

## 28. 最终答辩版总结

可以用这段作为最后总结：

> LinguaMate Agent 是一个英语学习智能助手系统。它采用 Spring Boot + MyBatis-Plus + MySQL 构建后端，Vue 3 + Vite + Element Plus 构建前端。系统围绕英语学习场景实现了对话练习、单词学习、错词复习、作文批改、新闻阅读和学习报告。项目的核心亮点是设计了可解释的 Agent Loop，通过 Observe、Think、Act、Answer 组织用户请求，并结合 RAG 知识库和多个业务工具完成回答。同时，项目支持 OpenAI-compatible 大模型接入，并实现了本地兜底，保证模型不可用时系统仍能正常运行。整体上，它不是简单的 CRUD 项目，而是一个把业务系统、AI 能力、知识库和学习闭环结合起来的完整应用。

