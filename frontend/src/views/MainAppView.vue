<template>
  <a class="skip-link" href="#main-content">跳转到学习内容</a>
  <div v-if="state.loading" class="app-loading">
    <img src="/assets/linguamate-icon.svg?v=bright" alt="" /><span
      >正在准备你的学习空间</span
    >
  </div>
  <div
    v-else
    class="app-shell"
    :class="[
      'page-' + activeSection,
      {
        'dashboard-expanded': dashboardExpanded,
        'has-writing-result': Boolean(state.writingResult),
      },
    ]"
    @keydown.esc="closeOverlays"
  >
    <aside
      class="side-nav"
      aria-label="学习导航"
      :inert="state.settingsOpen || wordDialogVisible"
    >
      <RouterLink class="brand" to="/">
        <img
          class="brand-mark"
          src="/assets/linguamate-icon.svg?v=bright"
          alt=""
        />
        <div>
          <strong>LinguaMate<span class="brand-period">.</span></strong
          ><span>让英语成为日常</span>
        </div>
      </RouterLink>
      <nav>
        <span
          class="nav-active-indicator"
          :style="{
            '--nav-index': navItems.findIndex(
              (item) => item.id === activeSection,
            ),
            '--nav-section-offset': navItems.findIndex((item) => item.id === activeSection) >= 4 ? 1 : 0,
          }"
          aria-hidden="true"
        ></span>
        <button
          v-for="(item, index) in navItems"
          :key="item.id"
          class="nav-item"
          :class="{ active: activeSection === item.id, 'nav-group-start': index === 4 }"
          :aria-current="activeSection === item.id ? 'page' : undefined"
          type="button"
          @click="goSection(item.id)"
        >
          <span class="nav-icon-wrap"><AppIcon :name="item.id" /></span><span>{{ item.label }}</span
          ><span
            v-if="item.id === 'review' && state.report.dueWords"
            class="nav-count"
            >{{ state.report.dueWords }}</span
          >
        </button>
      </nav>
      <div class="sidebar-bottom">
        <div class="sidebar-progress desktop-account-legacy">
          <div>
            <span>学习等级</span
            ><strong>Lv.{{ state.level.level || 1 }}</strong>
          </div>
          <div class="level-xp-bar">
            <div
              class="level-xp-fill"
              :style="{ width: levelPercent + '%' }"
            ></div>
          </div>
          <small
            >{{ state.level.experience || 0 }} /
            {{ state.level.nextLevelXp || 100 }} XP</small
          >
        </div>
        <button
          class="sidebar-model-config"
          type="button"
          aria-label="模型配置"
          @click="openSettings($event.currentTarget)"
        >
          <span class="sidebar-model-icon"><AppIcon name="settings" /></span>
          <span class="sidebar-model-copy"><strong>模型配置</strong><small :class="{ 'is-ready': state.aiSummary.label === '连接正常' }">{{ state.aiSummary.label }}</small></span>
          <AppIcon class="sidebar-model-arrow" name="arrow" />
        </button>
        <RouterLink
          v-if="state.currentUser?.role === 'ADMIN'"
          class="nav-link-button desktop-account-legacy"
          to="/admin"
          >管理工作台 <AppIcon name="arrow"
        /></RouterLink>
        <button
          class="sidebar-user desktop-account-legacy"
          type="button"
          aria-label="打开我的 API 设置"
          @click="openSettings"
        >
          <span class="level-avatar">{{ userName.slice(0, 1) }}</span
          ><span
            ><strong>{{ userName }}</strong></span
          ><AppIcon name="settings" />
        </button>
        <button class="nav-logout-button desktop-account-legacy" type="button" @click="logout">
          <AppIcon name="logout" />退出登录
        </button>
      </div>
    </aside>
    <main
      id="main-content"
      class="workspace"
      tabindex="-1"
      :inert="state.settingsOpen || wordDialogVisible"
    >
      <header class="workspace-bar">
        <div class="workspace-location">
          <span class="workspace-location-icon" aria-hidden="true"><AppIcon :name="activeSection" /></span>
          <span class="workspace-context"><span>学习空间</span><strong>{{ sectionTitle }}</strong></span>
        </div>
        <div class="workspace-tools">
          <span class="today-date"><AppIcon name="calendar" />{{ todayLabel }}</span
          ><button
            type="button"
            class="connection-button desktop-account-legacy"
            @click="openSettings"
          >
            <span class="status-dot"></span>模型设置<AppIcon name="settings" />
          </button>
          <ThemeToggle />
          <UserAccountMenu
            :user="state.currentUser || {}"
            :busy="state.logoutBusy"
            :level="state.level"
            :portal-path="state.currentUser?.role === 'ADMIN' ? '/admin' : ''"
            portal-label="管理工作台"
            @logout="logout"
          />
        </div>
      </header>
      <header class="topbar">
        <div class="hero-copy">
          <h1>
            {{
              activeSection === "dashboard"
                ? "你好，" + userName + "。"
                : sectionTitle
            }}
          </h1>
        </div>
      </header>
      <section
        id="dashboard"
        class="section"
        :class="{ active: activeSection === 'dashboard' }"
      >
        <section class="focus-session" aria-labelledby="studio-focus-title">
          <div class="focus-copy">
            <span class="session-label"
              ><span class="status-dot"></span>今日焦点</span
            >
            <h2 id="studio-focus-title">{{ routeDoneCount === journeySteps.length ? '这一路，' : '今天，从' }}<br /><span>{{ routeDoneCount === journeySteps.length ? '又前进了一点。' : (focusStep?.title || '记单词') + '开始。' }}</span></h2>
            <p class="studio-focus-detail">{{ focusDescription }}</p>
            <div class="studio-focus-actions">
              <button
                class="primary-button"
                type="button"
                @click="goSection(focusStep?.section || 'vocabulary')"
              >
                {{ routeDoneCount === journeySteps.length ? '再练习一次' : '开始' + (focusStep?.title || '学习') }}<AppIcon name="arrow" />
              </button>
              <button v-if="state.report.reviewWords && focusStep?.section !== 'review'" class="studio-review-link" type="button" @click="goSection('review')"><AppIcon name="review" />复习 {{ state.report.reviewWords }} 词</button>
            </div>
          </div>
          <div class="studio-art">
            <LearningStudioArt />
            <WordAtmosphere v-if="activeSection === 'dashboard'" variant="studio" />
          </div>
          <div class="studio-canvas-footer" aria-hidden="true"><span>LINGUAMATE</span><span>A little. Every day.</span></div>
        </section>
        <div class="home-grid">
          <section class="duo-route-shell">
            <div class="duo-route-heading">
              <div>
                <span class="studio-section-kicker">YOUR JOURNEY</span>
                <h2>学习路线</h2>
              </div>
              <div
                class="duo-route-progress"
                :aria-label="
                  '学习路线完成 ' + routeDoneCount + '/' + journeySteps.length
                "
              >
                <strong>{{ routeDoneCount }}<small>/{{ journeySteps.length }}</small></strong><span>已完成</span>
              </div>
            </div>
            <div class="duo-route-progress-bar" role="progressbar" aria-label="学习路线进度" :aria-valuenow="routeProgressPercent" aria-valuemin="0" aria-valuemax="100"><i :style="{ width: routeProgressPercent + '%' }"></i></div>
            <div class="duo-route-track" role="list" aria-label="学习路线">
              <div
                v-for="(step, index) in journeySteps"
                :key="step.id"
                class="duo-step"
                :class="{
                  'is-complete': step.done,
                  'is-current': step.recommended,
                }"
                role="listitem"
              >
                <button
                  class="duo-step-button"
                  :class="{
                    'is-complete': step.done,
                    'is-current': step.recommended,
                  }"
                  type="button"
                  :aria-label="routeStepAriaLabel(step)"
                  @click="goSection(step.section)"
                >
                  <span class="studio-step-icon"><AppIcon :name="step.section" /></span>
                  <span class="route-step-copy"><strong>{{ step.title }}</strong><small>{{ step.detail }}</small></span>
                  <AppIcon v-if="step.done" class="studio-step-done" name="check" />
                  <span v-else-if="step.recommended" class="studio-step-next">开始<AppIcon name="arrow" /></span>
                  <span v-else class="route-step-number">0{{ index + 1 }}</span>
                </button>
              </div>
            </div>
          </section>
          <div class="studio-journey-note"><AppIcon name="spark" /><p>{{ state.report.nextActions?.[0] || '从推荐练习开始，找到适合自己的学习节奏。' }}</p></div>
          <button class="studio-assistant-link" type="button" @click="openLearningAssistant"><AppIcon name="chat" /><span>和学习助手聊聊</span><AppIcon name="arrow" /></button>
        </div>
        <button
          class="dashboard-details-toggle"
          type="button"
          :aria-expanded="dashboardExpanded"
          aria-controls="dashboard-footprints dashboard-words"
          @click="dashboardExpanded = !dashboardExpanded"
        >
          <span>{{
            dashboardExpanded ? "收起学习概况" : "查看学习足迹与今日单词"
          }}</span
          ><span aria-hidden="true">{{ dashboardExpanded ? "−" : "+" }}</span>
        </button>
        <section id="dashboard-words" class="today-words">
          <div class="panel-heading">
            <div>
              <h2>今日单词</h2>
            </div>
            <button
              class="text-button"
              type="button"
              @click="goSection('wordbank')"
            >
              探索词库<AppIcon name="arrow" />
            </button>
          </div>
          <div class="word-strip">
            <button
              v-for="word in state.words.slice(0, 4)"
              :key="word.id"
              class="word-card daily-word"
              type="button"
              :aria-label="word.word + '，查看单词详情'"
              @click="state.selectedWord = word"
            >
              <strong>{{ word.word }}</strong
              ><span class="word-meta"
                >{{ word.partOfSpeech }} · {{ word.levelTag }}</span
              >
              <p>{{ word.meaningCn }}</p>
              <AppIcon name="arrow" />
            </button>
          </div>
        </section>
        <aside id="dashboard-footprints" class="daily-panel">
          <section class="duo-quest-panel">
            <div class="panel-heading"><h2>学习足迹</h2><button class="evidence-open" type="button" @click="evidenceOpen = true">学习变化 ↗</button></div>
            <div class="learning-stats">
              <button type="button" @click="goSection('wordbank')"><strong>{{ state.report.learnedWords || 0 }}<small>词</small></strong><span>已掌握单词</span></button>
              <button type="button" @click="goSection('review')"><strong>{{ state.report.dueWords || 0 }}<small>词</small></strong><span>到期复习</span></button>
              <button type="button" @click="goSection('writing')"><strong>{{ state.report.essayCount || 0 }}<small>篇</small></strong><span>写作练习</span></button>
              <button type="button" @click="goSection('news')"><strong>{{ state.report.readingCount || 0 }}<small>篇</small></strong><span>阅读记录</span></button>
            </div>
          </section>
        </aside>
      </section>

      <section
        id="chat"
        class="section chat-workspace"
        :class="{ active: activeSection === 'chat' }"
      >
        <ChatStudio
          :messages="state.messages" :conversations="state.conversations" :conversation-id="state.conversationId"
          :scene="state.chatScene" :mode="state.chatMode" v-model:draft="state.chatInput" :busy="state.chatBusy"
          :error="state.chatError" :model-name="state.aiSummary.modelName" :context-words="state.coachWords" :trace="state.trace"
          @send="sendChat" @prompt="sendChatPrompt" @mode="state.chatMode = $event" @scene="state.chatScene = $event"
          @history="handleConversationSelect" @new="newConversation" @settings="openSettings"
          @review="router.push({ path: '/wordbank', query: { scope: 'review' } })"
        >
          <template #message-extra="{ message, index }">
              <section
                v-if="message.learningCard"
                class="learning-advisor-card"
                :class="`card-${message.learningCard.type}`"
              >
                <header>
                  <div>
                    <span>{{
                      message.learningCard.type === "daily-route"
                        ? "LEARNING ROUTE"
                        : "VOCABULARY PROFILE"
                    }}</span>
                    <h3>{{ message.learningCard.title }}</h3>
                  </div>
                  <b v-if="message.learningCard.type === 'daily-route'">
                    {{
                      message.learningCard.items.filter(
                        (item) => item.status === "complete",
                      ).length
                    }}/{{ message.learningCard.items.length }}
                  </b>
                  <b v-else>{{ message.learningCard.totalElements }}</b>
                </header>
                <div class="learning-card-items">
                  <article
                    v-for="item in message.learningCard.items"
                    :key="item.key"
                    :class="{ 'is-complete': item.status === 'complete' }"
                  >
                    <i>{{
                      item.status === "complete"
                        ? "✓"
                        : message.learningCard.type === "daily-route"
                          ? "○"
                          : "Aa"
                    }}</i>
                    <div>
                      <strong>{{ item.label }}</strong
                      ><small>{{ item.detail }}</small>
                    </div>
                  </article>
                  <p
                    v-if="!message.learningCard.items.length"
                    class="learning-card-empty"
                  >
                    这个分组暂时没有单词。
                  </p>
                </div>
                <footer
                  v-if="
                    message.learningCard.type === 'vocabulary-summary' &&
                    message.learningCard.totalPages > 1
                  "
                  class="learning-card-pages"
                >
                  <button
                    type="button"
                    :disabled="message.learningCard.page <= 0"
                    @click="loadLearningCardPage(message, index, -1)"
                  >
                    上一页
                  </button>
                  <span
                    >{{ message.learningCard.page + 1 }} /
                    {{ message.learningCard.totalPages }}</span
                  >
                  <button
                    type="button"
                    :disabled="
                      message.learningCard.page >=
                      message.learningCard.totalPages - 1
                    "
                    @click="loadLearningCardPage(message, index, 1)"
                  >
                    下一页
                  </button>
                </footer>
              </section>
              <div v-if="message.actions?.length" class="learning-card-actions">
                <button
                  v-for="action in message.actions"
                  :key="action.id"
                  type="button"
                  @click="handleLearningAction(action)"
                >
                  {{ action.label }}<span>→</span>
                </button>
              </div>
          </template>
        </ChatStudio>
      </section>

      <section
        id="vocabulary"
        class="section"
        :class="{
          active: activeSection === 'vocabulary',
          'has-insight': quizPane === 'insight',
        }"
      >
        <section class="panel">
          <div class="panel-heading">
            <div>
              <h2 id="quiz-task-heading">选择正确释义</h2>
            </div>
            <select
              v-model="state.quizCategory"
              aria-label="选择闯关词库"
              @change="loadWords(false)"
            >
              <option value="">智能混合</option>
              <option
                v-for="category in state.quizCategories"
                :key="categoryValue(category)"
                :value="categoryValue(category)"
              >
                {{ categoryLabel(category) }}
              </option>
            </select>
          </div>
          <div class="word-quiz-shell">
            <div v-if="state.quizCompleted" class="quiz-complete">
              <div class="quiz-badge">完成</div>
              <h3>本轮单词闯关完成</h3>
              <p>
                已经完成
                {{ quizAnsweredCount }} 个单词，可以去复习错词队列继续巩固。
              </p>
              <button class="quiz-primary" type="button" @click="restartQuiz">
                再来一轮
              </button>
            </div>
            <div
              v-else-if="currentQuizWord"
              ref="quizWorkbench"
              class="quiz-workbench"
              :class="'shows-' + quizPane"
            >
              <article
                id="quiz-practice"
                class="word-quiz-card"
                :class="{ 'is-answered': quizResultForCurrent }"
                aria-labelledby="quiz-task-heading"
              >
                <div class="quiz-progress">
                  <span
                    >第 {{ state.quizIndex + 1 }} /
                    {{ state.words.length }} 题</span
                  >
                  <div class="quiz-progress-track">
                    <i :style="{ width: quizProgress + '%' }"></i>
                  </div>
                </div>
                <div class="quiz-study-stage">
                  <div class="quiz-word-shell">
                    <button
                      class="favorite-button quiz-favorite"
                      :class="{ 'is-active': currentQuizWord.favorite }"
                      type="button"
                      :aria-label="
                        (currentQuizWord.favorite ? '取消收藏 ' : '收藏 ') +
                        currentQuizWord.word
                      "
                      :aria-pressed="String(Boolean(currentQuizWord.favorite))"
                      @click="toggleFavorite(currentQuizWord)"
                    >
                      <AppIcon name="star" />
                    </button>
                    <div class="quiz-word" :key="currentQuizWord.id">
                      {{ currentQuizWord.word }}
                    </div>
                    <button
                      ref="quizInsightToggle"
                      class="quiz-insight-toggle"
                      type="button"
                      aria-controls="quiz-analysis"
                      :aria-expanded="quizPane === 'insight'"
                      :disabled="!quizResultForCurrent"
                      @click="
                        quizPane === 'insight'
                          ? closeQuizInsight()
                          : openQuizInsight()
                      "
                    >
                      <AppIcon name="spark" />
                      {{ quizPane === "insight" ? "收起解析" : "深度解析" }}
                      <span class="quiz-insight-chevron" aria-hidden="true"
                        >›</span
                      >
                    </button>
                  </div>
                  <div
                    class="quiz-example-slot"
                    :class="{ 'is-visible': quizResultForCurrent }"
                    :tabindex="quizResultForCurrent ? 0 : -1"
                    :aria-hidden="!quizResultForCurrent"
                    aria-label="完整词性释义与例句"
                  >
                    <div v-if="quizResultForCurrent" class="quiz-example-panel">
                      <dl class="quiz-definitions" aria-label="完整词性与释义">
                        <div
                          v-for="group in quizDefinitionGroups"
                          :key="group.partOfSpeech"
                          class="quiz-definition-group"
                        >
                          <dt>{{ group.partOfSpeech || "释义" }}</dt>
                          <dd>
                            <p v-for="meaning in group.meanings" :key="meaning">
                              {{ meaning }}
                            </p>
                          </dd>
                        </div>
                      </dl>
                      <p
                        class="quiz-example-en"
                        v-html="
                          highlightTerm(
                            quizExampleText(
                              currentQuizWord,
                              quizResultForCurrent,
                            ),
                            currentQuizWord.word,
                          )
                        "
                      ></p>
                      <p class="quiz-example-cn">
                        {{
                          quizExampleCn(currentQuizWord, quizResultForCurrent)
                        }}
                      </p>
                    </div>
                  </div>
                </div>
                <div class="quiz-feedback-slot" aria-live="polite">
                  <span
                    v-if="quizResultForCurrent?.nextReviewHint"
                    class="quiz-review-notice"
                    :class="{ 'is-wrong': !quizResultForCurrent.correct }"
                    ><AppIcon
                      :name="quizResultForCurrent.correct ? 'check' : 'review'"
                    />{{ quizResultForCurrent.nextReviewHint }}</span
                  >
                </div>
                <div class="quiz-options">
                  <button
                    v-for="(option, optionIndex) in quizOptions"
                    :key="option"
                    class="quiz-option"
                    :class="quizOptionClass(option)"
                    type="button"
                    :disabled="state.quizBusy || Boolean(quizResultForCurrent)"
                    @click="submitQuiz(option)"
                  >
                    <span class="option-letter">{{
                      String.fromCharCode(65 + optionIndex)
                    }}</span
                    ><span>{{ option }}</span>
                  </button>
                </div>
              </article>
              <QuizInsight
                v-if="quizPane === 'insight'"
                id="quiz-analysis"
                :key="currentQuizWord.id"
                :word="currentQuizWord"
                :detail="quizResultForCurrent ? wordDetailForCurrent : null"
                :source="modelLabel(wordDetailForCurrent?.data)"
                @request="(force) => loadWordDetail(currentQuizWord, force)"
                @close="closeQuizInsight"
              />
              <div class="quiz-nav">
                <button
                  class="quiz-secondary"
                  type="button"
                  @click="prevQuiz"
                  :disabled="state.quizIndex <= 0"
                >
                  上一题
                </button>
                <button
                  class="quiz-primary"
                  type="button"
                  :disabled="!quizResultForCurrent"
                  @click="nextQuiz"
                >
                  {{
                    state.quizIndex >= state.words.length - 1
                      ? "完成"
                      : "下一题"
                  }}
                </button>
              </div>
            </div>
            <div v-else class="quiz-empty">当前词库暂无单词。</div>
          </div>
        </section>
      </section>

      <section
        id="review"
        class="section"
        :class="{ active: activeSection === 'review' }"
      >
        <section class="panel">
          <div class="panel-heading">
            <div class="review-mode-switch" role="group" aria-label="复习类型"><button type="button" :class="{ active: state.reviewMode === 'today' }" :disabled="state.reviewBusy" @click="setReviewMode('today')">今日到期<span>{{ state.report.dueWords || 0 }}</span></button><button type="button" :class="{ active: state.reviewMode === 'mistakes' }" :disabled="state.reviewBusy" @click="setReviewMode('mistakes')">错词强化<span>{{ state.report.reviewWords || 0 }}</span></button></div>
            <button class="evidence-open" type="button" @click="evidenceOpen = true">学习变化<AppIcon name="arrow" /></button>
          </div>
          <div class="review-workspace">
            <div
              v-if="state.reviewCompleted"
              class="review-challenge-card is-complete"
            >
              <div class="quiz-badge">通过</div>
              <h3>本组复习已完成</h3>
              <dl class="review-summary">
                <div>
                  <dt>完成词数</dt>
                  <dd>{{ state.reviewTotal }}</dd>
                </div>
                <div>
                  <dt>答错次数</dt>
                  <dd>{{ state.reviewMistakes }}</dd>
                </div>
              </dl>
              <div class="review-card-actions">
                <button
                  class="settings-secondary"
                  type="button"
                  @click="loadReviews"
                >
                  再复习一组
                </button>
                <button
                  class="quiz-primary"
                  type="button"
                  @click="goSection('vocabulary')"
                >
                  继续学新词<AppIcon name="arrow" />
                </button>
              </div>
            </div>
            <div v-else-if="currentReviewWord" class="review-list">
              <article
                class="review-challenge-card"
                :class="{
                  'is-correct': state.lastReviewResult?.correct,
                  'is-wrong':
                    state.lastReviewResult && !state.lastReviewResult.correct,
                }"
              >
                <div class="quiz-progress">
                  <span>剩余 {{ reviewRemaining }} / {{ reviewTotal }} 词</span>
                  <div class="quiz-progress-track">
                    <i :style="{ width: reviewProgress + '%' }"></i>
                  </div>
                </div>
                <div class="review-prompt">
                  <button v-if="currentReviewWord.sourceTitle" class="review-source" type="button" @click="returnToArticle(currentReviewWord.sourceArticleId)"><AppIcon name="news" />来自阅读 · {{ currentReviewWord.sourceTitle }}<AppIcon name="arrow" /></button>
                  <div class="review-prompt-meta">
                    <span v-if="phonetic(currentReviewWord)">{{
                      phonetic(currentReviewWord)
                    }}</span>
                    <span v-if="currentReviewWord.partOfSpeech">{{
                      currentReviewWord.partOfSpeech
                    }}</span>
                    <span>错误 {{ currentReviewWord.wrongCount || 0 }} 次</span>
                  </div>
                  <div
                    v-if="!state.reviewPendingResult"
                    class="review-cloze"
                    v-html="reviewCloze(currentReviewWord)"
                  ></div>
                  <div v-else class="review-filled-example">
                    <p
                      v-html="
                        highlightTerm(
                          quizExampleText(
                            currentReviewWord,
                            state.lastReviewResult,
                          ),
                          currentReviewWord.sourceWord || state.lastReviewResult?.expectedWord ||
                            currentReviewWord.word,
                        )
                      "
                    ></p>
                    <span>{{
                      quizExampleCn(currentReviewWord, state.lastReviewResult)
                    }}</span>
                  </div>
                </div>
                <div
                  v-if="state.lastReviewResult"
                  class="review-result"
                  :class="
                    state.lastReviewResult.correct ? 'is-correct' : 'is-wrong'
                  "
                >
                  <strong>{{
                    state.lastReviewResult.correct ? "通过" : "继续巩固"
                  }}</strong>
                  <span>{{
                    strictReviewParts(state.lastReviewResult).join(" · ")
                  }}</span>
                  <span
                    >正确单词：{{
                      state.lastReviewResult.expectedWord ||
                      currentReviewWord.word
                    }}</span
                  >
                  <span
                    >正确释义：{{
                      state.lastReviewResult.expectedMeaning
                    }}</span
                  >
                  <small>{{ state.lastReviewResult.nextReviewHint }}</small>
                </div>
                <form
                  class="review-challenge-form"
                  :hidden="state.reviewPendingResult"
                  @submit.prevent="submitReview"
                >
                  <div class="review-input-grid">
                    <label>
                      <span>{{ currentReviewWord.sourceWord && currentReviewWord.sourceWord.toLowerCase() !== currentReviewWord.word.toLowerCase() ? '英文拼写（单词原形）' : '英文拼写' }}</span>
                      <input
                        v-model.trim="state.reviewForm.spellingAnswer"
                        autocomplete="off"
                        autocapitalize="none"
                        spellcheck="false"
                        placeholder="输入完整英文单词"
                        :disabled="state.reviewBusy"
                      />
                    </label>
                    <label>
                      <span>中文释义</span>
                      <input
                        v-model.trim="state.reviewForm.meaningAnswer"
                        autocomplete="off"
                        placeholder="输入一个释义关键词"
                        :disabled="state.reviewBusy"
                      />
                    </label>
                  </div>
                  <div class="review-card-actions">
                    <button
                      class="quiz-primary"
                      type="submit"
                      :disabled="state.reviewBusy"
                    >
                      {{ state.reviewBusy ? "提交中..." : "提交复习" }}
                    </button>
                  </div>
                </form>
                <div
                  v-if="state.reviewPendingResult"
                  class="review-card-actions review-next-actions"
                >
                  <button
                    class="quiz-primary"
                    type="button"
                    @click="advanceReview"
                  >
                    下一题
                  </button>
                </div>
              </article>
            </div>
            <div v-else class="review-empty-state">
              <AppIcon name="check" />
              <h3>{{ state.reviewMode === 'today' ? '当前到期任务已完成' : '当前没有待强化的错词' }}</h3><p class="review-empty-note">{{ state.reviewMode === 'today' ? '到复习时间后，单词会再次出现在这里。' : '单词练习中的错词会自动加入这里。' }}</p><button v-if="state.reviewMode === 'today' && state.report.reviewWords" class="settings-secondary" type="button" @click="setReviewMode('mistakes')">提前练习错词</button>
              <button
                class="quiz-primary"
                type="button"
                @click="goSection('vocabulary')"
              >
                去学新词<AppIcon name="arrow" />
              </button>
            </div>
          </div>
        </section>
      </section>

      <section
        id="writing"
        class="section"
        :class="{ active: activeSection === 'writing' }"
      >
        <WritingStudio v-if="state.currentUser" :user-id="state.currentUser.id" @reviewed="writingReviewed" @settings="openSettings" />
      </section>

      <section
        id="news"
        class="section"
        :class="{
          active: activeSection === 'news',
          'is-browsing': newsLibraryOpen,
        }"
      >
        <div class="news-layout">
          <section id="news-library" class="panel news-library">
            <div class="panel-heading news-heading">
              <h2><AppIcon name="news" /> 阅读推荐</h2>
              <span class="news-library-count">{{ state.newsList.length }} 篇</span>
              <button
                ref="newsLibraryCloseRef"
                class="reading-library-close text-button"
                type="button"
                @click="newsLibraryOpen = false"
              >
                返回阅读<AppIcon name="arrow" />
              </button>
            </div>
            <div class="news-filter-stack">
              <div class="reading-filter-block">
                <span class="reading-filter-label">难度</span>
                <div class="reading-segment" role="group" aria-label="阅读难度">
                  <button
                    v-for="item in newsDifficultyOptions"
                    :key="item.value"
                    type="button"
                    class="reading-filter-pill"
                    :class="{
                      'is-active': state.newsDifficulty === item.value,
                    }"
                    :aria-pressed="state.newsDifficulty === item.value"
                    @click="setNewsDifficulty(item.value)"
                  >
                    {{ item.label }}
                  </button>
                </div>
              </div>
              <div class="reading-filter-block">
                <span class="reading-filter-label">主题</span>
                <div class="news-topic-grid" role="group" aria-label="阅读主题">
                  <button
                    v-for="topic in visibleNewsTopics"
                    :key="topic.value"
                    type="button"
                    class="news-topic-chip"
                    :class="{ 'is-active': state.newsTopic === topic.value }"
                    :aria-pressed="state.newsTopic === topic.value"
                    @click="setNewsTopic(topic.value)"
                  >
                    {{ topic.label }}
                  </button>
                  <button
                    v-if="newsTopicOptions.length > 8"
                    type="button"
                    class="news-topic-more"
                    :aria-expanded="showAllNewsTopics"
                    @click="showAllNewsTopics = !showAllNewsTopics"
                  >
                    {{ showAllNewsTopics ? "收起主题" : "更多主题 +" }}
                  </button>
                </div>
              </div>
            </div>
            <div class="news-list">
              <article
                tabindex="0"
                role="button"
                @keydown.enter="loadArticle(article.id)"
                @keydown.space.prevent="loadArticle(article.id)"
                v-for="article in state.newsList"
                :key="article.id"
                class="news-item"
                :class="{
                  'is-active': state.currentArticle?.id === article.id,
                }"
                :aria-pressed="state.currentArticle?.id === article.id"
                @click="loadArticle(article.id)"
              >
                <div class="news-item-topline">
                  <span>{{ newsTopicLabel(splitArticleTopics(article.topicTags)[0]) || '精选' }}</span>
                  <span v-if="article.finished" class="news-item-finished"><AppIcon name="check" />已读</span>
                  <span v-else>{{ articleReadingTime(article) }} 分钟</span>
                </div>
                <strong>{{ article.title }}</strong>
                <p>{{ article.summary }}</p>
                <div class="news-item-bottomline">
                  <span class="news-level" :data-level="article.difficulty">{{ difficultyLabel(article.difficulty) }}</span>
                  <span v-if="article.readProgress && !article.finished" class="news-item-progress">已读 {{ article.readProgress }}%</span>
                  <AppIcon name="arrow" />
                </div>
              </article>
              <p v-if="!state.newsList.length" class="news-empty">
                当前筛选暂无文章，换个难度或主题试试。
              </p>
            </div>
          </section>
          <section class="panel reader-panel" :aria-busy="state.articleBusy">
            <header class="reader-cover">
              <div class="reader-cover-meta">
                <span class="reader-source">{{ state.currentArticle?.sourceName || "LinguaMate Reader" }}</span>
                <span v-if="state.currentArticle" class="reader-topic">{{ articleTopicText(state.currentArticle.topicTags) }}</span>
              </div>
              <div class="reader-cover-title">
                <div>
                  <h2 :key="state.currentArticle?.id">{{ state.currentArticle?.title || "从一个感兴趣的主题开始" }}</h2>
                  <p v-if="state.currentArticle?.summary" class="reader-deck">{{ state.currentArticle.summary }}</p>
                  <div v-if="state.currentArticle" class="reader-byline">
                    <span class="news-level" :data-level="state.currentArticle.difficulty">{{ difficultyLabel(state.currentArticle.difficulty) }}</span>
                    <span>{{ articleWordCount }} 词</span>
                    <span>约 {{ articleReadingTime(state.currentArticle) }} 分钟</span>
                  </div>
                </div>
                <div class="reader-cover-art" aria-hidden="true">
                  <span class="reader-art-orbit"></span><span class="reader-art-orbit"></span>
                  <span class="reader-art-glyph">Aa<span>·</span></span>
                  <span class="reader-art-line"></span><span class="reader-art-spark">✦</span>
                </div>
              </div>
            </header>
            <div class="reader-tools">
              <span class="reader-hint"><AppIcon name="spark" />点选单词，即时查词</span>
              <div class="reader-toolbar">
                <button class="reader-discuss" type="button" :disabled="!state.currentArticle || state.chatBusy || state.articleBusy" @click="discussArticle"><AppIcon name="chat" />和 AI 讨论</button>
                <button
                  ref="newsLibraryButtonRef"
                  class="reading-library-toggle settings-secondary"
                  type="button"
                  aria-controls="news-library"
                  :aria-expanded="newsLibraryOpen"
                  @click="newsLibraryOpen = true"
                >
                  更换文章
                </button>
                <button
                  class="translate-btn"
                  type="button"
                  :disabled="!state.currentArticle || state.translateBusy || state.articleBusy"
                  @click="translateArticle"
                >
                  <span class="reader-translate-symbol" aria-hidden="true">译</span>
                  {{ state.translateBusy ? "翻译中…" : state.translation.contentCn || state.translation.titleCn ? "收起译文" : "全文翻译" }}
                </button>
              </div>
            </div>
            <div
              ref="readerRef"
              class="reader-scroll-area"
              @scroll="handleReaderScroll"
            >
              <article :key="state.currentArticle?.id" class="reader-content" aria-label="英文正文">
                <template v-for="token in articleTokens" :key="token.key">
                  <span
                    v-if="token.word"
                    class="reader-word"
                    :class="{ 'is-looked-up': state.tooltip.visible && state.tooltip.lookupKey === normalizeLookupWord(token.text) }"
                    tabindex="0"
                    :data-sentence="token.sentence"
                    @focus="showTooltip(token.text, $event)"
                    @blur="scheduleHideTooltip"
                    @click="showTooltip(token.text, $event)"
                    @keydown.enter.prevent="focusLookupActions(token.text, $event)"
                    @mouseenter="showTooltip(token.text, $event)"
                    @mouseleave="scheduleHideTooltip"
                    >{{ token.text }}</span
                  >
                  <span v-else>{{ token.text }}</span>
                </template>
              </article>
              <div v-if="state.currentArticle" class="reader-endmark" aria-hidden="true"><span></span>✦<span></span></div>
              <div
                v-if="state.translation.titleCn || state.translation.contentCn"
                class="translation-area"
              >
                <h3>{{ state.translation.titleCn }}</h3>
                <p>{{ state.translation.contentCn }}</p>
              </div>
            </div>
            <footer class="reader-progress">
              <span class="reader-progress-label">{{ state.currentArticle?.finished ? "本篇已读完" : "阅读进度" }}</span>
              <div class="reader-progress-track" role="progressbar" aria-label="阅读进度" :aria-valuenow="readerProgress" aria-valuemin="0" aria-valuemax="100">
                <span :style="{ width: readerProgress + '%' }"></span>
              </div>
              <span>{{ readerProgress }}%</span>
              <button class="reader-finish-btn" type="button" :disabled="!state.currentArticle || state.readingBusy || state.articleBusy || state.currentArticle.finished" @click="finishReading">
                <AppIcon name="check" />{{ state.readingBusy ? "保存中…" : state.currentArticle?.finished ? "已完成" : "完成阅读" }}
              </button>
            </footer>
          </section>
        </div>
      </section>

      <section
        id="wordbank"
        class="section"
        :class="{ active: activeSection === 'wordbank' }"
      >
        <section class="panel wordbank-layout">
          <div class="panel-heading">
            <div class="wordbank-scopes" role="group" aria-label="词库分组">
              <button
                v-for="scope in wordBankScopes"
                :key="scope.id"
                type="button"
                :class="{ 'is-active': state.wordBank.scope === scope.id }"
                :aria-pressed="state.wordBank.scope === scope.id"
                aria-controls="wordbank-results"
                @click="setWordBankScope(scope.id)"
              >
                {{ scope.label }}<span class="wordbank-scope-count">{{ state.wordBank.stats[scope.id] ?? '—' }}</span>
              </button>
            </div>
            <div class="wordbank-heading-actions">
              <button v-if="state.wordBank.scope === 'review' && state.wordBank.stats.review" type="button" class="wordbank-start-review" @click="router.push('/review')">开始复习<AppIcon name="arrow" /></button>
              <span class="energy-pill">{{
              state.wordBank.loading
                ? "正在加载"
                : state.wordBank.total + " 个单词"
              }}</span>
            </div>
          </div>
          <div class="wordbank-filters">
            <div class="wordbank-categories" role="group" aria-label="词库分类">
              <button
                class="review-tab"
                :class="{ 'is-active': !state.wordBank.category }"
                type="button"
                @click="setWordBankCategory('')"
              >
                全部
              </button>
              <button
                v-for="category in state.wordBank.categories"
                :key="categoryValue(category)"
                class="review-tab"
                :class="{
                  'is-active':
                    state.wordBank.category === categoryValue(category),
                }"
                type="button"
                @click="setWordBankCategory(categoryValue(category))"
              >
                {{ categoryLabel(category) }}
              </button>
            </div>
            <input
              v-model.trim="state.wordBank.search"
              class="wordbank-search"
              aria-label="搜索单词"
              autocomplete="off"
              placeholder="搜索英文前缀或中文释义前缀"
              @input="scheduleWordBankSearch"
              @keyup.enter="loadWordBankWords(0)"
            />
            <button
              class="settings-secondary"
              type="button"
              @click="loadWordBankWords(0)"
            >
              搜索
            </button>
          </div>
          <div
            id="wordbank-results"
            ref="wordBankGrid"
            class="word-strip word-bank-grid"
            :class="{ 'is-loading': state.wordBank.loading }"
            :aria-busy="state.wordBank.loading"
            :inert="state.wordBank.loading"
          >
            <article
              v-for="word in state.wordBank.words"
              :key="word.id"
              class="word-card vue-clickable"
              tabindex="0"
              role="button"
              @keydown.enter.self="state.selectedWord = word"
              @keydown.space.prevent.self="state.selectedWord = word"
              @click="state.selectedWord = word"
            >
              <div class="word-card-head">
                <div>
                  <strong>{{ word.word }}</strong>
                  <span class="word-meta"
                    >{{ phonetic(word) }} ·
                    {{ word.partOfSpeech || word.levelTag }}</span
                  >
                </div>
                <button
                  class="favorite-button"
                  :class="{ 'is-active': word.favorite }"
                  type="button"
                  :aria-label="
                    (word.favorite ? '取消收藏 ' : '收藏 ') + word.word
                  "
                  :aria-pressed="String(Boolean(word.favorite))"
                  @click.stop="toggleFavorite(word)"
                >
                  <AppIcon name="star" />
                </button>
              </div>
              <p>{{ word.meaningCn }}</p>
              <div class="word-card-example">
                <p>{{ wordCardExampleEn(word) }}</p>
                <span>{{ wordCardExampleCn(word) }}</span>
              </div>
            </article>
            <div
              v-if="!state.wordBank.loading && !state.wordBank.words.length"
              class="wordbank-empty-state"
            >
              <AppIcon name="wordbank" />
              <h3>{{ wordBankEmptyText }}</h3>
              <button
                v-if="
                  state.wordBank.scope !== 'all' ||
                  state.wordBank.search ||
                  state.wordBank.category
                "
                class="settings-secondary"
                type="button"
                @click="resetWordBankFilters"
              >
                查看全部单词
              </button>
            </div>
          </div>
          <div class="wordbank-pagination">
            <span class="wordbank-page-info"
              >第 {{ state.wordBank.page + 1 }} 页 /
              {{ wordBankTotalPages }} 页</span
            >
            <button
              class="wordbank-page-btn"
              type="button"
              :disabled="state.wordBank.loading || state.wordBank.page <= 0"
              @click="loadWordBankWords(state.wordBank.page - 1)"
            >
              上一页
            </button>
            <button
              class="wordbank-page-btn"
              type="button"
              :disabled="
                state.wordBank.loading ||
                state.wordBank.page >= wordBankTotalPages - 1
              "
              @click="loadWordBankWords(state.wordBank.page + 1)"
            >
              下一页
            </button>
          </div>
        </section>
      </section>
    </main>

    <button
      class="settings-fab desktop-account-legacy"
      type="button"
      aria-label="我的 API 设置"
      title="我的 API 设置"
      @click="openSettings"
    >
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path
          d="M19.14 12.94c.04-.31.06-.63.06-.94s-.02-.63-.06-.94l2.03-1.58a.5.5 0 0 0 .12-.64l-1.92-3.32a.5.5 0 0 0-.6-.22l-2.39.96a7.1 7.1 0 0 0-1.63-.94l-.36-2.54A.5.5 0 0 0 13.9 1h-3.8a.5.5 0 0 0-.49.41l-.36 2.54c-.58.22-1.12.53-1.63.94l-2.39-.96a.5.5 0 0 0-.6.22L2.71 7.47a.5.5 0 0 0 .12.64l2.03 1.58c-.04.31-.06.63-.06.94s.02.63.06.94L2.83 13.15a.5.5 0 0 0-.12.64l1.92 3.32a.5.5 0 0 0 .6.22l2.39-.96c.5.41 1.05.73 1.63.94l.36 2.54a.5.5 0 0 0 .49.41h3.8a.5.5 0 0 0 .49-.41l.36-2.54c.58-.22 1.12-.53 1.63-.94l2.39.96a.5.5 0 0 0 .6-.22l1.92-3.32a.5.5 0 0 0-.12-.64l-2.03-1.58ZM12 15.2A3.2 3.2 0 1 1 12 8.8a3.2 3.2 0 0 1 0 6.4Z"
        />
      </svg>
    </button>
    <div
      class="settings-backdrop"
      :class="{ 'is-open': state.settingsOpen }"
      aria-hidden="true"
      @click="state.settingsOpen = false"
    ></div>
    <aside
      ref="settingsPanelRef"
      :inert="!state.settingsOpen"
      class="settings-panel"
      :class="{ 'is-open': state.settingsOpen }"
      role="dialog"
      aria-modal="true"
      aria-labelledby="settingsTitle"
      :aria-hidden="String(!state.settingsOpen)"
    >
      <div class="settings-panel-head">
        <div>
          <p class="eyebrow compact">我的 API 设置</p>
          <h2 id="settingsTitle">模型连接</h2>
        </div>
        <button
          class="settings-close"
          type="button"
          aria-label="关闭设置"
          @click="state.settingsOpen = false"
        >
          ×
        </button>
      </div>
      <p class="settings-note">
        这里保存的是当前账号自己的接口地址、密钥和模型名，不会同步到其他用户。
      </p>
      <div class="settings-form">
        <label class="settings-field">
          <span>模型地址</span>
          <input
            v-model="state.ai.baseUrl"
            :disabled="state.aiBusy"
            autocomplete="off"
            placeholder="https://api.openai.com/v1"
          />
        </label>
        <label class="settings-field">
          <span>API 密钥</span>
          <input
            v-model="state.ai.apiKey"
            :disabled="state.aiBusy"
            type="password"
            autocomplete="off"
            placeholder="留空则沿用已保存密钥"
          />
        </label>
        <div class="settings-row">
          <button
            class="settings-secondary"
            type="button"
            :disabled="state.aiBusy"
            @click="fetchModels"
          >
            获取模型
          </button>
          <span class="settings-status">{{ state.ai.status }}</span>
        </div>
        <label class="settings-field">
          <span>模型名</span>
          <input v-model.trim="state.ai.chatModel" list="available-ai-models" placeholder="输入模型名，或获取模型后选择" :disabled="state.aiBusy" /><datalist id="available-ai-models"><option v-for="model in state.ai.models" :key="model" :value="model" /></datalist>
        </label>
        <div class="settings-actions">
          <button class="settings-secondary" type="button" :disabled="state.aiBusy || !state.ai.chatModel" @click="testAiConnection">{{ state.aiTesting ? '检测中…' : '测试连接' }}</button>
          <button
            class="settings-primary"
            type="button"
            :disabled="state.aiBusy"
            @click="saveAiSettings"
          >
            保存并应用
          </button>
        </div>
      </div>
    </aside>

    <div
      class="wordbank-dialog-backdrop"
      :class="{ 'is-open': wordDialogVisible }"
      aria-hidden="true"
      @click="state.selectedWord = null"
    ></div>
    <aside
      ref="wordDialogRef"
      :inert="!wordDialogVisible"
      class="wordbank-dialog"
      :class="{ 'is-open': wordDialogVisible }"
      role="dialog"
      aria-modal="true"
      aria-labelledby="wordBankDialogTitle"
      :aria-hidden="String(!wordDialogVisible)"
    >
      <div v-if="state.selectedWord" class="wordbank-dialog-shell">
        <div class="wordbank-dialog-head">
          <div>
            <p class="eyebrow compact">单词详情</p>
            <h2 id="wordBankDialogTitle">{{ state.selectedWord.word }}</h2>
            <p class="word-meta">
              {{ phonetic(state.selectedWord) }} ·
              {{ state.selectedWord.partOfSpeech }} ·
              {{ state.selectedWord.levelTag }}
            </p>
          </div>
          <div class="wordbank-dialog-actions">
            <button
              class="favorite-button wordbank-dialog-favorite"
              :class="{ 'is-active': state.selectedWord.favorite }"
              type="button"
              :aria-label="
                (state.selectedWord.favorite ? '取消收藏 ' : '收藏 ') +
                state.selectedWord.word
              "
              :aria-pressed="String(Boolean(state.selectedWord.favorite))"
              @click="toggleFavorite(state.selectedWord)"
            >
              <AppIcon name="star" />
            </button>
            <button
              class="wordbank-dialog-close"
              type="button"
              aria-label="关闭"
              @click="state.selectedWord = null"
            >
              ×
            </button>
          </div>
        </div>
        <div class="wordbank-dialog-meaning">
          <span>中文释义</span>
          <p>{{ state.selectedWord.meaningCn }}</p>
        </div>
        <div class="wordbank-dialog-example">
          <span>例句</span>
          <p
            v-html="
              highlightTerm(
                wordCardExampleEn(state.selectedWord),
                state.selectedWord.word,
              )
            "
          ></p>
          <small>{{ wordCardExampleCn(state.selectedWord) }}</small>
        </div>
        <div class="wordbank-dialog-detail-actions">
          <button
            class="quiz-detail-button"
            :class="{
              'is-loading': selectedWordDetail?.status === 'loading',
              'is-done': selectedWordDetail?.status === 'ready',
            }"
            type="button"
            :disabled="selectedWordDetail?.status === 'loading'"
            @click="loadWordDetail(state.selectedWord)"
          >
            {{
              selectedWordDetail?.status === "loading"
                ? "生成中..."
                : selectedWordDetail?.status === "ready"
                  ? "已生成解析"
                  : "深度解析"
            }}
          </button>
        </div>
        <section
          v-if="selectedWordDetail"
          class="quiz-ai-detail wordbank"
          :class="{
            'is-loading': selectedWordDetail.status === 'loading',
            'is-error': selectedWordDetail.status === 'error',
          }"
        >
          <template v-if="selectedWordDetail.status === 'loading'">
            <span class="quiz-ai-spinner" aria-hidden="true"></span>
            <span>正在生成 {{ state.selectedWord.word }} 的深度解析...</span>
          </template>
          <template v-else-if="selectedWordDetail.status === 'error'">
            <span>{{
              selectedWordDetail.message ||
              "模型未配置或暂时不可用，请检查 AI 设置"
            }}</span>
            <button
              class="quiz-detail-button retry-detail-button"
              type="button"
              @click="loadWordDetail(state.selectedWord, true)"
            >
              重试
            </button>
          </template>
          <template v-else>
            <div class="quiz-ai-head">
              <strong
                >深度解析 ·
                {{
                  selectedWordDetail.data?.word || state.selectedWord.word
                }}</strong
              >
              <span
                >{{ modelLabel(selectedWordDetail.data)
                }}{{ selectedWordDetail.data?.cached ? " · 已缓存" : "" }}</span
              >
            </div>
            <div class="quiz-ai-grid">
              <section
                v-if="selectedWordDetail.data?.examples?.length"
                class="quiz-ai-section"
              >
                <h3>更多例句</h3>
                <div
                  v-for="example in selectedWordDetail.data.examples"
                  :key="example.en"
                  class="quiz-ai-row"
                >
                  <p
                    v-html="
                      highlightTerm(example.en || '', state.selectedWord.word)
                    "
                  ></p>
                  <small v-if="example.cn">{{ example.cn }}</small>
                </div>
              </section>
              <section
                v-if="selectedWordDetail.data?.variants?.length"
                class="quiz-ai-section"
              >
                <h3>词性变种</h3>
                <div
                  v-for="variant in selectedWordDetail.data.variants"
                  :key="`${variant.form}-${variant.type}`"
                  class="quiz-ai-row"
                >
                  <p>
                    <b>{{ variant.form }}</b> {{ variant.type }}
                  </p>
                  <small v-if="variant.meaningCn">{{
                    variant.meaningCn
                  }}</small>
                </div>
              </section>
              <section
                v-if="selectedWordDetail.data?.usageNotes?.length"
                class="quiz-ai-section"
              >
                <h3>用法提示</h3>
                <div
                  v-for="note in selectedWordDetail.data.usageNotes"
                  :key="note"
                  class="quiz-ai-row"
                >
                  <p>{{ note }}</p>
                </div>
              </section>
              <section
                v-if="selectedWordDetail.data?.collocations?.length"
                class="quiz-ai-section"
              >
                <h3>固定搭配</h3>
                <div
                  v-for="item in selectedWordDetail.data.collocations"
                  :key="`${item.phrase}-${item.meaningCn}`"
                  class="quiz-ai-row"
                >
                  <p>
                    <b>{{ item.phrase }}</b> {{ item.meaningCn }}
                  </p>
                  <small
                    v-if="item.exampleEn"
                    v-html="
                      highlightTerm(item.exampleEn, state.selectedWord.word)
                    "
                  ></small>
                </div>
              </section>
            </div>
          </template>
        </section>
      </div>
    </aside>

    <LearningEvidence :open="evidenceOpen" :evidence="state.report.evidence || {}" @close="evidenceOpen = false" @practice="goSection('review')" />
    <div
      v-if="state.tooltip.visible"
      ref="tooltipRef"
      class="word-tooltip visible"
      role="dialog"
      :aria-label="state.tooltip.data.word + ' 的释义'"
      :style="{ left: state.tooltip.x + 'px', top: state.tooltip.y + 'px' }"
      @mouseenter="cancelTooltipHide"
      @mouseleave="scheduleHideTooltip"
      @focusin="cancelTooltipHide"
      @focusout="handleTooltipFocusOut"
      @keydown.esc.stop="state.tooltip.visible = false"
    >
      <div class="tooltip-head">
        <strong>{{ state.tooltip.data.word }}</strong>
        <span v-if="state.tooltip.loading" class="tooltip-status">查询中</span>
      </div>
      <div
        v-if="phonetic(state.tooltip.data) || state.tooltip.data.partOfSpeech"
        class="tooltip-meta"
      >
        <span v-if="phonetic(state.tooltip.data)" class="phonetic">{{
          phonetic(state.tooltip.data)
        }}</span>
        <span v-if="state.tooltip.data.partOfSpeech" class="tooltip-pos">{{
          state.tooltip.data.partOfSpeech
        }}</span>
      </div>
      <section v-if="state.tooltip.context" class="tooltip-context"><span>{{ state.tooltip.context.source === 'model' ? '本文中的意思 · AI' : '通用词典释义' }}</span><p>{{ state.tooltip.context.meaning }}</p><small>{{ state.tooltip.context.explanation }}</small><div v-if="state.tooltip.context.phrase" class="tooltip-phrase"><b>{{ state.tooltip.context.phrase }}</b><span>{{ state.tooltip.context.phraseMeaning }}</span></div></section>
      <p v-else>{{ state.tooltip.data.meaningCn || "暂无释义" }}</p>
      <blockquote v-if="state.tooltip.sentence" class="tooltip-original-sentence">{{ state.tooltip.sentence }}</blockquote>
      <button class="tooltip-explain" type="button" :disabled="state.tooltip.loading || state.tooltip.contextBusy || !state.tooltip.sentence" @click="explainReaderWord"><AppIcon name="spark" />{{ state.tooltip.contextBusy ? '正在解读语境…' : state.tooltip.context ? '重新解读语境' : 'AI 语境解读' }}</button>
      <small v-if="!state.tooltip.sentence && state.tooltip.data.exampleEn">{{
        state.tooltip.data.exampleEn
      }}</small>
      <div class="tooltip-actions">
        <button class="tooltip-save-word" type="button" :class="{ 'is-saved': state.tooltip.data.savedInReviewBook }"
          :disabled="state.tooltip.loading || state.tooltip.saving || state.tooltip.data.savedInReviewBook || !tooltipHasDefinition"
          @click="saveReaderWord(state.tooltip.data.word)">
          <AppIcon :name="state.tooltip.data.savedInReviewBook ? 'check' : 'review'" />
          {{ state.tooltip.loading ? "查询中…" : state.tooltip.saving ? "加入中…" : state.tooltip.data.savedInReviewBook ? "已加入待复习" : tooltipHasDefinition ? "加入待复习" : "暂无可保存词条" }}
        </button>
        <button v-if="state.tooltip.data.savedInReviewBook" type="button" class="tooltip-library-link" @click="openPendingLibrary">查看词库<AppIcon name="arrow" /></button>
        <button v-if="state.tooltip.data.savedInReviewBook && state.tooltip.sentence && !state.tooltip.contextSaved" type="button" class="tooltip-library-link" :disabled="state.tooltip.saving" @click="saveReaderWord(state.tooltip.data.word)">保留本文原句<AppIcon name="arrow" /></button>
      </div>
    </div>
  </div>
