# 架构审查与重构

审查对象为本地 Spring Boot / Vue 项目。此次重构基于现有业务、账号隔离、词库和学习记录完成。

## 架构判断

后端按 auth、vocabulary、news、writing、mission 等领域组织，内部由 Controller 接收请求、Service 处理业务、Mapper 访问数据库。统一返回结构、构造注入、用户上下文和按账号配置模型的设计适合当前规模，采用模块化单体。

需要调整的部分有四项。

1. `MainAppView.vue` 接近 3,900 行，同时承担多种学习页面、接口请求、状态、焦点和跳转，功能之间依赖同一个大状态对象。`AdminView.vue` 约 1,380 行，也集中管理六组后台功能。
2. Vite 构建直接写入 Java 静态资源，Maven 和 Java 页面转发绑定了网页与后端的发布周期。
3. 本地启动依赖项目外脚本，端口、数据库、构建与进程状态缺少项目内统一入口。
4. 后端拒绝管理员权限的请求时可能残留线程用户上下文，登录缺少会话 ID 轮换，手工数据源配置阻碍标准连接池参数生效。

## 重构后的边界

```text
浏览器 127.0.0.1:5173
    Vue 页面 / 功能组件
        功能自己的组合式状态与操作
            api/client.js + api/modules.js
                /api 同源代理
                    Spring Boot 127.0.0.1:4040
                        认证与权限 → Controller → Service → Mapper → MySQL
```

- `frontend/src/views/MainAppView.vue` 协调账号、导航、汇总与跨功能操作。dashboard、chat、vocabulary、reading、settings 各自拥有组件与组合式状态。测验、复习和词库共用词汇领域状态，跨功能操作通过明确参数和事件衔接。
- `frontend/src/views/AdminView.vue` 负责后台导航和账号入口。`features/admin/` 分别管理概览、用户、词库、知识、阅读和模型配置，各组组件与状态独立。
- API 地址和 HTTP 行为集中在 `frontend/src/api/`。默认使用同源 `/api` 代理，独立域名部署时可配置 `VITE_API_BASE_URL` 与后端允许的 origin。
- 前端生成 `frontend/dist/`。Java JAR 仅包含后端资源。Docker 使用独立前端 Nginx 和后端服务。
- 后端通过精确 origin 配置支持凭据请求，登录轮换会话 ID，请求结束与权限拒绝都清理用户上下文。数据源采用 Spring Boot 标准自动配置。

## 本地运行

Windows 项目根目录的 `start.cmd` 调用 `scripts/start.ps1`，检查工具、加载私有数据库配置、准备依赖和 JAR、启动前后端并检查就绪。`stop.cmd` 根据 PID、启动时间、路径和命令标识停止本项目记录的服务。

脚本使用 `.runtime/` 保存运行状态与日志，数据库复用 `%LOCALAPPDATA%\LinguaMate` 的独立实例和 Windows 加密凭据。旧的后端网页构建产物已移至本机 `.runtime/legacy-static/` 备份。新电脑可运行 `scripts/setup-local-db.ps1` 初始化已安装的 MySQL 8，也可以显式提供自己的 `DB_*` 环境变量。

修改后端源码或前端依赖时，先 `stop.cmd` 再 `start.cmd`。页面开发通过 Vite 热更新。构建和测试分别在后端根目录、`frontend/` 内运行，两个项目可以独立发布。

## 验证

- 后端 Maven verify 的 91 项测试通过，新增测试覆盖 CORS、会话轮换、权限拒绝后的上下文与 API 边界。业务测试使用独立数据库。
- 前端 22 项测试与独立生产构建通过，JAR 检查确认只含后端资源。
- 浏览器实测登录、答题与切页进度、阅读查词转聊天、收藏筛选、复习、档案、主题任务、写作批改与改写，以及六个后台区域。
- Windows 的真实 `stop.cmd → start.cmd -NoOpen → start.cmd -NoOpen` 通过，重复启动复用进程，数据库就绪与前端代理正常。
- Docker Compose 完成配置校验，容器启动尚未实测。
