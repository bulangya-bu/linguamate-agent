<template>
  <div v-if="state.loading" class="admin-loading">
    <AdminSketchIcon name="overview" /><span>正在打开管理工作台…</span>
  </div>
  <div v-else-if="state.bootError" class="admin-loading" role="alert">
    <AdminSketchIcon name="refresh" /><span>{{ state.bootError }}</span
    ><button type="button" @click="retryBoot">重新加载</button>
  </div>
  <div v-else class="app-shell admin-shell">
    <aside class="side-nav" aria-label="管理导航">
      <RouterLink class="brand" to="/admin"
        ><img
          class="brand-mark"
          src="/assets/linguamate-icon.svg?v=bright"
          alt="LinguaMate 图标"
        />
        <div>
          <strong>LinguaMate<span class="brand-period">.</span></strong
          ><span>管理工作台</span>
        </div></RouterLink
      >
      <nav aria-label="后台功能">
        <span
          v-if="state.section !== 'ai'"
          class="nav-active-indicator"
          :style="{
            '--nav-index': sections.findIndex(
              (item) => item.id === state.section,
            ),
            '--nav-section-offset':
              sections.findIndex((item) => item.id === state.section) >= 3
                ? 1
                : 0,
          }"
          aria-hidden="true"
        ></span>
        <button
          v-for="(item, index) in sections.filter((item) => item.id !== 'ai')"
          :key="item.id"
          class="nav-item"
          :class="{
            active: state.section === item.id,
            'nav-group-start': index === 3,
          }"
          :aria-current="state.section === item.id ? 'page' : undefined"
          type="button"
          @click="showSection(item.id)"
        >
          <span class="nav-icon-wrap"><AdminSketchIcon :name="item.id" /></span
          ><span>{{ item.label }}</span>
        </button>
      </nav>
      <div class="sidebar-bottom">
        <button
          class="sidebar-model-config"
          :class="{ 'is-selected': state.section === 'ai' }"
          type="button"
          :aria-current="state.section === 'ai' ? 'page' : undefined"
          @click="showSection('ai')"
        >
          <span class="sidebar-model-icon"><AdminSketchIcon name="ai" /></span
          ><span class="sidebar-model-copy"
            ><strong>模型连接</strong><small>管理员独立配置</small></span
          ><AdminSketchIcon name="arrow" class="sidebar-model-arrow" />
        </button>
      </div>
    </aside>

    <main id="admin-main" class="workspace">
      <header class="workspace-bar">
        <div class="workspace-location">
          <span class="workspace-location-icon" aria-hidden="true"
            ><AdminSketchIcon :name="state.section" /></span
          ><span class="workspace-context"
            ><span>管理空间</span
            ><strong>{{ adminSectionTitles[state.section] }}</strong></span
          >
        </div>
        <div class="workspace-tools">
          <span class="today-date"
            ><AppIcon name="calendar" />{{ dateLabel }}</span
          ><ThemeToggle /><UserAccountMenu
            :user="state.user || {}"
            :busy="state.logoutBusy"
            portal-path="/"
            portal-label="学习空间"
            @logout="logout"
          />
        </div>
      </header>
      <header class="topbar admin-topbar">
        <div>
          <h1>{{ adminSectionTitles[state.section] }}</h1>
          <p>{{ sectionDescriptions[state.section] }}</p>
        </div>
        <button
          v-if="state.section === 'overview'"
          class="admin-secondary admin-refresh"
          type="button"
          :disabled="state.summaryBusy"
          @click="refreshSummary"
        >
          <AdminSketchIcon
            name="refresh"
            :class="{ 'is-refreshing': state.summaryBusy }"
          />{{ state.summaryBusy ? "正在更新" : "刷新数据" }}</button
        ><span v-else class="admin-role-badge"
          ><AdminSketchIcon name="shield" />管理员</span
        >
      </header>

      <section
        v-show="state.section === 'overview'"
        id="overview"
        class="section"
        :class="{ active: state.section === 'overview' }"
        aria-label="管理概览"
      >
        <div class="admin-overview-workspace">
          <div class="admin-metrics-band">
            <section
              v-for="metric in overviewMetrics"
              :key="metric.label"
              :class="metric.tone"
            >
              <span class="admin-metric-label"
                ><AdminSketchIcon :name="metric.icon" />{{ metric.label }}</span
              ><strong
                >{{ number(metric.value)
                }}<small>{{ metric.unit }}</small></strong
              ><span class="admin-metric-caption">{{ metric.caption }}</span>
            </section>
          </div>
          <div class="admin-overview-body">
            <article class="admin-trend-panel panel">
              <header class="admin-panel-heading">
                <div>
                  <h2><AdminSketchIcon name="overview" />近 7 天练词趋势</h2>
                  <p>包含单词练习与复习的每次作答</p>
                </div>
                <span class="admin-period-label"
                  >近 7 天 <b>{{ number(weekReviews) }}</b> 次</span
                >
              </header>
              <AdminTrendChart
                v-if="state.section === 'overview'"
                :points="state.summary.dailyWordStats || []"
              />
              <div class="admin-activity-totals">
                <div v-for="item in activityTotals" :key="item.label">
                  <AdminSketchIcon :name="item.icon" /><span>{{
                    item.label
                  }}</span
                  ><strong
                    >{{ number(item.value)
                    }}<small>{{ item.unit }}</small></strong
                  >
                </div>
              </div>
            </article>
            <aside class="admin-overview-rail">
              <section class="admin-status-panel panel">
                <header class="admin-panel-heading">
                  <h2><AdminSketchIcon name="users" />账号状态</h2>
                  <button
                    class="admin-text-link"
                    type="button"
                    @click="showSection('users')"
                  >
                    管理用户<AdminSketchIcon name="arrow" />
                  </button>
                </header>
                <div class="admin-status-body">
                  <div
                    class="admin-status-ring"
                    role="img"
                    :aria-label="`启用账号 ${state.summary.activeUsers || 0} 个，共 ${state.summary.users || 0} 个，占比 ${activePercent}%`"
                  >
                    <svg viewBox="0 0 180 180" aria-hidden="true">
                      <circle cx="90" cy="90" r="66" class="admin-ring-track" />
                      <circle
                        v-if="state.summary.users"
                        cx="90"
                        cy="90"
                        r="66"
                        class="admin-ring-progress"
                        pathLength="100"
                        :stroke-dasharray="`${activePercent} ${100 - activePercent}`"
                        transform="rotate(-90 90 90)"
                      />
                      <SketchShape
                        kind="circle"
                        :args="[90, 90, 149]"
                        :options="{
                          stroke: 'var(--admin-tree-line)',
                          strokeWidth: 1,
                          roughness: 0.75,
                        }"
                      />
                      <SketchShape
                        kind="circle"
                        :args="[90, 90, 115]"
                        :options="{
                          stroke: 'var(--admin-tree-line)',
                          strokeWidth: 0.8,
                          roughness: 0.65,
                        }"
                      />
                    </svg>
                    <div>
                      <strong>{{
                        state.summary.users ? activePercent + "%" : "—"
                      }}</strong
                      ><span>账号启用占比</span>
                    </div>
                  </div>
                  <dl class="admin-status-legend">
                    <div>
                      <dt><i></i>启用账号</dt>
                      <dd>{{ number(state.summary.activeUsers) }}</dd>
                    </div>
                    <div>
                      <dt><i class="is-muted"></i>停用账号</dt>
                      <dd>{{ number(inactiveUsers) }}</dd>
                    </div>
                  </dl>
                </div>
                <p class="admin-scope-note">
                  按账号启用状态统计，不代表登录活跃度。
                </p>
              </section>
              <section class="admin-assets-panel panel">
                <header class="admin-panel-heading">
                  <h2><AdminSketchIcon name="knowledge" />内容资产</h2>
                  <span>累计</span>
                </header>
                <div class="admin-asset-list">
                  <button
                    v-for="item in contentAssets"
                    :key="item.id"
                    type="button"
                    @click="showSection(item.id)"
                  >
                    <span class="admin-asset-icon" :class="item.id"
                      ><AdminSketchIcon :name="item.id" /></span
                    ><span
                      >{{ item.label }}<small>{{ item.caption }}</small></span
                    ><strong
                      >{{ number(item.value)
                      }}<small>{{ item.unit }}</small></strong
                    ><AdminSketchIcon name="arrow" />
                  </button>
                </div>
              </section>
            </aside>
          </div>
        </div>
      </section>

      <section
        v-show="state.section === 'users'"
        id="users"
        class="section"
        :class="{ active: state.section === 'users' }"
        aria-label="用户管理"
      >
        <div class="admin-two-column admin-users-layout">
          <section class="panel admin-list-panel">
            <header class="admin-panel-heading">
              <div>
                <h2><AdminSketchIcon name="users" />账号列表</h2>
                <p>
                  {{
                    state.users.length >= 100
                      ? "显示最近 100 个匹配账号"
                      : `共 ${state.users.length} 个匹配账号`
                  }}
                </p>
              </div>
              <label class="admin-search-box"
                ><AdminSketchIcon name="search" /><input
                  v-model.trim="state.userSearch"
                  class="admin-search"
                  placeholder="搜索用户名或昵称"
                  aria-label="搜索用户名或昵称"
                  @input="debouncedLoadUsers"
              /></label>
            </header>
            <div class="admin-table" role="table" aria-label="用户列表">
              <div class="admin-row admin-row-head" role="row">
                <span role="columnheader">用户</span
                ><span role="columnheader">角色</span
                ><span role="columnheader">状态</span
                ><span role="columnheader">操作</span>
              </div>
              <div
                v-for="user in state.users"
                :key="user.id"
                class="admin-row"
                :class="{ 'is-selected': state.userProfile?.id === user.id }"
                role="row"
              >
                <span class="admin-user-cell" role="cell"
                  ><span class="admin-user-avatar" aria-hidden="true">{{
                    (user.nickname || user.username).slice(0, 1)
                  }}</span
                  ><span
                    ><strong>{{ user.nickname || user.username }}</strong
                    ><small>@{{ user.username }}</small></span
                  ></span
                ><span role="cell">{{
                  user.role === "ADMIN" ? "管理员" : "学习者"
                }}</span
                ><span role="cell"
                  ><mark :class="user.status === 'ACTIVE' ? 'ok' : 'danger'">{{
                    user.status === "ACTIVE" ? "已启用" : "已停用"
                  }}</mark></span
                ><span class="admin-actions" role="cell"
                  ><button
                    type="button"
                    @click="runAction(() => loadUserProfile(user.id))"
                  >
                    详情</button
                  ><button
                    type="button"
                    :disabled="user.role === 'ADMIN'"
                    @click="runAction(() => editUser(user))"
                  >
                    编辑</button
                  ><button
                    type="button"
                    :disabled="user.role === 'ADMIN'"
                    @click="runAction(() => toggleUserStatus(user))"
                  >
                    {{ user.status === "ACTIVE" ? "停用" : "启用" }}</button
                  ><button
                    type="button"
                    :disabled="user.role === 'ADMIN'"
                    @click="runAction(() => resetUserPassword(user))"
                  >
                    重置密码
                  </button></span
                >
              </div>
              <div v-if="!state.users.length" class="admin-empty-state">
                <AdminSketchIcon name="search" />
                <h3>没有找到匹配用户</h3>
                <p>试试其他用户名或昵称</p>
              </div>
            </div>
          </section>
          <section class="panel admin-profile-panel">
            <header class="admin-panel-heading">
              <h2><AdminSketchIcon name="overview" />学习数据</h2>
              <span>用户详情</span>
            </header>
            <div v-if="state.userProfile" class="admin-profile">
              <div class="admin-profile-head">
                <div>
                  <strong>{{
                    state.userProfile.nickname || state.userProfile.username
                  }}</strong
                  ><small
                    >@{{ state.userProfile.username }} ·
                    {{
                      state.userProfile.role === "ADMIN" ? "管理员" : "学习者"
                    }}</small
                  >
                </div>
                <mark
                  :class="state.userProfile.aiConfigured ? 'ok' : 'neutral'"
                  >{{
                    state.userProfile.aiConfigured ? "已配置 AI" : "未配置 AI"
                  }}</mark
                >
              </div>
              <div class="admin-profile-grid">
                <div v-for="item in profileMetrics" :key="item.label">
                  <strong>{{ item.value }}</strong
                  ><span>{{ item.label }}</span>
                </div>
              </div>
              <section class="admin-readings">
                <header class="admin-panel-heading">
                  <h2>最近阅读</h2>
                  <span>{{ state.userReadings.length }} 条记录</span>
                </header>
                <div class="admin-table compact">
                  <div class="admin-row reading-row admin-row-head">
                    <span>文章</span><span>进度</span><span>查词</span
                    ><span>时间</span>
                  </div>
                  <div
                    v-for="record in state.userReadings"
                    :key="record.id"
                    class="admin-row reading-row"
                  >
                    <span
                      ><strong>{{ record.title }}</strong
                      ><small>{{
                        difficultyLabel(record.difficulty)
                      }}</small></span
                    ><span
                      ><mark :class="record.finished ? 'ok' : 'neutral'"
                        >{{ record.readProgress || 0 }}%</mark
                      ></span
                    ><span>{{ record.lookedUpWords || 0 }}</span
                    ><span>{{
                      formatDate(record.updatedAt || record.createdAt)
                    }}</span>
                  </div>
                  <p
                    v-if="!state.userReadings.length"
                    class="admin-inline-empty"
                  >
                    还没有阅读记录
                  </p>
                </div>
              </section>
            </div>
            <div v-else class="admin-empty-state">
              <AdminSketchIcon name="users" />
              <h3>查看用户的学习足迹</h3>
              <p>点击左侧「详情」查看词汇、写作和阅读数据</p>
            </div>
          </section>
        </div>
      </section>

      <section
        v-show="state.section === 'words'"
        id="words"
        class="section"
        :class="{ active: state.section === 'words' }"
        aria-label="词库维护"
      >
        <div class="admin-two-column">
          <section class="panel admin-list-panel">
            <header class="admin-panel-heading">
              <div>
                <h2><AdminSketchIcon name="words" />单词列表</h2>
                <p>共 {{ number(state.wordPage.total) }} 个单词</p>
              </div>
              <label class="admin-search-box"
                ><AdminSketchIcon name="search" /><input
                  v-model.trim="state.wordSearch"
                  class="admin-search"
                  placeholder="搜索英文或中文"
                  aria-label="搜索英文或中文"
                  @input="debouncedLoadWords"
              /></label>
            </header>
            <div class="admin-table">
              <div class="admin-row admin-row-head">
                <span>单词</span><span>释义</span><span>分类</span
                ><span>操作</span>
              </div>
              <div
                v-for="word in state.wordPage.words"
                :key="word.id"
                class="admin-row"
                :class="{ 'is-selected': state.wordForm.id === word.id }"
              >
                <span
                  ><strong class="admin-word-title">{{ word.word }}</strong
                  ><small>{{ word.partOfSpeech }}</small></span
                ><span>{{ word.meaningCn }}</span
                ><span
                  ><mark class="neutral">{{
                    word.levelTag || "未分类"
                  }}</mark></span
                ><span class="admin-actions"
                  ><button type="button" @click="fillWordForm(word)">
                    编辑</button
                  ><button
                    type="button"
                    class="is-danger"
                    @click="runAction(() => deleteWord(word))"
                  >
                    删除
                  </button></span
                >
              </div>
              <div
                v-if="!state.wordPage.words.length"
                class="admin-empty-state"
              >
                <AdminSketchIcon name="search" />
                <h3>没有找到匹配单词</h3>
                <p>试试其他英文或中文释义</p>
              </div>
            </div>
            <footer class="admin-pagination">
              <button
                type="button"
                :disabled="state.wordPage.page <= 0"
                @click="runAction(() => loadWords(state.wordPage.page - 1))"
              >
                上一页</button
              ><span
                >第 {{ state.wordPage.page + 1 }} 页 · 共
                {{ number(state.wordPage.total) }} 条</span
              ><button
                type="button"
                :disabled="
                  (state.wordPage.page + 1) * state.wordPage.size >=
                  state.wordPage.total
                "
                @click="runAction(() => loadWords(state.wordPage.page + 1))"
              >
                下一页
              </button>
            </footer>
          </section>
          <section class="panel admin-editor-panel">
            <header class="admin-panel-heading">
              <div>
                <h2>
                  <AdminSketchIcon
                    :name="state.wordForm.id ? 'writing' : 'plus'"
                  />{{ state.wordForm.id ? "编辑单词" : "新增单词" }}
                </h2>
                <p>
                  {{
                    state.wordForm.id
                      ? state.wordForm.word
                      : "完善释义与语境例句"
                  }}
                </p>
              </div>
              <button
                v-if="state.wordForm.id"
                class="admin-text-link"
                type="button"
                @click="clearWordForm"
              >
                新增
              </button>
            </header>
            <form class="admin-form" @submit.prevent="runAction(saveWord)">
              <div class="admin-form-fields">
                <label class="admin-field"
                  ><span>英文单词</span
                  ><input
                    v-model="state.wordForm.word"
                    placeholder="如 language"
                    required /></label
                ><label class="admin-field"
                  ><span>词性</span
                  ><input
                    v-model="state.wordForm.partOfSpeech"
                    placeholder="如 n. / v." /></label
                ><label class="admin-field span-all"
                  ><span>中文释义</span
                  ><input
                    v-model="state.wordForm.meaningCn"
                    placeholder="输入完整释义"
                    required /></label
                ><label class="admin-field span-all"
                  ><span>分类</span
                  ><input
                    v-model="state.wordForm.levelTag"
                    placeholder="如 cet4-bank" /></label
                ><label class="admin-field"
                  ><span>美式音标</span
                  ><input
                    v-model="state.wordForm.phoneticUs"
                    placeholder="/ˈlæŋɡwɪdʒ/" /></label
                ><label class="admin-field"
                  ><span>英式音标</span
                  ><input
                    v-model="state.wordForm.phoneticUk"
                    placeholder="/ˈlæŋɡwɪdʒ/" /></label
                ><label class="admin-field span-all"
                  ><span>英文例句</span
                  ><textarea
                    v-model="state.wordForm.exampleEn"
                    placeholder="放进真实语境，更容易理解"
                  ></textarea></label
                ><label class="admin-field span-all"
                  ><span>例句翻译</span
                  ><textarea
                    v-model="state.wordForm.exampleCn"
                    placeholder="与英文例句对应的中文翻译"
                  ></textarea>
                </label>
              </div>
              <footer class="admin-form-actions">
                <button
                  class="settings-secondary"
                  type="button"
                  @click="clearWordForm"
                >
                  清空</button
                ><button class="settings-primary" type="submit">
                  保存单词
                </button>
              </footer>
            </form>
          </section>
        </div>
      </section>

      <section
        v-show="state.section === 'knowledge'"
        id="knowledge"
        class="section"
        :class="{ active: state.section === 'knowledge' }"
        aria-label="知识库管理"
      >
        <div class="admin-two-column">
          <section class="panel admin-list-panel">
            <header class="admin-panel-heading">
              <div>
                <h2><AdminSketchIcon name="knowledge" />知识文档</h2>
                <p>共 {{ state.knowledge.length }} 篇 · 供 AI 检索参考</p>
              </div>
            </header>
            <div class="admin-table">
              <div class="admin-row admin-row-head">
                <span>标题</span><span>类型</span><span>标签</span>
              </div>
              <div
                v-for="doc in state.knowledge"
                :key="doc.id || doc.title"
                class="admin-row"
              >
                <span
                  ><strong>{{ doc.title }}</strong
                  ><small>{{ formatDate(doc.createdAt) }}</small></span
                ><span
                  ><mark class="neutral">{{ doc.sourceType }}</mark></span
                ><span>{{ doc.tags }}</span>
              </div>
              <div v-if="!state.knowledge.length" class="admin-empty-state">
                <AdminSketchIcon name="knowledge" />
                <h3>还没有知识文档</h3>
                <p>导入语法、评分标准等资料，为 AI 提供参考</p>
              </div>
            </div>
          </section>
          <section class="panel admin-editor-panel">
            <header class="admin-panel-heading">
              <div>
                <h2><AdminSketchIcon name="plus" />导入知识</h2>
                <p>保存后自动切分为可检索片段</p>
              </div>
            </header>
            <form
              class="admin-form"
              @submit.prevent="runAction(uploadKnowledge)"
            >
              <div class="admin-form-fields">
                <label class="admin-field span-all"
                  ><span>文档标题</span
                  ><input
                    v-model="state.knowledgeForm.title"
                    placeholder="如 四级写作评分标准"
                    required /></label
                ><label class="admin-field"
                  ><span>类型</span
                  ><input
                    v-model="state.knowledgeForm.sourceType"
                    placeholder="如 grammar / rubric" /></label
                ><label class="admin-field"
                  ><span>标签</span
                  ><input
                    v-model="state.knowledgeForm.tags"
                    placeholder="用逗号分隔" /></label
                ><label class="admin-field span-all"
                  ><span>知识内容</span
                  ><textarea
                    v-model="state.knowledgeForm.content"
                    class="admin-tall-textarea"
                    placeholder="粘贴正文、语法规则或评分标准"
                    required
                  ></textarea>
                </label>
              </div>
              <footer class="admin-form-actions">
                <button class="settings-primary" type="submit">
                  导入并切分
                </button>
              </footer>
            </form>
          </section>
        </div>
      </section>

      <section
        v-show="state.section === 'news'"
        id="news"
        class="section"
        :class="{ active: state.section === 'news' }"
        aria-label="阅读内容"
      >
        <div class="admin-two-column">
          <section class="panel admin-list-panel">
            <header class="admin-panel-heading">
              <div>
                <h2><AdminSketchIcon name="news" />阅读文章</h2>
                <p>共 {{ number(state.newsPage.total) }} 篇文章</p>
              </div>
              <label class="admin-search-box"
                ><AdminSketchIcon name="search" /><input
                  v-model.trim="state.newsSearch"
                  class="admin-search"
                  placeholder="搜索标题、来源或标签"
                  aria-label="搜索标题、来源或标签"
                  @input="debouncedLoadNews"
              /></label>
            </header>
            <div class="admin-table">
              <div class="admin-row admin-row-head">
                <span>标题</span><span>难度</span><span>来源</span
                ><span>操作</span>
              </div>
              <div
                v-for="article in state.newsPage.articles"
                :key="article.id"
                class="admin-row"
                :class="{ 'is-selected': state.newsForm.id === article.id }"
              >
                <span
                  ><strong>{{ article.title }}</strong
                  ><small>{{ article.topicTags }}</small></span
                ><span
                  ><mark class="neutral">{{
                    difficultyLabel(article.difficulty)
                  }}</mark></span
                ><span>{{ article.sourceName }}</span
                ><span class="admin-actions"
                  ><button type="button" @click="fillNewsForm(article)">
                    编辑</button
                  ><button
                    type="button"
                    class="is-danger"
                    @click="runAction(() => deleteNews(article))"
                  >
                    删除
                  </button></span
                >
              </div>
              <div
                v-if="!state.newsPage.articles.length"
                class="admin-empty-state"
              >
                <AdminSketchIcon name="search" />
                <h3>没有找到匹配文章</h3>
                <p>试试其他标题或主题</p>
              </div>
            </div>
            <footer class="admin-pagination">
              <button
                type="button"
                :disabled="state.newsPage.page <= 0"
                @click="runAction(() => loadNews(state.newsPage.page - 1))"
              >
                上一页</button
              ><span
                >第 {{ state.newsPage.page + 1 }} 页 · 共
                {{ number(state.newsPage.total) }} 篇</span
              ><button
                type="button"
                :disabled="
                  (state.newsPage.page + 1) * state.newsPage.size >=
                  state.newsPage.total
                "
                @click="runAction(() => loadNews(state.newsPage.page + 1))"
              >
                下一页
              </button>
            </footer>
          </section>
          <section class="panel admin-editor-panel">
            <header class="admin-panel-heading">
              <div>
                <h2>
                  <AdminSketchIcon
                    :name="state.newsForm.id ? 'writing' : 'plus'"
                  />{{ state.newsForm.id ? "编辑阅读" : "新增阅读" }}
                </h2>
                <p>
                  {{
                    state.newsForm.id
                      ? "更新文章内容与阅读难度"
                      : "为阅读空间添加新的内容"
                  }}
                </p>
              </div>
              <button
                v-if="state.newsForm.id"
                class="admin-text-link"
                type="button"
                @click="clearNewsForm"
              >
                新增
              </button>
            </header>
            <form class="admin-form" @submit.prevent="runAction(saveNews)">
              <div class="admin-form-fields">
                <label class="admin-field span-all"
                  ><span>文章标题</span
                  ><input
                    v-model="state.newsForm.title"
                    placeholder="输入文章标题"
                    required /></label
                ><label class="admin-field"
                  ><span>来源</span
                  ><input
                    v-model="state.newsForm.sourceName"
                    placeholder="来源名称" /></label
                ><label class="admin-field"
                  ><span>难度</span
                  ><select v-model="state.newsForm.difficulty">
                    <option value="" disabled>选择难度</option>
                    <option value="easy">简单</option>
                    <option value="medium">进阶</option>
                    <option value="hard">挑战</option>
                  </select></label
                ><label class="admin-field span-all"
                  ><span>主题标签</span
                  ><input
                    v-model="state.newsForm.topicTags"
                    placeholder="用逗号分隔，如 科学,科技" /></label
                ><label class="admin-field span-all"
                  ><span>摘要</span
                  ><textarea
                    v-model="state.newsForm.summary"
                    placeholder="简要介绍文章内容"
                  ></textarea></label
                ><label class="admin-field span-all"
                  ><span>英文正文</span
                  ><textarea
                    v-model="state.newsForm.content"
                    class="admin-tall-textarea"
                    placeholder="粘贴完整文章正文"
                    required
                  ></textarea>
                </label>
              </div>
              <footer class="admin-form-actions">
                <button
                  class="settings-secondary"
                  type="button"
                  @click="clearNewsForm"
                >
                  清空</button
                ><button class="settings-primary" type="submit">
                  保存阅读
                </button>
              </footer>
            </form>
          </section>
        </div>
      </section>

      <section
        v-show="state.section === 'ai'"
        id="ai"
        class="section"
        :class="{ active: state.section === 'ai' }"
        aria-label="模型连接"
      >
        <div class="admin-ai-workspace">
          <section class="panel admin-ai-panel">
            <header class="admin-panel-heading">
              <div>
                <h2><AdminSketchIcon name="ai" />连接 AI 模型</h2>
                <p>使用兼容 OpenAI 接口的服务</p>
              </div>
              <span class="settings-status" role="status">{{
                state.ai.status
              }}</span>
            </header>
            <div class="settings-form">
              <label class="settings-field"
                ><span>模型地址</span
                ><input
                  v-model="state.ai.baseUrl"
                  autocomplete="off"
                  placeholder="https://api.openai.com/v1" /></label
              ><label class="settings-field"
                ><span>API 密钥</span
                ><input
                  v-model="state.ai.apiKey"
                  type="password"
                  autocomplete="off"
                  placeholder="留空则沿用已保存密钥"
              /></label>
              <div class="admin-model-row">
                <label class="settings-field admin-model-select"
                  ><span>模型名称</span
                  ><select
                    v-model="state.ai.chatModel"
                    :disabled="!state.ai.models.length"
                  >
                    <option v-if="!state.ai.models.length" value="">
                      请先获取模型列表
                    </option>
                    <option
                      v-for="model in state.ai.models"
                      :key="model"
                      :value="model"
                    >
                      {{ model }}
                    </option>
                  </select></label
                ><button
                  class="settings-secondary"
                  type="button"
                  :disabled="state.aiBusy"
                  @click="runAction(fetchModels)"
                >
                  获取模型
                </button>
              </div>
              <div class="admin-form-actions">
                <button
                  class="settings-primary"
                  type="button"
                  :disabled="state.aiBusy"
                  @click="runAction(saveAiSettings)"
                >
                  保存并应用
                </button>
              </div>
            </div>
          </section>
          <aside class="admin-ai-context">
            <AdminSketchIcon name="shield" />
            <h2>当前账号的模型连接</h2>
            <p>设置仅应用于当前管理员，学习者使用各自的模型配置。</p>
            <div class="admin-ai-identity">
              <span class="admin-user-avatar">{{
                (state.user?.nickname || "管").slice(0, 1)
              }}</span
              ><span
                ><strong>{{ state.user?.nickname || "管理员" }}</strong
                ><small>@{{ state.user?.username }}</small></span
              >
            </div>
          </aside>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import AppIcon from "@/components/AppIcon.vue";