</template>

<script setup>
import AppIcon from "@/components/AppIcon.vue";
import ChatStudio from "@/components/ChatStudio.vue";
import WritingStudio from "@/components/WritingStudio.vue";
import LearningEvidence from "@/components/LearningEvidence.vue";
import { readingTokens } from "@/reading-context";
import LearningStudioArt from "@/components/LearningStudioArt.vue";
import WordAtmosphere from "@/components/WordAtmosphere.vue";
import UserAccountMenu from "@/components/UserAccountMenu.vue";
import ThemeToggle from "@/components/ThemeToggle.vue";
import QuizInsight from "@/components/QuizInsight.vue";
import { wordDefinitionGroups } from "@/word-definitions";
import { ElMessage } from "element-plus";
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from "vue";
import { useRoute, useRouter } from "vue-router";
import { aiApi, authApi, learningApi } from "@/api/modules";
import { formatDate, normalizeModelList } from "@/api/client";
import {
  learningMessageFromResponse,
  learningMessageFromHistory,
  nextVocabularyPage,
} from "@/learning-advisor";

const route = useRoute();
const router = useRouter();
const readerRef = ref(null);
const tooltipRef = ref(null);
const wordBankGrid = ref(null);
const quizPane = ref("practice");
const quizInsightToggle = ref(null);
const quizWorkbench = ref(null);
let quizMotionId = 0;
let quizTransition = null;
let quizAnimations = [];
const dashboardExpanded = ref(false);
const evidenceOpen = ref(false);
const newsLibraryOpen = ref(false);
const newsLibraryButtonRef = ref(null);
const newsLibraryCloseRef = ref(null);
const showAllNewsTopics = ref(false);
const settingsPanelRef = ref(null);
const wordDialogRef = ref(null);
let previousFocus;
let settingsReturnFocus;
function closeOverlays() {
  state.settingsOpen = false;
  state.selectedWord = null;
  state.tooltip.visible = false;
  newsLibraryOpen.value = false;
}
function trapDialogFocus(event) {
  if (
    event.key === "Escape" &&
    (state.settingsOpen || wordDialogVisible.value)
  ) {
    event.preventDefault();
    closeOverlays();
    return;
  }
  if (event.key !== "Tab") return;
  const dialog = state.settingsOpen
    ? settingsPanelRef.value
    : wordDialogVisible.value
      ? wordDialogRef.value
      : null;
  if (!dialog) return;
  const controls = [
    ...dialog.querySelectorAll(
      "button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea, a[href]",
    ),
  ];
  const first = controls[0],
    last = controls.at(-1);
  if (
    event.shiftKey &&
    (document.activeElement === first ||
      !dialog.contains(document.activeElement))
  ) {
    event.preventDefault();
    last?.focus();
  } else if (
    !event.shiftKey &&
    (document.activeElement === last ||
      !dialog.contains(document.activeElement))
  ) {
    event.preventDefault();
    first?.focus();
  }
}

const todayLabel = new Intl.DateTimeFormat("zh-CN", {
  month: "long",
  day: "numeric",
  weekday: "long",
  timeZone: "Asia/Shanghai",
}).format(new Date());

const navItems = [
  { id: "dashboard", label: "学习概览", path: "/" },
  { id: "chat", label: "对话练习", path: "/chat" },
  { id: "vocabulary", label: "单词练习", path: "/vocabulary" },
  { id: "review", label: "温故知新", path: "/review" },
  { id: "writing", label: "写作工坊", path: "/writing" },
  { id: "news", label: "阅读空间", path: "/news" },
  { id: "wordbank", label: "我的词库", path: "/wordbank" },
];

const wordBankScopes = [
  { id: "all", label: "全部单词" },
  { id: "review", label: "待复习" },
  { id: "favorites", label: "收藏词" },
  { id: "mastered", label: "已掌握" },
];

const newsDifficultyOptions = [
  { value: "", label: "全部" },
  { value: "easy", label: "简单" },
  { value: "medium", label: "进阶" },
  { value: "hard", label: "挑战" },
];

const chatSceneOptions = [
  { value: "daily", label: "日常交流" },
  { value: "exam", label: "考试口语" },
  { value: "business", label: "商务表达" },
  { value: "reading", label: "阅读讨论" },
];