import AdminSketchIcon from "@/components/AdminSketchIcon.vue";
import AdminTrendChart from "@/components/AdminTrendChart.vue";
import SketchShape from "@/components/SketchShape.vue";
import UserAccountMenu from "@/components/UserAccountMenu.vue";
import ThemeToggle from "@/components/ThemeToggle.vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { computed, onMounted, reactive } from "vue";
import { useRouter } from "vue-router";
import { adminApi, aiApi, authApi } from "@/api/modules";
import { formatDate, normalizeModelList } from "@/api/client";
import "@/styles/admin-studio.css";

const router = useRouter();

const sections = [
  { id: "overview", label: "数据概览" },
  { id: "users", label: "用户管理" },
  { id: "words", label: "词库维护" },
  { id: "knowledge", label: "知识文档" },
  { id: "news", label: "阅读内容" },
  { id: "ai", label: "模型连接" },
];

const adminSectionTitles = {
  overview: "管理概览",
  users: "用户管理",
  words: "词库维护",
  knowledge: "知识库管理",
  news: "阅读内容",
  ai: "模型连接",
};
const emptyWord = () => ({
  id: "",
  word: "",
  meaningCn: "",
  partOfSpeech: "",
  levelTag: "",
  phoneticUs: "",
  phoneticUk: "",
  exampleEn: "",
  exampleCn: "",
});
const emptyNews = () => ({
  id: "",
  title: "",
  summary: "",
  content: "",
  sourceName: "",
  difficulty: "",
  topicTags: "",
});