const curatedNewsTopics = [
  { value: "", label: "全部" },
  { value: "story", label: "故事" },
  { value: "science", label: "科学" },
  { value: "technology", label: "科技" },
  { value: "culture", label: "文化" },
  { value: "health", label: "健康" },
  { value: "environment", label: "环境" },
  { value: "business", label: "商业" },
  { value: "education", label: "教育" },
  { value: "career", label: "职业" },
  { value: "friendship", label: "友谊" },
  { value: "honesty", label: "诚信" },
  { value: "kindness", label: "善意" },
  { value: "perseverance", label: "坚持" },
  { value: "wisdom", label: "智慧" },
  { value: "growth", label: "成长" },
];

const newsTopicLabelMap = {
  story: "故事",
  perseverance: "坚持",
  pride: "骄傲",
  honesty: "诚信",
  trust: "信任",
  kindness: "善意",
  strength: "力量",
  preparation: "准备",
  "hard-work": "勤奋",
  gratitude: "感恩",
  acceptance: "接纳",
  flexibility: "灵活",
  humility: "谦逊",
  friendship: "友谊",
  loyalty: "忠诚",
  greed: "贪婪",
  contentment: "知足",
  community: "社区",
  confidence: "自信",
  uniqueness: "独特",
  wisdom: "智慧",
  observation: "观察",
  courage: "勇气",
  integrity: "正直",
  freedom: "自由",
  clever: "机智",
  hope: "希望",
  sacrifice: "奉献",
  patience: "耐心",
  dedication: "投入",
  endurance: "耐力",
  "self-reflection": "自省",
  truth: "真相",
  growth: "成长",
  legacy: "传承",
  generosity: "慷慨",
  science: "科学",
  technology: "科技",
  culture: "文化",
  health: "健康",
  environment: "环境",
  business: "商业",
  education: "教育",
  career: "职业",
  travel: "旅行",
  lifestyle: "生活",
  history: "历史",
  responsibility: "责任",
  communication: "沟通",
  service: "服务",
  research: "研究",
};

const sectionMeta = {
  dashboard: "学习概览",
  chat: "对话练习",
  vocabulary: "单词练习",
  review: "温故知新",
  writing: "写作工坊",
  news: "阅读空间",
  wordbank: "我的词库",
};

const state = reactive({
  loading: true,
  currentUser: null,
  report: {},
  level: {},
  conversations: [],
  conversationId: null,
  messages: [],
  trace: [],
  chatScene: "daily",
  chatInput: "",
  chatBusy: false,
  chatMode: "practice",
  chatError: "",
  chatReadingArticle: null,
  coachWords: [],
  words: [],
  quizCategory: "",
  quizCategories: [],
  quizIndex: 0,
  quizAnswers: {},
  quizOptions: [],
  quizCompleted: false,
  quizBusy: false,
  wordDetails: {},
  reviewMode: "today",
  reviewWords: [],
  reviewIndex: 0,
  reviewTotal: 0,
  reviewMistakes: 0,
  reviewForm: { spellingAnswer: "", meaningAnswer: "" },
  reviewBusy: false,
  lastReviewResult: null,
  reviewPendingResult: false,
  reviewCompleted: false,
  writing: { title: "", examType: "general", targetLevel: "B1", essayText: "" },
  writingBusy: false,
  writingResult: null,
  newsTopics: [],
  newsList: [],
  newsDifficulty: "",
  newsTopic: "",
  currentArticle: null,
  articleBusy: false,
  readingBusy: false,
  readerProgress: 0,
  progressTimer: null,
  lookupCache: {},
  contextCache: {},
  savedOriginals: {},
  tooltipTimer: null,
  tooltip: {
    visible: false,
    loading: false,
    saving: false,
    contextBusy: false,
    context: null,
    sentence: "",
    contextKey: "",
    contextSaved: false,
    lookupKey: "",
    x: 0,
    y: 0,
    data: {},
  },
  translateBusy: false,
  translation: {},
  wordBank: {
    scope: "all",
    stats: {},
    loading: false,
    categories: [],
    words: [],
    total: 0,
    page: 0,
    size: 48,
    category: "",
    search: "",
  },
  selectedWord: null,
  settingsOpen: false,
  ai: { baseUrl: "", apiKey: "", chatModel: "", models: [], status: "未连接" },
  aiBusy: false,
  aiTesting: false,
  aiSaved: { baseUrl: '', chatModel: '' },
  logoutBusy: false,
  aiSummary: { label: "个人配置", modelName: "", loaded: false, loading: false },
});

const activeSection = computed(() => route.meta.section || "dashboard");
const tooltipHasDefinition = computed(() => {
  const meaning = state.tooltip.data.meaningCn || "";
  return Boolean(meaning.trim()) && !/未收录|查询中|查询失败/.test(meaning);
});
const sectionTitle = computed(
  () => sectionMeta[activeSection.value] || sectionMeta.dashboard,
);
const userName = computed(
  () => state.currentUser?.nickname || state.currentUser?.username || "学习者",
);
const currentConversation = computed(() =>
  state.conversations.find(
    (item) => Number(item.id) === Number(state.conversationId),
  ),
);
const conversationSelectValue = computed(() =>
  state.conversationId ? `${state.conversationId}` : "new",
);
const currentConversationTitle = computed(() => {
  if (currentConversation.value?.title) return currentConversation.value.title;
  return state.conversationId
    ? "学习对话"
    : `新对话 · ${sceneLabel(state.chatScene)}`;
});
const levelPercent = computed(() => {
  const next = Number(state.level.nextLevelXp || 100);
  return Math.min(
    100,
    Math.round((Number(state.level.experience || 0) / next) * 100),
  );
});
const routeDoneCount = computed(
  () => journeySteps.value.filter((step) => step.done).length,
);
const routeProgressPercent = computed(() =>
  Math.round(
    (routeDoneCount.value / Math.max(1, journeySteps.value.length)) * 100,
  ),
);
const journeyEnergy = computed(() =>
  Math.max(
    levelPercent.value,
    Math.round((routeDoneCount.value / journeySteps.value.length) * 100),
  ),
);
const focusStep = computed(
  () => journeySteps.value.find((step) => !step.done) || journeySteps.value[0],
);
const focusDescription = computed(() => {
  if (routeDoneCount.value === journeySteps.value.length) return "继续探索，或再练习一次喜欢的内容。";
  return {
    chat: "选一个生活场景，用英语聊起来。",
    vocabulary: `${state.words.length} 个单词，在语境中建立记忆。`,
    review: `${state.report.reviewWords || 0} 个待复习单词，巩固容易忘记的表达。`,
    writing: "写下一段英文，把想法表达得更清楚。",
    news: "读一篇文章，在真实内容里发现新表达。",
  }[focusStep.value?.section] || "选择一项练习，开始今天的学习。";
});
const currentQuizWord = computed(() => state.words[state.quizIndex]);
const quizResultForCurrent = computed(() =>
  currentQuizWord.value ? state.quizAnswers[currentQuizWord.value.id] : null,
);
const wordDetailForCurrent = computed(() =>
  currentQuizWord.value ? state.wordDetails[currentQuizWord.value.id] : null,
);
const quizDefinitionGroups = computed(() =>
  wordDefinitionGroups(
    currentQuizWord.value,
    wordDetailForCurrent.value?.status === "ready"
      ? wordDetailForCurrent.value.data
      : null,
  ),
);
const currentReviewWord = computed(() => state.reviewWords[state.reviewIndex]);
const selectedWordDetail = computed(() =>
  state.selectedWord ? state.wordDetails[state.selectedWord.id] : null,
);
const quizOptions = computed(() => state.quizOptions[state.quizIndex] || []);
const quizAnsweredCount = computed(
  () => Object.keys(state.quizAnswers || {}).length,
);
const quizProgress = computed(() => {
  const total = Math.max(1, state.words.length);
  return Math.round(
    ((state.quizIndex + (quizResultForCurrent.value ? 1 : 0)) / total) * 100,
  );
});
const reviewTotal = computed(() =>
  Math.max(1, state.reviewTotal || state.reviewWords.length),
);
const reviewRemaining = computed(() => state.reviewWords.length);
const reviewProgress = computed(() => {
  const total = reviewTotal.value;
  const passed = Math.max(0, total - reviewRemaining.value);
  return Math.round((passed / total) * 100);
});
const wordBankEmptyText = computed(() => {
  if (state.wordBank.search || state.wordBank.category)
    return "没有符合筛选的单词";
  if (state.wordBank.scope === "favorites") return "还没有收藏词";
  if (state.wordBank.scope === "mastered") return "还没有已掌握的单词";
  if (state.wordBank.scope === "review") return "暂时没有待复习的单词";
  return "词库暂时为空";
});
const newsTopicOptions = computed(() => {
  const topics = new Map();
  curatedNewsTopics.forEach((topic) => topics.set(topic.value, topic));
  (state.newsTopics || []).flatMap(splitArticleTopics).forEach((topic) => {
    if (!topics.has(topic)) {
      topics.set(topic, { value: topic, label: newsTopicLabel(topic) });
    }
  });
  return Array.from(topics.values());
});
const visibleNewsTopics = computed(() => {
  if (showAllNewsTopics.value) return newsTopicOptions.value;
  const topics = newsTopicOptions.value.slice(0, 8);
  const selected = newsTopicOptions.value.find(
    (topic) => topic.value === state.newsTopic,
  );
  if (selected && !topics.includes(selected)) topics.push(selected);
  return topics;
});
const readerProgress = computed(
  () => state.currentArticle?.readProgress ?? state.readerProgress ?? 0,
);
const wordBankTotalPages = computed(() =>
  Math.max(1, Math.ceil((state.wordBank.total || 0) / state.wordBank.size)),
);
const wordDialogVisible = computed({
  get: () => Boolean(state.selectedWord),
  set: (value) => {
    if (!value) state.selectedWord = null;
  },
});