const state = reactive({
  logoutBusy: false,
  loading: true,
  bootError: "",
  summaryBusy: false,
  user: null,
  section: "overview",
  summary: {},
  users: [],
  userSearch: "",
  userProfile: null,
  userReadings: [],
  wordSearch: "",
  wordPage: { words: [], total: 0, page: 0, size: 20 },
  wordForm: emptyWord(),
  knowledge: [],
  knowledgeForm: { title: "", sourceType: "", tags: "", content: "" },
  newsSearch: "",
  newsPage: { articles: [], total: 0, page: 0, size: 20 },
  newsForm: emptyNews(),
  ai: { baseUrl: "", apiKey: "", chatModel: "", models: [], status: "未连接" },
  aiBusy: false,
});

const inactiveUsers = computed(() =>
  Math.max(
    0,
    Number(state.summary.users || 0) - Number(state.summary.activeUsers || 0),
  ),
);
const activePercent = computed(() =>
  state.summary.users
    ? Math.round((state.summary.activeUsers / state.summary.users) * 100)
    : 0,
);
const sectionDescriptions = {
  overview: "查看学习记录、账号状态与内容资产",
  users: "管理账号，了解每位学习者的学习记录",
  words: "维护释义、词性和例句，为学习提供准确内容",
  knowledge: "整理知识资料，为 AI 回答补充参考依据",
  news: "编辑阅读文章、主题和难度",
  ai: "配置当前管理员使用的 AI 服务",
};
const dateLabel = new Intl.DateTimeFormat("zh-CN", {
  month: "long",
  day: "numeric",
  weekday: "long",
  timeZone: "Asia/Shanghai",
}).format(new Date());
const number = (value) => (Number(value) || 0).toLocaleString("zh-CN");
const difficultyLabel = (value) =>
  ({ easy: "简单", medium: "进阶", hard: "挑战" })[value] || value || "未分类";