const journeySteps = computed(() => {
  const report = state.report || {};
  const answeredWords = Object.keys(state.quizAnswers || {}).length;
  const reviewDone =
    Number(report.reviewWords || 0) === 0 || state.reviewWords.length === 0;
  const steps = [
    [
      "chat",
      "chat",
      "开口练",
      "会话营地",
      "说",
      "green",
      16,
      63,
      state.messages.some((item) => item.role === "user"),
      "完成一次问答",
    ],
    [
      "vocabulary",
      "vocabulary",
      "记单词",
      "词汇森林",
      "词",
      "blue",
      34,
      31,
      answeredWords >= Math.min(6, state.words.length || 6),
      `${answeredWords} 个已作答`,
    ],
    [
      "review",
      "review",
      "复习错词",
      "复习山口",
      "复",
      "amber",
      53,
      56,
      reviewDone,
      `${report.reviewWords || 0} 个待复习`,
    ],
    [
      "writing",
      "writing",
      "改作文",
      "写作工坊",
      "写",
      "coral",
      72,
      78,
      Number(report.essayCount || 0) > 0 || Boolean(state.writingResult),
      `${report.essayCount || 0} 篇已批改`,
    ],
    [
      "news",
      "news",
      "读文章",
      "阅读灯塔",
      "读",
      "violet",
      82,
      25,
      Number(report.readingCount || 0) > 0 ||
        Boolean(state.currentArticle?.finished),
      `${report.readingCount || 0} 篇记录`,
    ],
  ];
  const firstPending = steps.find((item) => !item[8])?.[0];
  return steps.map(
    ([
      id,
      section,
      title,
      landmark,
      symbol,
      tone,
      mapX,
      mapY,
      done,
      detail,
    ]) => ({
      id,
      section,
      title,
      landmark,
      symbol,
      tone,
      mapX,
      mapY,
      done,
      detail,
      recommended: id === firstPending,
    }),
  );
});

function routeStepAriaLabel(step) {
  const status = step.done
    ? "已完成"
    : step.recommended
      ? "推荐下一关"
      : "未完成";
  return `${step.title}，${step.landmark}，${step.detail}，${status}`;
}

const articleTokens = computed(() => readingTokens(state.currentArticle?.content || ""));