const weekReviews = computed(() =>
  (state.summary.dailyWordStats || []).reduce(
    (sum, item) => sum + (Number(item.words) || 0),
    0,
  ),
);
const overviewMetrics = computed(() => [
  {
    label: "今日练词作答",
    value: state.summary.todayWordReviews,
    unit: "次",
    caption: "包含练习与复习",
    icon: "overview",
    tone: "is-green",
  },
  {
    label: "今日练词人数",
    value: state.summary.todayLearners,
    unit: "人",
    caption: "按作答用户去重",
    icon: "users",
    tone: "is-blue",
  },
  {
    label: "今日覆盖单词",
    value: state.summary.todayUniqueWords,
    unit: "词",
    caption: "按已练单词去重",
    icon: "words",
    tone: "",
  },
  {
    label: "注册账号",
    value: state.summary.users,
    unit: "个",
    caption: "当前全部账号",
    icon: "shield",
    tone: "",
  },
]);
const contentAssets = computed(() => [
  {
    id: "words",
    label: "单词词库",
    caption: "释义与语境例句",
    value: state.summary.words,
    unit: "词",
  },
  {
    id: "news",
    label: "阅读文章",
    caption: "分级阅读内容",
    value: state.summary.news,
    unit: "篇",
  },
  {
    id: "knowledge",
    label: "知识文档",
    caption: "AI 检索参考资料",
    value: state.summary.knowledgeDocuments,
    unit: "篇",
  },
]);
const activityTotals = computed(() => [
  {
    label: "累计会话",
    value: state.summary.conversations,
    unit: "个",
    icon: "chat",
  },
  {
    label: "写作提交",
    value: state.summary.essays,
    unit: "次",
    icon: "writing",
  },
  {
    label: "阅读记录",
    value: state.summary.readings,
    unit: "条",
    icon: "news",
  },
  {
    label: "错词词条",
    value: state.summary.reviewWords,
    unit: "条",
    icon: "review",
  },
]);
const profileMetrics = computed(() => {
  const profile = state.userProfile || {};
  return [
    { label: "学习等级", value: `Lv.${profile.level || 1}` },
    { label: "当前经验", value: number(profile.experience) },
    { label: "已背单词", value: number(profile.learnedWords) },
    { label: "收藏单词", value: number(profile.favoriteWords) },
    { label: "已掌握", value: number(profile.masteredWords) },
    { label: "错词", value: number(profile.reviewWords) },
    { label: "写作提交", value: number(profile.essays) },
    { label: "阅读记录", value: number(profile.readings) },
    { label: "会话", value: number(profile.conversations) },
  ];
});

function debounce(fn, wait = 300) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), wait);
  };
}

const debouncedLoadUsers = debounce(() => runAction(loadUsers));
const debouncedLoadWords = debounce(() => runAction(() => loadWords(0)));
const debouncedLoadNews = debounce(() => runAction(() => loadNews(0)));

async function runAction(action) {
  try {
    await action();
  } catch (error) {
    if (error === "cancel" || error === "close") return;
    ElMessage.error(error.message || "操作未完成，请稍后重试");
  }
}

async function showSection(id) {
  state.section = id;
  try {
    if (id === "overview") await loadSummary();
    if (id === "users") await loadUsers();
    if (id === "words") await loadWords();
    if (id === "knowledge") await loadKnowledge();
    if (id === "news") await loadNews();
    if (id === "ai") await loadAiSettings();
  } catch (error) {
    ElMessage.error(error.message || "页面数据加载失败，请重试");
  }
}

async function logout() {
  if (state.logoutBusy) return;
  state.logoutBusy = true;
  try {
    await authApi.logout();
    await router.replace("/login");
  } catch (error) {
    ElMessage.error(error.message || "退出登录失败，请重试");
  } finally {
    state.logoutBusy = false;
  }
}

async function loadSummary() {
  state.summary = await adminApi.summary();
}