const articleWordCount = computed(() =>
  (state.currentArticle?.content?.match(/[A-Za-z][A-Za-z'-]*/g) || []).length,
);

function articleReadingTime(article) {
  const words = (article?.content || article?.summary || "").match(/[A-Za-z][A-Za-z'-]*/g) || [];
  return Math.max(1, Math.ceil(words.length / 180));
}

function phonetic(word) {
  const value = word?.phoneticUs || word?.phoneticUk || "";
  if (!value) return "";
  const clean = String(value).replaceAll("/", "");
  return `/${clean}/`;
}

function splitArticleTopics(topicTags) {
  return String(topicTags || "")
    .split(/[,，;；\s]+/)
    .map((topic) => topic.trim().toLowerCase())
    .filter(Boolean);
}

function newsTopicLabel(topic) {
  return newsTopicLabelMap[topic] || topic;
}

function articleTopicText(topicTags) {
  const topics = splitArticleTopics(topicTags).map(newsTopicLabel);
  if (!topics.length) return "综合";
  const shown = topics.slice(0, 3).join(" / ");
  return topics.length > 3 ? `${shown} 等` : shown;
}

function sceneLabel(value) {
  return (
    chatSceneOptions.find((item) => item.value === value)?.label ||
    value ||
    "日常交流"
  );
}

function difficultyLabel(value) {
  const option = newsDifficultyOptions.find((item) => item.value === value);
  return option?.label || value || "全部";
}

async function setNewsDifficulty(value) {
  if (state.newsDifficulty === value) return;
  state.newsDifficulty = value;
  await loadNews();
}

async function setNewsTopic(value) {
  if (state.newsTopic === value) return;
  state.newsTopic = value;
  await loadNews();
}

function goSection(section, query = {}) {
  const target = navItems.find((item) => item.id === section) || navItems[0];
  router.push({ path: target.path, query });
}

function openLearningAssistant() {
  state.chatMode = "assistant";
  goSection("chat");
}

async function handleLearningAction(action) {
  if (action.section === "review") await loadReviews();
  await goSection(action.section, action.query || {});
}

async function loadLearningCardPage(message, index, direction) {
  const card = message.learningCard;
  const page = nextVocabularyPage(card, direction);
  if (page === card.page) return;
  try {
    const result = await learningApi.advisorVocabulary({
      group: card.group,
      page,
      size: card.size,
    });
    state.messages[index] = learningMessageFromResponse(result);
  } catch (error) {
    ElMessage.error(error.message || "词库分页加载失败");
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

function sameLocalDay(value) {
  if (!value) return false;
  const date = new Date(String(value).replace(" ", "T"));
  const now = new Date();
  return (
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()
  );
}

async function loadReport() {
  const [report, level] = await Promise.all([
    learningApi.report(),
    learningApi.level(),
  ]);
  state.report = report || {};
  state.level = level || {};
}

function categoryValue(category) {
  return category?.tag || category?.code || "";
}

function categoryLabel(category) {
  const label = category?.label || categoryValue(category) || "未命名";
  return category?.count ? `${label} (${category.count})` : label;
}

function buildQuizOptions(word, index = 0) {
  if (!word) return [];
  const distractors = [];
  for (
    let offset = 1;
    offset < state.words.length && distractors.length < 3;
    offset += 1
  ) {
    const candidate = state.words[(index + offset) % state.words.length];
    if (
      candidate?.id !== word.id &&
      candidate?.meaningCn &&
      candidate.meaningCn !== word.meaningCn &&
      !distractors.includes(candidate.meaningCn)
    ) {
      distractors.push(candidate.meaningCn);
    }
  }
  [
    "清晰的；明确的",
    "自然的表达方式",
    "复习计划；学习安排",
    "阅读材料；文章",
  ].forEach((item) => {
    if (
      distractors.length < 3 &&
      item !== word.meaningCn &&
      !distractors.includes(item)
    ) {
      distractors.push(item);
    }
  });
  const options = [word.meaningCn, ...distractors.slice(0, 3)].filter(Boolean);
  const rotateBy = options.length
    ? Number(word.id || index) % options.length
    : 0;
  return [...options.slice(rotateBy), ...options.slice(0, rotateBy)];
}

async function loadQuizCategories() {
  state.quizCategories = await learningApi.quizCategories();
}

async function loadWords(updateToday = true) {
  state.words = await learningApi.todayWords({
    limit: 8,
    category: state.quizCategory,
  });
  state.quizIndex = 0;
  resetQuizPane();
  state.quizAnswers = {};
  state.quizOptions = state.words.map((word, index) =>
    buildQuizOptions(word, index),
  );
  state.quizCompleted = false;
  if (updateToday) await loadReport();
}

function prevQuiz() {
  if (state.quizIndex <= 0) return;
  state.quizIndex -= 1;
}

async function nextQuiz() {
  if (!quizResultForCurrent.value) return;
  if (state.quizIndex >= state.words.length - 1) {
    state.quizCompleted = true;
    return;
  }
  state.quizIndex += 1;
}

async function restartQuiz() {
  await loadWords(false);
}

function quizOptionClass(option) {
  const result = quizResultForCurrent.value;
  if (!result) return "";
  if (option === result.expectedMeaning) return "is-correct";
  if (option === result.selectedAnswer) return "is-wrong is-selected";
  return "";
}

async function submitQuiz(answer) {
  if (!currentQuizWord.value || quizResultForCurrent.value || state.quizBusy)
    return;
  const wordId = currentQuizWord.value.id;
  state.quizBusy = true;
  try {
    const result = await learningApi.quiz(wordId, { answer });
    state.quizAnswers[wordId] = { ...result, selectedAnswer: answer };
    applyWordProgress(result.wordId, result);
    await Promise.all([loadReport(), loadReviews()]);
  } catch (error) {
    ElMessage.error(error.message || "提交失败，请重试");
  } finally {
    state.quizBusy = false;
  }
}

async function loadWordDetail(word, force = false) {
  if (!word) return;
  if (state.wordDetails[word.id]?.status === "loading") return;
  if (state.wordDetails[word.id]?.status === "ready" && !force) return;
  state.wordDetails[word.id] = { status: "loading" };
  try {
    state.wordDetails[word.id] = {
      status: "ready",
      data: await learningApi.wordAiDetail(word.id),
    };
  } catch (error) {
    state.wordDetails[word.id] = {
      status: "error",
      message: error.message || "模型未配置或暂时不可用，请检查 AI 设置",
    };
    ElMessage.error(state.wordDetails[word.id].message);
  }
}

function openQuizInsight() {
  if (!quizResultForCurrent.value) return;
  changeQuizPane("insight");
  loadWordDetail(currentQuizWord.value);
}

async function closeQuizInsight() {
  await changeQuizPane("practice");
  if (quizPane.value === "practice") {
    quizInsightToggle.value?.focus({ preventScroll: true });
  }
}

function stopQuizMotion() {
  quizMotionId++;
  quizTransition?.skipTransition();
  quizTransition = null;
  quizAnimations.forEach((animation) => animation.cancel());
  quizAnimations = [];
}

function resetQuizPane() {
  stopQuizMotion();
  quizPane.value = "practice";
}

async function changeQuizPane(pane) {
  stopQuizMotion();
  const motionId = quizMotionId;
  const update = async () => {
    if (motionId !== quizMotionId) return;
    quizPane.value = pane;
    await nextTick();
  };
  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
    await update();
    return;
  }
  if (document.startViewTransition) {
    const transition = document.startViewTransition(update);
    quizTransition = transition;
    transition.ready.catch(() => {});
    await transition.finished.catch(() => {});
    if (quizTransition === transition) quizTransition = null;
    return;
  }

  // Keep both directions animated on browsers without View Transitions.
  const surface = quizWorkbench.value;
  const frames = [
    ...(surface?.querySelectorAll(".word-quiz-card, .quiz-nav") || []),
  ]
    .filter((element) => element.getBoundingClientRect().width)
    .map((element) => ({ element, before: element.getBoundingClientRect() }));
  const animate = (element, keyframes, duration = 380) => {
    const animation = element.animate(keyframes, {
      duration,
      easing: "cubic-bezier(.22,1,.36,1)",
    });
    quizAnimations.push(animation);
    return animation.finished.catch(() => {});
  };
  const aside = surface?.querySelector(".quiz-insight");
  if (pane === "practice" && aside) {
    await animate(
      aside,
      [
        { opacity: 1, transform: "none" },
        { opacity: 0, transform: "translateX(16px)" },
      ],
      180,
    );
  }
  if (motionId !== quizMotionId) return;
  await update();
  const motions = frames.flatMap(({ element, before }) => {
    const after = element.getBoundingClientRect();
    if (!after.width || !after.height) return [];
    return [
      animate(element, [
        {
          transformOrigin: "0 0",
          transform: `translate(${before.x - after.x}px, ${before.y - after.y}px) scale(${before.width / after.width}, ${before.height / after.height})`,
        },
        { transformOrigin: "0 0", transform: "none" },
      ]),
    ];
  });
  const incoming = surface?.querySelector(
    pane === "insight" ? ".quiz-insight" : ".word-quiz-card",
  );
  if (
    incoming &&
    (pane === "insight" || !frames.some(({ element }) => element === incoming))
  ) {
    motions.push(
      animate(incoming, [
        { opacity: 0, transform: "translateX(16px)" },
        { opacity: 1, transform: "none" },
      ]),
    );
  }
  await Promise.all(motions);
  if (motionId === quizMotionId) quizAnimations = [];
}

function applyWordProgress(wordId, result) {
  patchWord(wordId, (word) => ({
    ...word,
    familiarity: result.familiarity ?? word.familiarity,
    wrongCount: result.wrongCount ?? word.wrongCount,
    exampleEn: result.exampleEn || word.exampleEn,
    exampleCn: result.exampleCn || word.exampleCn,
  }));
}

function patchWord(wordId, patcher) {
  const numericId = Number(wordId);
  const patchList = (words) =>
    words.map((word) => (Number(word.id) === numericId ? patcher(word) : word));
  state.words = patchList(state.words);
  state.reviewWords = patchList(state.reviewWords);
  state.wordBank.words = patchList(state.wordBank.words);
  if (state.selectedWord && Number(state.selectedWord.id) === numericId) {
    state.selectedWord = patcher(state.selectedWord);
  }
}

function applyFavoriteUpdate(updated) {
  patchWord(updated.id, (word) => ({ ...word, favorite: updated.favorite }));
  if (
    state.wordBank.scope === "favorites" &&
    !updated.favorite &&
    state.wordBank.words.some((word) => word.id === updated.id)
  ) {
    state.wordBank.words = state.wordBank.words.filter(
      (word) => word.id !== updated.id,
    );
    state.wordBank.total = Math.max(0, state.wordBank.total - 1);
  }
}

async function toggleFavorite(word) {
  if (!word) return;
  try {
    const updated = await learningApi.toggleFavorite(word.id);
    applyFavoriteUpdate(updated);
    await loadReport();
    if (activeSection.value === "wordbank")
      await Promise.all([loadWordBankStats(), loadWordBankWords(state.wordBank.page)]);
  } catch (error) {
    ElMessage.error(error.message || "收藏失败");
  }
}

async function setReviewMode(mode) { if (state.reviewBusy || mode === state.reviewMode) return; state.reviewMode = mode; await loadReviews(); }
async function loadReviews() {
  state.reviewWords = await learningApi.reviews(state.reviewMode, 8);
  state.reviewIndex = 0;
  state.reviewTotal = state.reviewWords.length;
  state.reviewMistakes = 0;
  state.lastReviewResult = null;
  state.reviewPendingResult = false;
  state.reviewCompleted = false;
  state.reviewForm.spellingAnswer = "";
  state.reviewForm.meaningAnswer = "";
}

async function submitReview() {
  if (!currentReviewWord.value || state.reviewBusy || state.reviewPendingResult)
    return;
  if (!state.reviewForm.spellingAnswer || !state.reviewForm.meaningAnswer) {
    ElMessage.warning("请同时填写英文拼写和中文释义");
    return;
  }
  state.reviewBusy = true;
  try {
    state.lastReviewResult = await learningApi.submitReview(
      currentReviewWord.value.id,
      { ...state.reviewForm },
    );
    applyWordProgress(currentReviewWord.value.id, state.lastReviewResult);
    if (!state.lastReviewResult.correct) state.reviewMistakes++;
    state.reviewPendingResult = true;
    await loadReport();
  } catch (error) {
    ElMessage.error(error.message || "提交失败，答案已保留，请重试");
  } finally {
    state.reviewBusy = false;
  }
}

function advanceReview() {
  if (!currentReviewWord.value || !state.reviewPendingResult) return;
  if (state.lastReviewResult?.correct) {
    state.reviewWords.splice(state.reviewIndex, 1);
  } else {
    const [word] = state.reviewWords.splice(state.reviewIndex, 1);
    if (word) state.reviewWords.push(word);
  }
  state.reviewIndex = 0;
  state.lastReviewResult = null;
  state.reviewPendingResult = false;
  state.reviewForm.spellingAnswer = "";
  state.reviewForm.meaningAnswer = "";
  if (!state.reviewWords.length) {
    state.reviewCompleted = true;
  }
}

function quizExampleText(word, result = {}) {
  if (word?.sourceSentence) return word.sourceSentence;
  return (
    result?.exampleEn ||
    word?.exampleEn ||
    `Try to use ${word?.word || "this word"} in a complete sentence.`
  );
}

function quizExampleCn(word, result = {}) {
  if (word?.sourceSentence) return "";
  return (
    result?.exampleCn || word?.exampleCn || "试着把这个单词放进一个完整句子里。"
  );
}

function wordCardExampleEn(word) {
  return (
    word?.exampleEn ||
    `The word "${word?.word || "this word"}" is useful in English practice.`
  );
}

function wordCardExampleCn(word) {
  return (
    word?.exampleCn || `单词“${word?.word || "这个单词"}”在英语练习中很有用。`
  );
}

function strictReviewParts(result = {}) {
  return [
    result.spellingCorrect ? "拼写正确" : "拼写待修正",
    result.meaningCorrect ? "释义正确" : "释义待加强",
  ];
}

function reviewCloze(word) {
  const example = word?.sourceSentence || word?.exampleEn || `Use ${word?.word || "this word"} in a complete sentence.`;
  const term = word?.sourceWord || word?.word;
  if (!term) return escapeHtml(example);
  return String(example).split(new RegExp(`(\\b${escapeRegExp(term)}\\b)`, "gi"))
    .map(piece => piece.toLowerCase() === term.toLowerCase() ? '<mark class="review-blank">_____</mark>' : escapeHtml(piece)).join('');
}

function highlightTerm(text, term) {
  if (!term) return escapeHtml(text || "");
  const pieces = String(text || "").split(
    new RegExp(`(${escapeRegExp(term)})`, "gi"),
  );
  return pieces
    .map((piece) =>
      piece.toLowerCase() === String(term).toLowerCase()
        ? `<mark>${escapeHtml(piece)}</mark>`
        : escapeHtml(piece),
    )
    .join("");
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;");
}

function escapeRegExp(value) {
  return String(value ?? "").replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function modelLabel(detail) {
  if (!detail?.modelName) return "";
  return detail.modelName === "local-rules" ? "本地词库兜底" : detail.modelName;
}

async function loadConversations() {
  state.conversations = await learningApi.conversations();
  return state.conversations;
}

async function openConversation(id) {
  state.chatReadingArticle = null;
  state.conversationId = id;
  const conversation = state.conversations.find(
    (item) => Number(item.id) === Number(id),
  );
  if (conversation?.scene) state.chatScene = conversation.scene;
  state.chatError = "";
  state.messages = (await learningApi.messages(id)).map(learningMessageFromHistory);
  const latest = state.messages.findLast(item => item.role === "assistant");
  state.trace = latest?.trace || [];
  state.chatMode = latest?.coaching ? "practice" : "assistant";
}

async function handleConversationSelect(value) {
  if (state.chatBusy) return;
  if (value === "new") {
    newConversation();
    return;
  }
  const id = Number(value);
  if (Number.isFinite(id)) await openConversation(id);
}

function newConversation() {
  if (state.chatBusy) return;
  state.chatError = "";
  state.conversationId = null;
  state.messages = [];
  state.trace = [];
  state.chatInput = "";
  state.chatReadingArticle = null;
}

function sendChatPrompt(prompt) {
  if (state.chatBusy) return;
  state.chatInput = prompt;
  return sendChat();
}

async function loadCoachContext() {
  try { state.coachWords = await learningApi.practiceContext(); }
  catch { state.coachWords = []; }
}

async function sendChat() {
  if (!state.chatInput.trim() || state.chatBusy) return;
  const message = state.chatInput.trim();
  state.chatError = "";
  state.chatInput = "";
  const readingArticle = state.chatReadingArticle;
  state.messages.push({ role: "user", content: message, readingArticle });
  state.chatBusy = true;
  try {
    const result = await learningApi.chat({
      conversationId: state.conversationId,
      scene: state.chatScene,
      mode: state.chatMode,
      message,
      readingArticleId: readingArticle?.id,
    });
    state.conversationId = result.conversationId;
    state.chatReadingArticle = null;
    state.messages.push(learningMessageFromResponse(result));
    if (result.coaching?.source === 'model') state.aiSummary.label = '连接正常';
    state.trace = result.trace || [];
    await Promise.allSettled([loadConversations(), loadReport()]);
  } catch (error) {
    state.messages.pop();
    state.chatInput = message;
    state.chatError = error.message || "暂时没能发送，内容已保留，请重试。";
  } finally {
    state.chatBusy = false;
  }
}

async function writingReviewed(result) { state.writingResult = result; if (result.reviewSource === 'AI批改') state.aiSummary.label = '连接正常'; await loadReport(); }

async function loadNewsTopics() {
  state.newsTopics = await learningApi.newsTopics();
}

let newsRequestId = 0;
async function loadNews() {
  const requestId = ++newsRequestId;
  const articles = await learningApi.newsRecommendations({
    difficulty: state.newsDifficulty,
    topic: state.newsTopic,
  });
  if (requestId !== newsRequestId) return;
  state.newsList = articles;
  const currentVisible = state.newsList.some(
    (article) => article.id === state.currentArticle?.id,
  );
  if (!state.newsList.length) {
    ++articleRequestId;
    clearTimeout(state.progressTimer);
    state.tooltip.visible = false;
    state.articleBusy = false;
    state.currentArticle = null;
    state.translation = {};
    state.readerProgress = 0;
    return;
  }
  if (!currentVisible) {
    await loadArticle(state.newsList[0].id);
  }
}

let articleRequestId = 0;
async function loadArticle(id) {
  const requestId = ++articleRequestId;
  clearTimeout(state.progressTimer);
  state.tooltip.visible = false;
  state.articleBusy = true;
  try {
    const article = await learningApi.article(id);
    if (requestId !== articleRequestId) return;
    state.currentArticle = article;
    newsLibraryOpen.value = false;
    state.translation = {};
    state.readerProgress = article.readProgress || 0;
    await nextTick();
    if (readerRef.value) readerRef.value.scrollTop = 0;
  } catch (error) {
    if (requestId === articleRequestId) ElMessage.error(error.message || "文章加载失败，请重试");
  } finally {
    if (requestId === articleRequestId) state.articleBusy = false;
  }
}

function calculateReaderProgress() {
  const el = readerRef.value;
  if (!el || !state.currentArticle) return state.readerProgress;
  const max = el.scrollHeight - el.clientHeight;
  if (max <= 0) return state.readerProgress;
  return Math.min(
    100,
    Math.max(state.readerProgress, Math.round((el.scrollTop / max) * 100)),
  );
}

function handleReaderScroll() {
  if (!state.currentArticle || state.articleBusy) return;
  const nextProgress = calculateReaderProgress();
  state.readerProgress = nextProgress;
  clearTimeout(state.progressTimer);
  const articleId = state.currentArticle.id;
  state.progressTimer = setTimeout(() => updateReadingProgress(nextProgress, false, articleId).catch(() => {}), 500);
}

async function updateReadingProgress(progress, finished, articleId = state.currentArticle?.id) {
  if (!articleId) return;
  const result = await learningApi.articleProgress(articleId, {
    readProgress: progress,
    finished,
  });
  if (state.currentArticle?.id === articleId) {
    state.currentArticle.readProgress = result.readProgress;
    state.currentArticle.finished = result.finished;
    state.readerProgress = result.readProgress;
  }
  const listed = state.newsList.find((article) => article.id === articleId);
  if (listed) Object.assign(listed, { readProgress: result.readProgress, finished: result.finished });
  await loadReport();
}

async function finishReading() {
  if (!state.currentArticle || state.readingBusy) return;
  clearTimeout(state.progressTimer);
  state.readingBusy = true;
  try {
    await updateReadingProgress(100, true);
    ElMessage.success("本篇阅读已完成");
  } catch (error) {
    ElMessage.error(error.message || "阅读进度保存失败，请重试");
  } finally {
    state.readingBusy = false;
  }
}

async function translateArticle() {
  if (!state.currentArticle || state.translateBusy) return;
  if (state.translation.contentCn || state.translation.titleCn) {
    state.translation = {};
    return;
  }
  const articleId = state.currentArticle.id;
  state.translateBusy = true;
  try {
    const translation = await learningApi.translateArticle({
      title: state.currentArticle.title,
      content: state.currentArticle.content,
    });
    if (state.currentArticle?.id === articleId) {
      state.translation = translation;
      await nextTick();
      readerRef.value?.querySelector('.translation-area')?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  } catch (error) {
    if (state.currentArticle?.id === articleId) ElMessage.error(error.message || "翻译失败，请稍后重试");
  } finally {
    state.translateBusy = false;
  }
}

const lookupStopWords = new Set([
  "a",
  "an",
  "the",
  "and",
  "or",
  "but",
  "to",
  "of",
  "in",
  "on",
  "at",
  "by",
  "for",
  "with",
  "is",
  "are",
  "was",
  "were",
  "be",
  "been",
  "it",
  "its",
  "they",
  "them",
  "this",
  "that",
]);

function normalizeLookupWord(word) {
  return String(word || "")
    .toLowerCase()
    .replace(/^[^a-z]+|[^a-z]+$/g, "");
}

function placeTooltip(event) {
  if (event?.currentTarget) {
    const rect = event.currentTarget.getBoundingClientRect();
    state.tooltip.anchor = { left: rect.left, top: rect.top, bottom: rect.bottom };
  }
  const rect = state.tooltip.anchor;
  if (!rect) return;
  const width = tooltipRef.value?.offsetWidth || 340;
  const height = tooltipRef.value?.offsetHeight || 260;
  const margin = 14;
  const x = rect.left - 20;
  const y = rect.bottom + height + margin > window.innerHeight
    ? rect.top - height - 10
    : rect.bottom + 10;
  state.tooltip.x = Math.max(
    margin,
    Math.min(x, window.innerWidth - width - margin),
  );
  state.tooltip.y = Math.max(
    margin,
    Math.min(y, window.innerHeight - height - margin),
  );
}

async function showTooltip(word, event) {
  if (state.tooltip.saving) return;
  const normalized = normalizeLookupWord(word);
  if (!normalized || lookupStopWords.has(normalized)) return;
  cancelTooltipHide();
  placeTooltip(event);
  const sentence = event?.currentTarget?.dataset?.sentence || "";
  const contextKey = `${state.currentArticle?.id}:${normalized}:${sentence}`;
  state.tooltip.contextKey = contextKey;
  state.tooltip.contextSaved = Boolean(state.savedOriginals[contextKey]);
  state.tooltip.sentence = sentence;
  state.tooltip.context = state.contextCache[contextKey] || null;
  state.tooltip.contextBusy = false;
  state.tooltip.visible = true;
  state.tooltip.lookupKey = normalized;
  state.tooltip.loading = !state.lookupCache[normalized];
  state.tooltip.data = state.lookupCache[normalized] || {
    word: normalized,
    meaningCn: "查询中...",
  };
  nextTick(() => placeTooltip());
  if (state.lookupCache[normalized]) {
    return;
  }
  try {
    const result = await learningApi.lookupWord(
      normalized,
      state.currentArticle?.id,
    );
    state.lookupCache[normalized] = result;
    if (state.tooltip.visible && state.tooltip.lookupKey === normalized) {
      state.tooltip.data = result;
    }
  } catch (error) {
    if (state.tooltip.visible && state.tooltip.lookupKey === normalized) {
      state.tooltip.data = {
        word: normalized,
        meaningCn: "查询失败，请稍后重试",
      };
    }
  } finally {
    if (state.tooltip.lookupKey === normalized) {
      state.tooltip.loading = false;
      await nextTick();
      placeTooltip();
    }
  }
}

async function focusLookupActions(word, event) {
  if (lookupStopWords.has(normalizeLookupWord(word))) return;
  await showTooltip(word, event);
  await nextTick();
  tooltipRef.value?.querySelector('button:not(:disabled)')?.focus();
}

function scheduleHideTooltip() {
  clearTimeout(state.tooltipTimer);
  if (state.tooltip.contextBusy || state.tooltip.saving || tooltipRef.value?.matches(':hover') || tooltipRef.value?.contains(document.activeElement)) return;
  state.tooltipTimer = setTimeout(() => {
    state.tooltip.visible = false;
  }, 240);
}

function cancelTooltipHide() {
  clearTimeout(state.tooltipTimer);
}

function handleTooltipFocusOut(event) {
  if (!event.currentTarget.contains(event.relatedTarget)) scheduleHideTooltip();
}

function openPendingLibrary() {
  state.tooltip.visible = false;
  router.push({ path: '/wordbank', query: { scope: 'review' } });
}

async function saveReaderWord(word) {
  if (!word || state.tooltip.loading || state.tooltip.saving || !tooltipHasDefinition.value) return;
  if (state.tooltip.data.savedInReviewBook && (!state.tooltip.sentence || state.tooltip.contextSaved)) return;
  const normalized = normalizeLookupWord(word);
  const lookupKey = state.tooltip.lookupKey;
  state.tooltip.saving = true;
  try {
    const result = state.currentArticle?.id && state.tooltip.sentence
      ? await learningApi.saveReadingWord(state.currentArticle.id, { word: state.tooltip.lookupKey, sentence: state.tooltip.sentence })
      : await learningApi.saveDictionaryWord(word);
    for (const [key, cached] of Object.entries(state.lookupCache)) {
      if (normalizeLookupWord(cached.word) === normalized) state.lookupCache[key] = result;
    }
    state.lookupCache[normalized] = result;
    if (state.tooltip.lookupKey === lookupKey) { state.tooltip.data = result; if (state.tooltip.sentence) { state.savedOriginals[state.tooltip.contextKey] = true; state.tooltip.contextSaved = true; } }
    ElMessage.success(state.tooltip.sentence ? "已保留原句，加入待复习" : "已加入待复习");
    await Promise.allSettled([loadReport(), loadWordBankStats()]);
  } catch (error) {
    ElMessage.error(error.message || "加入待复习失败，请重试");
  } finally {
    state.tooltip.saving = false;
    await nextTick();
    placeTooltip();
  }
}

async function explainReaderWord() {
  if (state.tooltip.contextBusy || !state.tooltip.sentence || !state.currentArticle) return;
  const key = state.tooltip.contextKey;
  state.tooltip.contextBusy = true;
  cancelTooltipHide();
  try {
    const result = await learningApi.explainReadingWord(state.currentArticle.id, { word: state.tooltip.lookupKey, sentence: state.tooltip.sentence });
    state.contextCache[key] = result;
    if (state.tooltip.contextKey === key) state.tooltip.context = result;
  } catch (error) { ElMessage.error(error.message || "语境解读失败，请重试"); }
  finally { if (state.tooltip.contextKey === key) { state.tooltip.contextBusy = false; await nextTick(); placeTooltip(); } }
}

async function discussArticle() {
  if (!state.currentArticle || state.chatBusy) return;
  const article = state.currentArticle;
  newConversation(); state.chatMode = 'practice'; state.chatScene = 'reading';
  state.chatReadingArticle = { id: article.id, title: article.title };
  state.tooltip.visible = false;
  await router.push('/chat');
  await sendChatPrompt('我读完了这篇文章，想用英语聊聊其中的观点。');
}
async function returnToArticle(id) { if (!id) return; await router.push('/news'); await loadArticle(id); }

let wordBankSearchTimer;
let wordBankRequestId = 0;

async function loadWordBank() {
  const scope = wordBankScopes.some((item) => item.id === route.query.scope)
    ? route.query.scope
    : "all";
  const page = state.wordBank.scope === scope ? state.wordBank.page : 0;
  state.wordBank.scope = scope;
  await Promise.all([
    loadWordBankStats(),
    !state.wordBank.categories.length ? learningApi.wordBankCategories().then((categories) => { state.wordBank.categories = categories; }) : Promise.resolve(),
  ]);
  await loadWordBankWords(page);
}

async function loadWordBankStats() {
  state.wordBank.stats = await learningApi.wordBankStats();
}

async function loadWordBankWords(page = 0) {
  const requestId = ++wordBankRequestId;
  state.wordBank.loading = true;
  state.wordBank.page = Math.max(0, page);
  try {
    const result = await learningApi.wordBankWords({
      page: state.wordBank.page,
      size: state.wordBank.size,
      category: state.wordBank.category,
      search: state.wordBank.search,
      scope: state.wordBank.scope,
    });
    if (requestId !== wordBankRequestId) return;
    const lastPage = Math.max(
      0,
      Math.ceil((result.total || 0) / (result.size || state.wordBank.size)) - 1,
    );
    if (state.wordBank.page > lastPage) {
      await loadWordBankWords(lastPage);
      return;
    }
    state.wordBank.words = result.words || [];
    state.wordBank.total = result.total || 0;
    state.wordBank.page = result.page || 0;
    state.wordBank.size = result.size || state.wordBank.size;
    await nextTick();
    wordBankGrid.value?.scrollTo({ top: 0 });
  } catch (error) {
    if (requestId !== wordBankRequestId) return;
    state.wordBank.words = [];
    state.wordBank.total = 0;
    ElMessage.error(error.message || "词库加载失败");
  } finally {
    if (requestId === wordBankRequestId) state.wordBank.loading = false;
  }
}

async function setWordBankScope(scope) {
  if (state.wordBank.scope === scope) return;
  clearTimeout(wordBankSearchTimer);
  await router.push({
    path: "/wordbank",
    query: scope === "all" ? {} : { scope },
  });
}

async function resetWordBankFilters() {
  clearTimeout(wordBankSearchTimer);
  state.wordBank.category = "";
  state.wordBank.search = "";
  if (state.wordBank.scope === "all") await loadWordBankWords(0);
  else await setWordBankScope("all");
}

async function setWordBankCategory(category) {
  clearTimeout(wordBankSearchTimer);
  state.wordBank.category = category;
  await loadWordBankWords(0);
}

function scheduleWordBankSearch() {
  clearTimeout(wordBankSearchTimer);
  wordBankSearchTimer = setTimeout(() => {
    loadWordBankWords(0).catch((error) =>
      ElMessage.error(error.message || "词库搜索失败"),
    );
  }, 350);
}

function updateAiSummary(settings) {
  state.aiSummary.label = settings.chatModel ? "未检测" : "未配置";
  state.aiSummary.modelName = settings.chatModel || "";
  state.aiSummary.loaded = true;
}

async function loadAiSummary() {
  if (state.aiSummary.loaded || state.aiSummary.loading) return;
  state.aiSummary.loading = true;
  try { updateAiSummary(await aiApi.settings()); }
  catch { state.aiSummary.label = "查看配置"; }
  finally { state.aiSummary.loading = false; }
}

async function openSettings(returnTarget) {
  settingsReturnFocus = returnTarget instanceof HTMLElement ? returnTarget : null;
  state.settingsOpen = true;
  try {
    const settings = await aiApi.settings();
    state.ai.baseUrl = settings.baseUrl || "";
    state.ai.chatModel = settings.chatModel || "";
    state.ai.apiKey = "";
    state.ai.models = settings.chatModel ? [settings.chatModel] : [];
    state.ai.status = settings.hasApiKey ? "已保存密钥" : "未保存密钥";
    state.aiSaved = { baseUrl: settings.baseUrl || '', chatModel: settings.chatModel || '' };
    updateAiSummary(settings);
  } catch (error) {
    state.ai.status = error.message || "加载设置失败";
  }
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
    state.ai.status = state.ai.models.length
      ? `已获取 ${state.ai.models.length} 个模型`
      : "没有找到可用模型";
  } catch (error) {
    state.ai.status = error.message || "获取模型失败，可以直接输入模型名后测试连接";
  } finally {
    state.aiBusy = false;
  }
}

async function testAiConnection() {
  if (state.aiBusy) return;
  state.aiBusy = true; state.aiTesting = true;
  state.ai.status = '正在发送实际模型请求…';
  try {
    const result = await aiApi.test({ baseUrl: state.ai.baseUrl, apiKey: state.ai.apiKey, chatModel: state.ai.chatModel });
    state.ai.status = `${result.modelName} 连接正常 · ${result.latencyMs} ms（当前表单）`;
    if (!state.ai.apiKey && state.ai.baseUrl === state.aiSaved.baseUrl && state.ai.chatModel === state.aiSaved.chatModel) state.aiSummary.label = '连接正常';
  } catch (error) { state.ai.status = error.message || '连接失败，请检查配置后重试'; }
  finally { state.aiBusy = false; state.aiTesting = false; }
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
    updateAiSummary(settings);
    state.aiSaved = { baseUrl: settings.baseUrl || '', chatModel: settings.chatModel || '' };
    ElMessage.success("AI 设置已保存");
  } catch (error) {
    state.ai.status = error.message || "保存失败，输入内容已保留，请重试";
  } finally {
    state.aiBusy = false;
  }
}

async function boot() {
  document.body.className = "";
  state.currentUser = await authApi.me();
  await Promise.all([
    loadReport(),
    loadQuizCategories().then(() => loadWords(false)),
    loadReviews(),
    loadNewsTopics().then(() => loadNews()),
    loadConversations(),
    loadAiSummary(),
    loadCoachContext(),
  ]);
  if (activeSection.value === "wordbank") {
    await loadWordBank();
  }
  state.loading = false;
}

watch(
  () => currentQuizWord.value?.id,
  () => {
    resetQuizPane();
  },
);

watch(newsLibraryOpen, async (open) => {
  await nextTick();
  (open ? newsLibraryCloseRef.value : newsLibraryButtonRef.value)?.focus();
});

watch(() => [state.ai.baseUrl, state.ai.apiKey, state.ai.chatModel], () => {
  if (!state.aiBusy && state.ai.status.includes('连接正常')) state.ai.status = '配置已修改，请重新测试连接';
});

watch(
  () => state.settingsOpen || wordDialogVisible.value,
  async (open) => {
    if (open) {
      previousFocus = state.settingsOpen && settingsReturnFocus ? settingsReturnFocus : document.activeElement;
      document.body.style.overflow = "hidden";
      await nextTick();
      const dialog = state.settingsOpen
        ? settingsPanelRef.value
        : wordDialogRef.value;
      requestAnimationFrame(() =>
        dialog?.querySelector("button, input, select")?.focus(),
      );
    } else {
      document.body.style.overflow = "";
      await nextTick();
      previousFocus?.focus();
      settingsReturnFocus = null;
    }
  },
  { flush: "post" },
);

watch([activeSection, () => route.query.scope], async ([section]) => {
  document.title = sectionTitle.value + " · LinguaMate";
  state.tooltip.visible = false;
  if (section === "news") state.lookupCache = {};
  if (section === "review" && !state.loading && !state.reviewBusy) await loadReviews();
  if (section === "wordbank" && !state.loading) await loadWordBank();
  if (section === "chat" && !state.loading) await loadCoachContext();
});

onMounted(() => {
  document.title = sectionTitle.value + " · LinguaMate";
  document.addEventListener("keydown", trapDialogFocus, true);
  boot().catch((error) => {
    console.error(error);
    state.loading = false;
    ElMessage.error(error.message || "加载失败");
  });
});

onBeforeUnmount(() => {
  stopQuizMotion();
  document.removeEventListener("keydown", trapDialogFocus, true);
  document.body.style.overflow = "";
  clearTimeout(wordBankSearchTimer);
  clearTimeout(state.tooltipTimer);
  clearTimeout(state.progressTimer);
});
</script>

<style scoped>
.sidebar-model-config { display: none; }
@media (min-width: 981px) {
  .sidebar-model-config {
    display: flex;
    align-items: center;
    gap: 10px;
    width: 100%;
    min-height: 56px;
    padding: 9px 11px;
    border: 1px solid #e1eaf0;
    border-radius: 13px;
    background: #f8fbfd;
    color: #476174;
    text-align: left;
    font-size: 13px;
    transition: background 200ms ease, border-color 200ms ease;
  }
  .sidebar-model-config:hover { border-color: #c5dfed; background: #f0f8fd; }
  .sidebar-model-icon { display: grid; place-items: center; width: 32px; height: 32px; border-radius: 9px; background: #eaf2f8; color: #6993ad; }
  .sidebar-model-icon > svg { width: 19px; height: 19px; transition: transform 240ms var(--ease); }
  .sidebar-model-config:hover .sidebar-model-icon > svg { transform: rotate(20deg); }
  .sidebar-model-config > small { margin-left: auto; color: #8b9eab; font-size: 10px; }
  .sidebar-model-config > small.is-ready { color: #638a49; }
}
@media (prefers-reduced-motion: reduce) {
  .sidebar-model-config,
  .sidebar-model-icon > svg { transition: none; }
}
</style>