async function refreshSummary() {
  if (state.summaryBusy) return;
  state.summaryBusy = true;
  try {
    await loadSummary();
  } catch (error) {
    ElMessage.error(error.message || "数据刷新失败，请重试");
  } finally {
    state.summaryBusy = false;
  }
}

async function loadUsers() {
  state.users = await adminApi.users(state.userSearch);
}

async function loadUserProfile(id) {
  const [profile, readings] = await Promise.all([
    adminApi.userProfile(id),
    adminApi.userReadings(id),
  ]);
  state.userProfile = profile;
  state.userReadings = readings || [];
}

async function editUser(user) {
  if (user.role === "ADMIN") {
    ElMessage.warning("管理员账号不在这里编辑");
    return;
  }
  const { value } = await ElMessageBox.prompt("输入新的昵称", "编辑用户", {
    inputValue: user.nickname || user.username,
    confirmButtonText: "保存",
    cancelButtonText: "取消",
  });
  await adminApi.updateUser(user.id, { nickname: value });
  ElMessage.success("用户昵称已更新");
  await loadUsers();
  if (state.userProfile?.id === user.id) await loadUserProfile(user.id);
}

async function toggleUserStatus(user) {
  if (user.role === "ADMIN") {
    ElMessage.warning("管理员账号不能禁用");
    return;
  }
  const status = user.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
  await adminApi.updateUserStatus(user.id, status);
  ElMessage.success(status === "ACTIVE" ? "用户已启用" : "用户已禁用");
  await loadUsers();
}

async function resetUserPassword(user) {
  if (user.role === "ADMIN") {
    ElMessage.warning("管理员密码不在这里重置");
    return;
  }
  const { value } = await ElMessageBox.prompt(
    "输入新密码，至少 6 位",
    "重置密码",
    {
      inputValue: "student123",
      confirmButtonText: "重置",
      cancelButtonText: "取消",
    },
  );
  await adminApi.resetPassword(user.id, value);
  ElMessage.success("密码已重置");
}

async function loadWords(page = state.wordPage.page) {
  state.wordPage = await adminApi.words({
    page,
    size: state.wordPage.size,
    search: state.wordSearch,
  });
}

function fillWordForm(word) {
  Object.assign(state.wordForm, emptyWord(), word || {});
}

function clearWordForm() {
  Object.assign(state.wordForm, emptyWord());
}

async function saveWord() {
  const payload = { ...state.wordForm };
  const id = payload.id;
  delete payload.id;
  if (id) await adminApi.updateWord(id, payload);
  else await adminApi.createWord(payload);
  ElMessage.success(id ? "单词已更新" : "单词已新增");
  clearWordForm();
  await loadWords();
}

async function deleteWord(word) {
  await ElMessageBox.confirm(`确认删除 ${word.word}？`, "删除单词", {
    type: "warning",
  });
  await adminApi.deleteWord(word.id);
  ElMessage.success("单词已删除");
  await loadWords();
}

async function loadKnowledge() {
  state.knowledge = await adminApi.knowledge();
}

async function uploadKnowledge() {
  await adminApi.uploadKnowledge({ ...state.knowledgeForm });
  state.knowledgeForm = { title: "", sourceType: "", tags: "", content: "" };
  ElMessage.success("知识已导入");
  await Promise.all([loadKnowledge(), loadSummary()]);
}

async function loadNews(page = state.newsPage.page) {
  state.newsPage = await adminApi.news({
    page,
    size: state.newsPage.size,
    search: state.newsSearch,
  });
}

function fillNewsForm(article) {
  Object.assign(state.newsForm, emptyNews(), article || {});
}

function clearNewsForm() {
  Object.assign(state.newsForm, emptyNews());
}

async function saveNews() {
  const payload = { ...state.newsForm };
  const id = payload.id;
  delete payload.id;
  if (id) await adminApi.updateNews(id, payload);
  else await adminApi.createNews(payload);
  ElMessage.success(id ? "阅读文章已更新" : "阅读文章已新增");
  clearNewsForm();
  await Promise.all([loadNews(), loadSummary()]);
}

async function deleteNews(article) {
  await ElMessageBox.confirm(`确认删除《${article.title}》？`, "删除文章", {
    type: "warning",
  });
  await adminApi.deleteNews(article.id);
  ElMessage.success("文章已删除");
  await Promise.all([loadNews(), loadSummary()]);
}

async function loadAiSettings() {
  const settings = await aiApi.settings();
  state.ai.baseUrl = settings.baseUrl || "";
  state.ai.apiKey = "";
  state.ai.chatModel = settings.chatModel || "";
  state.ai.models = settings.chatModel ? [settings.chatModel] : [];
  state.ai.status = settings.hasApiKey ? "已保存密钥" : "未保存密钥";
}

async function fetchModels() {
  state.aiBusy = true;
  state.ai.status = "正在获取模型...";
  try {
    const models = await aiApi.models({
      baseUrl: state.ai.baseUrl,
      apiKey: state.ai.apiKey,
    });
    state.ai.models = normalizeModelList(models);
    state.ai.chatModel = state.ai.models.includes(state.ai.chatModel)
      ? state.ai.chatModel
      : state.ai.models[0] || state.ai.chatModel;
    state.ai.status = `已获取 ${state.ai.models.length} 个模型`;
  } catch (error) {
    state.ai.status = "获取失败，请检查连接";
    throw error;
  } finally {
    state.aiBusy = false;
  }
}

async function saveAiSettings() {
  state.aiBusy = true;
  state.ai.status = "正在保存...";
  try {
    const settings = await aiApi.saveSettings({
      baseUrl: state.ai.baseUrl,
      apiKey: state.ai.apiKey,
      chatModel: state.ai.chatModel,
    });
    state.ai.apiKey = "";
    state.ai.chatModel = settings.chatModel || state.ai.chatModel;
    state.ai.status = `已应用 ${state.ai.chatModel || "当前模型"}`;
    ElMessage.success("AI 设置已保存");
  } catch (error) {
    state.ai.status = "保存失败，请重试";
    throw error;
  } finally {
    state.aiBusy = false;
  }
}

async function boot() {
  document.body.className = "admin-page";
  state.user = await authApi.me();
  if (state.user?.role !== "ADMIN") {
    router.replace("/");
    return;
  }
  await loadSummary();
  state.loading = false;
}

async function retryBoot() {
  state.loading = true;
  state.bootError = "";
  try {
    await boot();
  } catch (error) {
    state.bootError = error.message || "管理端加载失败，请稍后重试";
    state.loading = false;
  }
}

onMounted(() => {
  document.title = "LinguaMate 管理端";
  retryBoot();
});
</script>
