<template>
  <canvas ref="ambientCanvasRef" class="ambient-canvas" aria-hidden="true"></canvas>
  <canvas ref="cursorCanvasRef" class="cursor-canvas" aria-hidden="true"></canvas>
  <div ref="cursorOrbRef" class="cursor-orb" aria-hidden="true"></div>
  <div v-if="state.loading" class="app-loading">正在加载学习工作台...</div>
  <div v-else class="app-shell">
    <aside class="side-nav">
      <div class="brand">
        <img class="brand-mark" src="/assets/linguamate-icon.svg" alt="" />
        <div>
          <strong>LinguaMate</strong>
          <span>英语学习助手</span>
        </div>
      </div>
      <nav>
        <button
          v-for="item in navItems"
          :key="item.id"
          class="nav-item"
          :class="{ active: activeSection === item.id }"
          type="button"
          @click="goSection(item.id)"
        >
          <span class="nav-dot"></span>{{ item.label }}
        </button>
      </nav>
      <button class="nav-logout-button" type="button" @click="logout">退出登录</button>
    </aside>

    <main class="workspace">
      <header class="topbar">
        <div class="hero-copy">
          <p class="eyebrow">今天从哪一关开始？</p>
          <h1>每天一点，英语升级</h1>
          <p class="subtitle">对话、背词、复习、作文和阅读，一条路线完成。</p>
        </div>
        <div class="status-grid">
          <div><strong>{{ state.report.learnedWords || 0 }}</strong><span>已掌握</span></div>
          <div><strong>{{ state.report.reviewWords || 0 }}</strong><span>待复习</span></div>
          <div><strong>{{ state.report.favoriteWords || 0 }}</strong><span>已收藏</span></div>
          <div><strong>{{ state.report.essayCount || 0 }}</strong><span>作文批改</span></div>
          <div><strong>{{ state.report.readingCount || 0 }}</strong><span>阅读记录</span></div>
        </div>
        <section class="level-panel" :class="{ 'is-admin-card': state.currentUser?.role === 'ADMIN' }">
          <div class="level-user-row">
            <div class="level-avatar">{{ userName.slice(0, 1) }}</div>
            <div class="level-user-copy">
              <span>{{ userName }}</span>
              <small>{{ state.currentUser?.username }}</small>
            </div>
            <span class="level-role-pill">{{ state.currentUser?.role === "ADMIN" ? "管理中" : "学习中" }}</span>
          </div>
          <div v-if="state.currentUser?.role !== 'ADMIN'" class="level-student-block">
            <div class="level-badge-row">
              <div class="level-badge">Lv.{{ state.level.level || 1 }}</div>
              <span>下一等级</span>
            </div>
            <div class="level-xp-row">
              <div class="level-xp-bar"><div class="level-xp-fill" :style="{ width: levelPercent + '%' }"></div></div>
              <span class="level-xp-text">{{ state.level.experience || 0 }} / {{ state.level.nextLevelXp || 100 }}</span>
            </div>
          </div>
          <div v-else class="level-admin-block">
            <div>
              <strong>管理工作台</strong>
              <span>维护用户、词库与阅读内容</span>
            </div>
            <RouterLink class="level-admin-link" to="/admin">进入后台</RouterLink>
          </div>
        </section>
      </header>

      <section id="dashboard" class="section" :class="{ active: activeSection === 'dashboard' }">
        <div class="home-grid">
          <section class="duo-route-shell">
            <div class="duo-route-heading">
              <div>
                <p class="eyebrow compact">今日路线</p>
                <h2>{{ routeDoneCount === journeySteps.length ? "今天的课程全部完成！" : "继续你的学习连胜" }}</h2>
                <p>完成每一关，稳稳积累今天的学习能量。</p>
              </div>
              <div class="duo-route-progress" :aria-label="`今日路线完成 ${routeDoneCount}/${journeySteps.length}`">
                <div class="duo-route-progress-copy">
                  <span>今日进度</span>
                  <strong>{{ routeDoneCount }}/{{ journeySteps.length }}</strong>
                </div>
                <div class="duo-route-progress-bar"><i :style="{ width: routeProgressPercent + '%' }"></i></div>
              </div>
            </div>

            <div class="duo-route-track" role="list" aria-label="今日学习关卡">
              <div
                v-for="(step, index) in journeySteps"
                :key="step.id"
                class="duo-step"
                :class="{ 'is-complete': step.done, 'is-current': step.recommended }"
                role="listitem"
              >
                <span v-if="index < journeySteps.length - 1" class="duo-step-connector" :class="{ 'is-complete': step.done }" aria-hidden="true"></span>
                <span v-if="step.recommended" class="duo-current-flag">下一关</span>
                <button
                  class="duo-step-button"
                  :class="[`node-${step.tone}`, { 'is-complete': step.done, 'is-current': step.recommended }]"
                  type="button"
                  :aria-label="routeStepAriaLabel(step)"
                  @click="goSection(step.section)"
                >
                  <span>{{ step.done ? "✓" : step.symbol }}</span>
                </button>
                <strong>{{ step.title }}</strong>
                <small>{{ step.detail }}</small>
              </div>
            </div>

            <div class="duo-route-footer">
              <span><strong>{{ journeyEnergy }}%</strong> 今日能量</span>
              <span><strong>{{ journeySteps.length - routeDoneCount }}</strong> 关待完成</span>
              <button type="button" @click="goSection(focusStep?.section || 'vocabulary')">
                {{ routeDoneCount === journeySteps.length ? "再练一组" : `开始${focusStep?.title || "学习"}` }}
              </button>
            </div>
          </section>
          <aside class="daily-panel">
            <section class="duo-quest-panel">
              <span class="duo-quest-kicker">推荐任务</span>
              <div class="duo-quest-icon" :class="`node-${focusStep?.tone || 'green'}`">{{ focusStep?.symbol || "学" }}</div>
              <div class="duo-quest-copy">
                <h2>{{ focusStep?.title || "继续学习" }}</h2>
                <p>{{ focusStep?.landmark || "今日课程" }} · {{ focusStep?.detail || "完成一个学习任务" }}</p>
              </div>
              <button type="button" @click="goSection(focusStep?.section || 'vocabulary')">开始学习</button>
            </section>
            <section class="panel duo-tips-panel">
              <div class="panel-heading"><h2>今日建议</h2><span>{{ (state.report.nextActions || []).length }} 项</span></div>
              <ul class="clean-list">
                <li v-for="item in state.report.nextActions || []" :key="item">{{ item }}</li>
              </ul>
            </section>
          </aside>
        </div>
        <section class="panel">
          <div class="panel-heading">
            <h2>今日单词</h2>
            <button class="soft-action" type="button" @click="goSection('vocabulary')">去背词</button>
          </div>
          <div class="word-strip">
            <article v-for="word in state.words.slice(0, 5)" :key="word.id" class="word-card">
              <strong>{{ word.word }}</strong>
              <span class="word-meta">{{ word.partOfSpeech }} · {{ word.levelTag }}</span>
              <p>{{ word.meaningCn }}</p>
            </article>
          </div>
        </section>
      </section>

      <section id="chat" class="section chat-workspace" :class="{ active: activeSection === 'chat' }">
        <section class="panel chat-panel">
          <div class="chat-panel-heading">
            <div class="chat-title-block">
              <h2>英语对话</h2>
              <p>{{ currentConversationTitle }}</p>
            </div>
            <div class="chat-controls" aria-label="对话控制">
              <button class="chat-new-button" type="button" @click="newConversation">新对话</button>
              <el-select
                class="chat-dropdown chat-conversation-select"
                :model-value="conversationSelectValue"
                placeholder="历史会话"
                filterable
                :teleported="true"
                popper-class="chat-select-popper"
                @change="handleConversationSelect"
              >
                <el-option value="new" label="新对话">
                  <span class="chat-option-title">新对话</span>
                  <span class="chat-option-meta">从空白会话开始</span>
                </el-option>
                <el-option
                  v-for="conv in state.conversations"
                  :key="conv.id"
                  :value="`${conv.id}`"
                  :label="conv.title || '学习对话'"
                >
                  <span class="chat-option-title">{{ conv.title || "学习对话" }}</span>
                  <span class="chat-option-meta">{{ sceneLabel(conv.scene) }} · {{ formatDate(conv.updatedAt) }}</span>
                </el-option>
              </el-select>
              <el-select
                v-model="state.chatScene"
                class="chat-dropdown chat-scene-select"
                aria-label="选择对话场景"
                :teleported="true"
                popper-class="chat-select-popper"
              >
                <el-option
                  v-for="scene in chatSceneOptions"
                  :key="scene.value"
                  :label="scene.label"
                  :value="scene.value"
                />
              </el-select>
              <el-popover
                placement="bottom-end"
                trigger="click"
                :width="360"
                :teleported="true"
                popper-class="trace-popover"
              >
                <template #reference>
                  <button class="trace-toggle" type="button">
                    思考过程
                    <span>{{ state.trace.length || 0 }}</span>
                  </button>
                </template>
                <div class="trace-dropdown-list">
                  <article v-for="(step, index) in state.trace" :key="`${step.phase}-${index}`" class="trace-item">
                    <span>{{ step.phase }}</span>
                    <strong>{{ step.title }}</strong>
                    <p>{{ step.detail }}</p>
                  </article>
                  <p v-if="!state.trace.length" class="trace-empty">暂无思考过程</p>
                </div>
              </el-popover>
            </div>
          </div>
          <div class="chat-messages">
            <article v-for="(message, index) in state.messages" :key="`${message.role}-${index}-${message.createdAt || ''}`" class="chat-message" :class="message.role">
              <strong>{{ message.role === "user" ? "你" : "LinguaMate" }}</strong>
              <p>{{ message.content }}</p>
              <section v-if="message.learningCard" class="learning-advisor-card" :class="`card-${message.learningCard.type}`">
                <header>
                  <div>
                    <span>{{ message.learningCard.type === "daily-route" ? "LEARNING ROUTE" : "VOCABULARY PROFILE" }}</span>
                    <h3>{{ message.learningCard.title }}</h3>
                  </div>
                  <b v-if="message.learningCard.type === 'daily-route'">
                    {{ message.learningCard.items.filter((item) => item.status === "complete").length }}/{{ message.learningCard.items.length }}
                  </b>
                  <b v-else>{{ message.learningCard.totalElements }}</b>
                </header>
                <div class="learning-card-items">
                  <article v-for="item in message.learningCard.items" :key="item.key" :class="{ 'is-complete': item.status === 'complete' }">
                    <i>{{ item.status === "complete" ? "✓" : message.learningCard.type === "daily-route" ? "○" : "Aa" }}</i>
                    <div><strong>{{ item.label }}</strong><small>{{ item.detail }}</small></div>
                  </article>
                  <p v-if="!message.learningCard.items.length" class="learning-card-empty">这个分组暂时没有单词。</p>
                </div>
                <footer v-if="message.learningCard.type === 'vocabulary-summary' && message.learningCard.totalPages > 1" class="learning-card-pages">
                  <button type="button" :disabled="message.learningCard.page <= 0" @click="loadLearningCardPage(message, index, -1)">上一页</button>
                  <span>{{ message.learningCard.page + 1 }} / {{ message.learningCard.totalPages }}</span>
                  <button type="button" :disabled="message.learningCard.page >= message.learningCard.totalPages - 1" @click="loadLearningCardPage(message, index, 1)">下一页</button>
                </footer>
              </section>
              <div v-if="message.actions?.length" class="learning-card-actions">
                <button v-for="action in message.actions" :key="action.id" type="button" @click="handleLearningAction(action)">
                  {{ action.label }}<span>→</span>
                </button>
              </div>
            </article>
          </div>
          <form class="input-row" @submit.prevent="sendChat">
            <input v-model.trim="state.chatInput" autocomplete="off" placeholder="输入英文句子，或问：查询单词 essential" />
            <button type="submit" :disabled="state.chatBusy">{{ state.chatBusy ? "思考中..." : "发送" }}</button>
          </form>
        </section>
      </section>

      <section id="vocabulary" class="section" :class="{ active: activeSection === 'vocabulary' }">
        <section class="panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow compact">Word Quest</p>
              <h2>单词闯关</h2>
            </div>
            <select v-model="state.quizCategory" aria-label="选择闯关词库" @change="loadWords(false)">
              <option value="">智能混合</option>
              <option v-for="category in state.quizCategories" :key="categoryValue(category)" :value="categoryValue(category)">
                {{ categoryLabel(category) }}
              </option>
            </select>
          </div>
          <div class="word-quiz-shell">
            <div v-if="state.quizCompleted" class="quiz-complete">
              <div class="quiz-badge">完成</div>
              <h3>本轮单词闯关完成</h3>
              <p>已经完成 {{ quizAnsweredCount }} 个单词，可以去复习错词队列继续巩固。</p>
              <button class="quiz-primary" type="button" @click="restartQuiz">再来一轮</button>
            </div>
            <article
              v-else-if="currentQuizWord"
              class="word-quiz-card"
              :class="{ 'is-answered': quizResultForCurrent, 'has-ai-detail': wordDetailForCurrent }"
            >
              <div class="quiz-progress">
                <span>第 {{ state.quizIndex + 1 }} / {{ state.words.length }} 题</span>
                <div class="quiz-progress-track"><i :style="{ width: quizProgress + '%' }"></i></div>
              </div>
              <div class="quiz-study-stage">
                <div class="quiz-word-shell">
                  <button class="favorite-button quiz-favorite" :class="{ 'is-active': currentQuizWord.favorite }" type="button" :aria-pressed="String(Boolean(currentQuizWord.favorite))" @click="toggleFavorite(currentQuizWord)">
                    ★
                  </button>
                  <div class="quiz-word">{{ currentQuizWord.word }}</div>
                </div>
                <div class="quiz-example-slot" :class="{ 'is-visible': quizResultForCurrent }">
                  <aside v-if="quizResultForCurrent" class="quiz-example-panel" aria-label="单词例句">
                    <p class="quiz-example-en" v-html="highlightTerm(quizExampleText(currentQuizWord, quizResultForCurrent), currentQuizWord.word)"></p>
                    <p class="quiz-example-cn">{{ quizExampleCn(currentQuizWord, quizResultForCurrent) }}</p>
                    <span v-if="quizResultForCurrent.nextReviewHint" class="quiz-example-source">{{ quizResultForCurrent.nextReviewHint }}</span>
                  </aside>
                </div>
              </div>
              <div v-if="quizResultForCurrent" class="quiz-detail-actions">
                <button
                  class="quiz-detail-button"
                  :class="{ 'is-loading': wordDetailForCurrent?.status === 'loading', 'is-done': wordDetailForCurrent?.status === 'ready' }"
                  type="button"
                  :disabled="wordDetailForCurrent?.status === 'loading'"
                  @click="loadWordDetail(currentQuizWord)"
                >
                  {{ wordDetailForCurrent?.status === "loading" ? "生成中..." : wordDetailForCurrent?.status === "ready" ? "已生成解析" : "深度解析" }}
                </button>
              </div>
              <section
                v-if="wordDetailForCurrent"
                class="quiz-ai-detail"
                :class="{ 'is-loading': wordDetailForCurrent.status === 'loading', 'is-error': wordDetailForCurrent.status === 'error' }"
              >
                <template v-if="wordDetailForCurrent.status === 'loading'">
                  <span class="quiz-ai-spinner" aria-hidden="true"></span>
                  <span>正在生成 {{ currentQuizWord.word }} 的深度解析...</span>
                </template>
                <template v-else-if="wordDetailForCurrent.status === 'error'">
                  <span>{{ wordDetailForCurrent.message || "模型未配置或暂时不可用，请检查 AI 设置" }}</span>
                  <button class="quiz-detail-button retry-detail-button" type="button" @click="loadWordDetail(currentQuizWord, true)">重试</button>
                </template>
                <template v-else>
                  <div class="quiz-ai-head">
                    <strong>深度解析 · {{ wordDetailForCurrent.data?.word || currentQuizWord.word }}</strong>
                    <span>{{ modelLabel(wordDetailForCurrent.data) }}{{ wordDetailForCurrent.data?.cached ? " · 已缓存" : "" }}</span>
                  </div>
                  <div class="quiz-ai-grid">
                    <section v-if="wordDetailForCurrent.data?.examples?.length" class="quiz-ai-section">
                      <h3>更多例句</h3>
                      <div v-for="example in wordDetailForCurrent.data.examples" :key="example.en" class="quiz-ai-row">
                        <p v-html="highlightTerm(example.en || '', currentQuizWord.word)"></p>
                        <small v-if="example.cn">{{ example.cn }}</small>
                      </div>
                    </section>
                    <section v-if="wordDetailForCurrent.data?.variants?.length" class="quiz-ai-section">
                      <h3>词性变种</h3>
                      <div v-for="variant in wordDetailForCurrent.data.variants" :key="`${variant.form}-${variant.type}`" class="quiz-ai-row">
                        <p><b>{{ variant.form }}</b> {{ variant.type }}</p>
                        <small v-if="variant.meaningCn">{{ variant.meaningCn }}</small>
                      </div>
                    </section>
                    <section v-if="wordDetailForCurrent.data?.usageNotes?.length" class="quiz-ai-section">
                      <h3>用法提示</h3>
                      <div v-for="note in wordDetailForCurrent.data.usageNotes" :key="note" class="quiz-ai-row">
                        <p>{{ note }}</p>
                      </div>
                    </section>
                    <section v-if="wordDetailForCurrent.data?.collocations?.length" class="quiz-ai-section">
                      <h3>固定搭配</h3>
                      <div v-for="item in wordDetailForCurrent.data.collocations" :key="`${item.phrase}-${item.meaningCn}`" class="quiz-ai-row">
                        <p><b>{{ item.phrase }}</b> {{ item.meaningCn }}</p>
                        <small v-if="item.exampleEn" v-html="highlightTerm(item.exampleEn, currentQuizWord.word)"></small>
                      </div>
                    </section>
                  </div>
                </template>
              </section>
              <div class="quiz-options">
                <button
                  v-for="option in quizOptions"
                  :key="option"
                  class="quiz-option"
                  :class="quizOptionClass(option)"
                  type="button"
                  :disabled="Boolean(quizResultForCurrent)"
                  @click="submitQuiz(option)"
                >
                  {{ option }}
                </button>
              </div>
              <div class="quiz-nav">
                <button class="quiz-secondary" type="button" @click="prevQuiz" :disabled="state.quizIndex <= 0">上一题</button>
                <button class="quiz-primary" type="button" :disabled="!quizResultForCurrent" @click="nextQuiz">
                  {{ state.quizIndex >= state.words.length - 1 ? "完成" : "下一题" }}
                </button>
              </div>
            </article>
            <div v-else class="quiz-empty">当前词库暂无单词。</div>
          </div>
        </section>
      </section>

      <section id="review" class="section" :class="{ active: activeSection === 'review' }">
        <section class="panel">
          <div class="panel-heading">
            <h2>复习挑战</h2>
            <div class="review-switch">
              <button v-for="mode in reviewModes" :key="mode.id" class="review-tab" :class="{ 'is-active': state.reviewMode === mode.id }" type="button" @click="changeReviewMode(mode.id)">
                {{ mode.label }}
              </button>
            </div>
          </div>
          <div v-if="state.reviewMode === 'today'">
            <div v-if="state.reviewCompleted" class="review-challenge-card is-complete">
              <div class="quiz-badge">通过</div>
              <h3>今日复习已完成</h3>
              <p>本组错词已经复习完，可以重新加载今日队列或去背新词。</p>
              <div class="review-card-actions">
                <button class="quiz-primary" type="button" @click="loadReviews">重新加载</button>
              </div>
            </div>
            <div v-else-if="currentReviewWord" class="review-list">
              <article class="review-challenge-card" :class="{ 'is-correct': state.lastReviewResult?.correct, 'is-wrong': state.lastReviewResult && !state.lastReviewResult.correct }">
                <div class="quiz-progress">
                  <span>剩余 {{ reviewRemaining }} / {{ reviewTotal }} 词</span>
                  <div class="quiz-progress-track"><i :style="{ width: reviewProgress + '%' }"></i></div>
                </div>
                <div class="review-prompt">
                  <div class="review-prompt-meta">
                    <span v-if="phonetic(currentReviewWord)">{{ phonetic(currentReviewWord) }}</span>
                    <span v-if="currentReviewWord.partOfSpeech">{{ currentReviewWord.partOfSpeech }}</span>
                    <span>错误 {{ currentReviewWord.wrongCount || 0 }} 次</span>
                  </div>
                  <div v-if="!state.reviewPendingResult" class="review-cloze" v-html="reviewCloze(currentReviewWord)"></div>
                  <div v-else class="review-filled-example">
                    <p v-html="highlightTerm(quizExampleText(currentReviewWord, state.lastReviewResult), state.lastReviewResult?.expectedWord || currentReviewWord.word)"></p>
                    <span>{{ quizExampleCn(currentReviewWord, state.lastReviewResult) }}</span>
                  </div>
                </div>
                <div v-if="state.lastReviewResult" class="review-result" :class="state.lastReviewResult.correct ? 'is-correct' : 'is-wrong'">
                  <strong>{{ state.lastReviewResult.correct ? "通过" : "继续巩固" }}</strong>
                  <span>{{ strictReviewParts(state.lastReviewResult).join(" · ") }}</span>
                  <span>正确单词：{{ state.lastReviewResult.expectedWord || currentReviewWord.word }}</span>
                  <span>正确释义：{{ state.lastReviewResult.expectedMeaning }}</span>
                  <small>{{ state.lastReviewResult.nextReviewHint }}</small>
                </div>
                <form class="review-challenge-form" :hidden="state.reviewPendingResult" @submit.prevent="submitReview">
                  <div class="review-input-grid">
                    <label>
                      <span>英文拼写</span>
                      <input v-model.trim="state.reviewForm.spellingAnswer" autocomplete="off" autocapitalize="none" spellcheck="false" placeholder="输入完整英文单词" :disabled="state.reviewBusy" />
                    </label>
                    <label>
                      <span>中文释义</span>
                      <input v-model.trim="state.reviewForm.meaningAnswer" autocomplete="off" placeholder="输入一个释义关键词" :disabled="state.reviewBusy" />
                    </label>
                  </div>
                  <div class="review-card-actions">
                    <button class="quiz-primary" type="submit" :disabled="state.reviewBusy">{{ state.reviewBusy ? "提交中..." : "提交复习" }}</button>
                  </div>
                </form>
                <div v-if="state.reviewPendingResult" class="review-card-actions review-next-actions">
                  <button class="quiz-primary" type="button" @click="advanceReview">下一题</button>
                </div>
              </article>
            </div>
            <p v-else class="empty-state review-empty">今日复习队列暂时为空。</p>
          </div>
          <div v-else class="review-list">
            <article v-for="word in state.reviewWords" :key="word.id" class="word-card">
              <div class="word-card-head">
                <div>
                  <strong>{{ word.word }}</strong>
                  <span class="word-meta">{{ phonetic(word) }} · {{ word.partOfSpeech || word.levelTag }}</span>
                </div>
                <button class="favorite-button" :class="{ 'is-active': word.favorite }" type="button" :aria-pressed="String(Boolean(word.favorite))" @click="toggleFavorite(word)">★</button>
              </div>
              <p>{{ word.meaningCn }}</p>
              <div class="word-card-example">
                <p>{{ wordCardExampleEn(word) }}</p>
                <span>{{ wordCardExampleCn(word) }}</span>
              </div>
            </article>
            <p v-if="!state.reviewWords.length" class="empty-state review-empty">{{ reviewEmptyText }}</p>
          </div>
        </section>
      </section>

      <section id="writing" class="section" :class="{ active: activeSection === 'writing' }">
        <div class="writing-layout">
          <section class="panel">
            <div class="panel-heading">
              <h2>作文批改</h2>
              <select v-model="state.writing.examType" aria-label="作文类型">
                <option value="general">通用写作</option>
                <option value="cet4">四级作文</option>
                <option value="cet6">六级作文</option>
                <option value="ielts">雅思写作</option>
              </select>
            </div>
            <form class="writing-form" @submit.prevent="reviewEssay">
              <input v-model.trim="state.writing.title" placeholder="作文标题" />
              <textarea v-model.trim="state.writing.essayText" rows="10" placeholder="Paste your English essay here..."></textarea>
              <div class="settings-row">
                <input v-model.trim="state.writing.targetLevel" placeholder="目标水平，如 B1" />
                <button class="quiz-primary" type="submit" :disabled="state.writingBusy">{{ state.writingBusy ? "批改中..." : "提交批改" }}</button>
              </div>
            </form>
          </section>
          <section class="panel">
            <h2>批改结果</h2>
            <div v-if="state.writingResult" class="writing-result">
              <div class="score-grid">
                <div><strong>{{ state.writingResult.totalScore }}</strong><span>总分</span></div>
                <div><strong>{{ state.writingResult.grammarScore }}</strong><span>语法</span></div>
                <div><strong>{{ state.writingResult.vocabularyScore }}</strong><span>词汇</span></div>
                <div><strong>{{ state.writingResult.structureScore }}</strong><span>结构</span></div>
              </div>
              <p>{{ state.writingResult.summary }}</p>
              <h3>句子建议</h3>
              <article v-for="item in state.writingResult.feedback || []" :key="item.originalSentence" class="feedback-item">
                <strong>{{ item.category }}</strong>
                <p>{{ item.originalSentence }}</p>
                <small>{{ item.suggestion }}</small>
              </article>
              <h3>润色版本</h3>
              <p>{{ state.writingResult.polishedText }}</p>
            </div>
            <p v-else class="empty-state">提交作文后会显示评分、逐句建议和润色版本。</p>
          </section>
        </div>
      </section>

      <section id="news" class="section" :class="{ active: activeSection === 'news' }">
        <div class="news-layout">
          <section class="panel">
            <div class="panel-heading news-heading">
              <h2>阅读推荐</h2>
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
                    :class="{ 'is-active': state.newsDifficulty === item.value }"
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
                    v-for="topic in newsTopicOptions"
                    :key="topic.value"
                    type="button"
                    class="news-topic-chip"
                    :class="{ 'is-active': state.newsTopic === topic.value }"
                    @click="setNewsTopic(topic.value)"
                  >
                    {{ topic.label }}
                  </button>
                </div>
              </div>
            </div>
            <div class="news-list">
              <article
                v-for="article in state.newsList"
                :key="article.id"
                class="news-item"
                :class="{ 'is-active': state.currentArticle?.id === article.id }"
                @click="loadArticle(article.id)"
              >
                <strong>{{ article.title }}</strong>
                <p>{{ article.summary }}</p>
                <span>{{ difficultyLabel(article.difficulty) }} · {{ articleTopicText(article.topicTags) }} · {{ article.readProgress || 0 }}%</span>
              </article>
              <p v-if="!state.newsList.length" class="news-empty">当前筛选暂无文章，换个难度或主题试试。</p>
            </div>
          </section>
          <section class="panel reader-panel">
            <div class="panel-heading">
              <div>
                <p class="eyebrow compact">{{ state.currentArticle?.sourceName || "Reader" }}</p>
                <h2>{{ state.currentArticle?.title || "选择一篇文章" }}</h2>
              </div>
              <button class="translate-btn" type="button" :disabled="!state.currentArticle || state.translateBusy" @click="translateArticle">
                {{ state.translateBusy ? "翻译中..." : "全文翻译" }}
              </button>
            </div>
            <p class="reader-summary">{{ state.currentArticle?.summary || "鼠标停在英文单词上，可查看音标、词性和中文释义。" }}</p>
            <div class="reader-progress">
              <div class="reader-progress-track"><span :style="{ width: readerProgress + '%' }"></span></div>
              <span>{{ readerProgress }}%</span>
              <button class="reader-finish-btn" type="button" :disabled="!state.currentArticle" @click="finishReading">完成阅读</button>
            </div>
            <article ref="readerRef" class="reader-content" @scroll="handleReaderScroll">
              <template v-for="token in articleTokens" :key="token.key">
                <span
                  v-if="token.word"
                  class="reader-word"
                  @mouseenter="showTooltip(token.text, $event)"
                  @mouseleave="scheduleHideTooltip"
                >{{ token.text }}</span>
                <span v-else>{{ token.text }}</span>
              </template>
            </article>
            <div v-if="state.translation.titleCn || state.translation.contentCn" class="translation-area">
              <h3>{{ state.translation.titleCn }}</h3>
              <p>{{ state.translation.contentCn }}</p>
            </div>
          </section>
        </div>
      </section>

      <section id="wordbank" class="section" :class="{ active: activeSection === 'wordbank' }">
        <section class="panel wordbank-layout">
          <div class="panel-heading">
            <div>
              <p class="eyebrow compact">Word Bank</p>
              <h2>词库浏览</h2>
            </div>
            <span class="energy-pill">{{ state.wordBank.total }} 个单词</span>
          </div>
          <div class="wordbank-filters">
            <div class="wordbank-categories">
              <button class="review-tab" :class="{ 'is-active': !state.wordBank.category }" type="button" @click="setWordBankCategory('')">全部</button>
              <button v-for="category in state.wordBank.categories" :key="categoryValue(category)" class="review-tab" :class="{ 'is-active': state.wordBank.category === categoryValue(category) }" type="button" @click="setWordBankCategory(categoryValue(category))">
                {{ categoryLabel(category) }}
              </button>
            </div>
            <input v-model.trim="state.wordBank.search" class="wordbank-search" autocomplete="off" placeholder="搜索英文前缀或中文释义前缀" @input="scheduleWordBankSearch" @keyup.enter="loadWordBankWords(0)" />
            <button class="settings-secondary" type="button" @click="loadWordBankWords(0)">搜索</button>
          </div>
          <div class="word-strip word-bank-grid">
            <article v-for="word in state.wordBank.words" :key="word.id" class="word-card vue-clickable" @click="state.selectedWord = word">
              <div class="word-card-head">
                <div>
                  <strong>{{ word.word }}</strong>
                  <span class="word-meta">{{ phonetic(word) }} · {{ word.partOfSpeech || word.levelTag }}</span>
                </div>
                <button class="favorite-button" :class="{ 'is-active': word.favorite }" type="button" :aria-pressed="String(Boolean(word.favorite))" @click.stop="toggleFavorite(word)">★</button>
              </div>
              <p>{{ word.meaningCn }}</p>
              <div class="word-card-example">
                <p>{{ wordCardExampleEn(word) }}</p>
                <span>{{ wordCardExampleCn(word) }}</span>
              </div>
            </article>
          </div>
          <div class="wordbank-pagination">
            <span class="wordbank-page-info">第 {{ state.wordBank.page + 1 }} 页 / {{ wordBankTotalPages }} 页</span>
            <button class="wordbank-page-btn" type="button" :disabled="state.wordBank.page <= 0" @click="loadWordBankWords(state.wordBank.page - 1)">上一页</button>
            <button class="wordbank-page-btn" type="button" :disabled="state.wordBank.page >= wordBankTotalPages - 1" @click="loadWordBankWords(state.wordBank.page + 1)">下一页</button>
          </div>
        </section>
      </section>
    </main>

    <button class="settings-fab" type="button" aria-label="我的 API 设置" title="我的 API 设置" @click="openSettings">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M19.14 12.94c.04-.31.06-.63.06-.94s-.02-.63-.06-.94l2.03-1.58a.5.5 0 0 0 .12-.64l-1.92-3.32a.5.5 0 0 0-.6-.22l-2.39.96a7.1 7.1 0 0 0-1.63-.94l-.36-2.54A.5.5 0 0 0 13.9 1h-3.8a.5.5 0 0 0-.49.41l-.36 2.54c-.58.22-1.12.53-1.63.94l-2.39-.96a.5.5 0 0 0-.6.22L2.71 7.47a.5.5 0 0 0 .12.64l2.03 1.58c-.04.31-.06.63-.06.94s.02.63.06.94L2.83 13.15a.5.5 0 0 0-.12.64l1.92 3.32a.5.5 0 0 0 .6.22l2.39-.96c.5.41 1.05.73 1.63.94l.36 2.54a.5.5 0 0 0 .49.41h3.8a.5.5 0 0 0 .49-.41l.36-2.54c.58-.22 1.12-.53 1.63-.94l2.39.96a.5.5 0 0 0 .6-.22l1.92-3.32a.5.5 0 0 0-.12-.64l-2.03-1.58ZM12 15.2A3.2 3.2 0 1 1 12 8.8a3.2 3.2 0 0 1 0 6.4Z" />
      </svg>
    </button>
    <div class="settings-backdrop" :class="{ 'is-open': state.settingsOpen }" aria-hidden="true" @click="state.settingsOpen = false"></div>
    <aside class="settings-panel" :class="{ 'is-open': state.settingsOpen }" role="dialog" aria-modal="true" aria-labelledby="settingsTitle" :aria-hidden="String(!state.settingsOpen)">
      <div class="settings-panel-head">
        <div>
          <p class="eyebrow compact">我的 API 设置</p>
          <h2 id="settingsTitle">模型连接</h2>
        </div>
        <button class="settings-close" type="button" aria-label="关闭设置" @click="state.settingsOpen = false">×</button>
      </div>
      <p class="settings-note">这里保存的是当前账号自己的接口地址、密钥和模型名，不会同步到其他用户。</p>
      <div class="settings-form">
        <label class="settings-field">
          <span>模型地址</span>
          <input v-model="state.ai.baseUrl" autocomplete="off" placeholder="https://api.openai.com/v1" />
        </label>
        <label class="settings-field">
          <span>API 密钥</span>
          <input v-model="state.ai.apiKey" type="password" autocomplete="off" placeholder="留空则沿用已保存密钥" />
        </label>
        <div class="settings-row">
          <button class="settings-secondary" type="button" :disabled="state.aiBusy" @click="fetchModels">获取模型</button>
          <span class="settings-status">{{ state.ai.status }}</span>
        </div>
        <label class="settings-field">
          <span>模型名</span>
          <select v-model="state.ai.chatModel" :disabled="!state.ai.models.length">
            <option v-for="model in state.ai.models" :key="model" :value="model">{{ model }}</option>
          </select>
        </label>
        <div class="settings-actions">
          <button class="settings-primary" type="button" :disabled="state.aiBusy" @click="saveAiSettings">保存并应用</button>
        </div>
      </div>
    </aside>

    <div class="wordbank-dialog-backdrop" :class="{ 'is-open': wordDialogVisible }" aria-hidden="true" @click="state.selectedWord = null"></div>
    <aside class="wordbank-dialog" :class="{ 'is-open': wordDialogVisible }" role="dialog" aria-modal="true" aria-labelledby="wordBankDialogTitle" :aria-hidden="String(!wordDialogVisible)">
      <div v-if="state.selectedWord" class="wordbank-dialog-shell">
        <div class="wordbank-dialog-head">
          <div>
            <p class="eyebrow compact">单词详情</p>
            <h2 id="wordBankDialogTitle">{{ state.selectedWord.word }}</h2>
            <p class="word-meta">{{ phonetic(state.selectedWord) }} · {{ state.selectedWord.partOfSpeech }} · {{ state.selectedWord.levelTag }}</p>
          </div>
          <div class="wordbank-dialog-actions">
            <button class="favorite-button wordbank-dialog-favorite" :class="{ 'is-active': state.selectedWord.favorite }" type="button" :aria-pressed="String(Boolean(state.selectedWord.favorite))" @click="toggleFavorite(state.selectedWord)">★</button>
            <button class="wordbank-dialog-close" type="button" aria-label="关闭" @click="state.selectedWord = null">×</button>
          </div>
        </div>
        <div class="wordbank-dialog-meaning">
          <span>中文释义</span>
          <p>{{ state.selectedWord.meaningCn }}</p>
        </div>
        <div class="wordbank-dialog-example">
          <span>例句</span>
          <p v-html="highlightTerm(wordCardExampleEn(state.selectedWord), state.selectedWord.word)"></p>
          <small>{{ wordCardExampleCn(state.selectedWord) }}</small>
        </div>
        <div class="wordbank-dialog-detail-actions">
          <button
            class="quiz-detail-button"
            :class="{ 'is-loading': selectedWordDetail?.status === 'loading', 'is-done': selectedWordDetail?.status === 'ready' }"
            type="button"
            :disabled="selectedWordDetail?.status === 'loading'"
            @click="loadWordDetail(state.selectedWord)"
          >
            {{ selectedWordDetail?.status === "loading" ? "生成中..." : selectedWordDetail?.status === "ready" ? "已生成解析" : "深度解析" }}
          </button>
        </div>
        <section
          v-if="selectedWordDetail"
          class="quiz-ai-detail wordbank"
          :class="{ 'is-loading': selectedWordDetail.status === 'loading', 'is-error': selectedWordDetail.status === 'error' }"
        >
          <template v-if="selectedWordDetail.status === 'loading'">
            <span class="quiz-ai-spinner" aria-hidden="true"></span>
            <span>正在生成 {{ state.selectedWord.word }} 的深度解析...</span>
          </template>
          <template v-else-if="selectedWordDetail.status === 'error'">
            <span>{{ selectedWordDetail.message || "模型未配置或暂时不可用，请检查 AI 设置" }}</span>
            <button class="quiz-detail-button retry-detail-button" type="button" @click="loadWordDetail(state.selectedWord, true)">重试</button>
          </template>
          <template v-else>
            <div class="quiz-ai-head">
              <strong>深度解析 · {{ selectedWordDetail.data?.word || state.selectedWord.word }}</strong>
              <span>{{ modelLabel(selectedWordDetail.data) }}{{ selectedWordDetail.data?.cached ? " · 已缓存" : "" }}</span>
            </div>
            <div class="quiz-ai-grid">
              <section v-if="selectedWordDetail.data?.examples?.length" class="quiz-ai-section">
                <h3>更多例句</h3>
                <div v-for="example in selectedWordDetail.data.examples" :key="example.en" class="quiz-ai-row">
                  <p v-html="highlightTerm(example.en || '', state.selectedWord.word)"></p>
                  <small v-if="example.cn">{{ example.cn }}</small>
                </div>
              </section>
              <section v-if="selectedWordDetail.data?.variants?.length" class="quiz-ai-section">
                <h3>词性变种</h3>
                <div v-for="variant in selectedWordDetail.data.variants" :key="`${variant.form}-${variant.type}`" class="quiz-ai-row">
                  <p><b>{{ variant.form }}</b> {{ variant.type }}</p>
                  <small v-if="variant.meaningCn">{{ variant.meaningCn }}</small>
                </div>
              </section>
              <section v-if="selectedWordDetail.data?.usageNotes?.length" class="quiz-ai-section">
                <h3>用法提示</h3>
                <div v-for="note in selectedWordDetail.data.usageNotes" :key="note" class="quiz-ai-row">
                  <p>{{ note }}</p>
                </div>
              </section>
              <section v-if="selectedWordDetail.data?.collocations?.length" class="quiz-ai-section">
                <h3>固定搭配</h3>
                <div v-for="item in selectedWordDetail.data.collocations" :key="`${item.phrase}-${item.meaningCn}`" class="quiz-ai-row">
                  <p><b>{{ item.phrase }}</b> {{ item.meaningCn }}</p>
                  <small v-if="item.exampleEn" v-html="highlightTerm(item.exampleEn, state.selectedWord.word)"></small>
                </div>
              </section>
            </div>
          </template>
        </section>
      </div>
    </aside>

    <div
      v-if="state.tooltip.visible"
      class="word-tooltip visible"
      :style="{ left: state.tooltip.x + 'px', top: state.tooltip.y + 'px' }"
      @mouseenter="cancelTooltipHide"
      @mouseleave="scheduleHideTooltip"
    >
      <div class="tooltip-head">
        <strong>{{ state.tooltip.data.word }}</strong>
        <span v-if="state.tooltip.loading" class="tooltip-status">查询中</span>
      </div>
      <div v-if="phonetic(state.tooltip.data) || state.tooltip.data.partOfSpeech" class="tooltip-meta">
        <span v-if="phonetic(state.tooltip.data)" class="phonetic">{{ phonetic(state.tooltip.data) }}</span>
        <span v-if="state.tooltip.data.partOfSpeech" class="tooltip-pos">{{ state.tooltip.data.partOfSpeech }}</span>
      </div>
      <p>{{ state.tooltip.data.meaningCn || "暂无释义" }}</p>
      <small v-if="state.tooltip.data.exampleEn">{{ state.tooltip.data.exampleEn }}</small>
      <button
        class="tooltip-save-word"
        type="button"
        :disabled="state.tooltip.loading || state.tooltip.data.savedInReviewBook"
        @click="saveReaderWord(state.tooltip.data.word)"
      >
        {{ state.tooltip.data.savedInReviewBook ? "已在生词本" : "加入生词本" }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ElMessage } from "element-plus";
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { aiApi, authApi, learningApi } from "@/api/modules";
import { formatDate, normalizeModelList } from "@/api/client";
import { learningMessageFromResponse, nextVocabularyPage } from "@/learning-advisor";

const route = useRoute();
const router = useRouter();
const readerRef = ref(null);
const ambientCanvasRef = ref(null);
const cursorCanvasRef = ref(null);
const cursorOrbRef = ref(null);
let stopAmbientScene = () => {};

const navItems = [
  { id: "dashboard", label: "首页", path: "/" },
  { id: "chat", label: "对话", path: "/chat" },
  { id: "vocabulary", label: "单词", path: "/vocabulary" },
  { id: "review", label: "复习", path: "/review" },
  { id: "writing", label: "作文", path: "/writing" },
  { id: "news", label: "阅读", path: "/news" },
  { id: "wordbank", label: "词库", path: "/wordbank" }
];

const reviewModes = [
  { id: "today", label: "今日复习" },
  { id: "favorites", label: "收藏词" },
  { id: "mastered", label: "已掌握" }
];

const newsDifficultyOptions = [
  { value: "", label: "全部" },
  { value: "easy", label: "简单" },
  { value: "medium", label: "进阶" },
  { value: "hard", label: "挑战" }
];

const chatSceneOptions = [
  { value: "daily", label: "日常交流" },
  { value: "exam", label: "考试口语" },
  { value: "business", label: "商务表达" },
  { value: "reading", label: "阅读讨论" }
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
  { value: "growth", label: "成长" }
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
  research: "研究"
};

const sectionMeta = {
  dashboard: ["探索今日学习岛", "从对话、背词、复习、写作和阅读中选择下一步。"],
  chat: ["对话练习", "保留 Agent 的 Observe、Think、Act、Answer 轨迹。"],
  vocabulary: ["单词闯关", "用选择题、例句和 AI 释义逐步加深记忆。"],
  review: ["复习挑战", "同时检查拼写和释义，错词会进入后续计划。"],
  writing: ["作文批改", "提交英文作文，获得评分、句子反馈和润色版本。"],
  news: ["新闻阅读", "在文章中悬停查词，记录阅读进度。"],
  wordbank: ["词库浏览", "按考试词库搜索、分页浏览和查看详情。"]
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
  words: [],
  quizCategory: "",
  quizCategories: [],
  quizIndex: 0,
  quizAnswers: {},
  quizOptions: [],
  quizCompleted: false,
  wordDetails: {},
  reviewMode: "today",
  reviewWords: [],
  reviewIndex: 0,
  reviewTotal: 0,
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
  readerProgress: 0,
  progressTimer: null,
  lookupCache: {},
  tooltipTimer: null,
  tooltip: { visible: false, loading: false, lookupKey: "", x: 0, y: 0, data: {} },
  translateBusy: false,
  translation: {},
  wordBank: { categories: [], words: [], total: 0, page: 0, size: 48, category: "", search: "", loaded: false },
  selectedWord: null,
  settingsOpen: false,
  ai: { baseUrl: "", apiKey: "", chatModel: "", models: [], status: "未连接" },
  aiBusy: false
});

const activeSection = computed(() => route.meta.section || "dashboard");
const sectionTitle = computed(() => sectionMeta[activeSection.value]?.[0] || sectionMeta.dashboard[0]);
const sectionSubtitle = computed(() => sectionMeta[activeSection.value]?.[1] || sectionMeta.dashboard[1]);
const userName = computed(() => state.currentUser?.nickname || state.currentUser?.username || "学习者");
const currentConversation = computed(() => state.conversations.find((item) => Number(item.id) === Number(state.conversationId)));
const conversationSelectValue = computed(() => state.conversationId ? `${state.conversationId}` : "new");
const currentConversationTitle = computed(() => {
  if (currentConversation.value?.title) return currentConversation.value.title;
  return state.conversationId ? "学习对话" : `新对话 · ${sceneLabel(state.chatScene)}`;
});
const levelPercent = computed(() => {
  const next = Number(state.level.nextLevelXp || 100);
  return Math.min(100, Math.round((Number(state.level.experience || 0) / next) * 100));
});
const routeDoneCount = computed(() => journeySteps.value.filter((step) => step.done).length);
const routeProgressPercent = computed(() => Math.round((routeDoneCount.value / Math.max(1, journeySteps.value.length)) * 100));
const journeyEnergy = computed(() => Math.max(levelPercent.value, Math.round((routeDoneCount.value / journeySteps.value.length) * 100)));
const focusStep = computed(() => journeySteps.value.find((step) => !step.done) || journeySteps.value[0]);
const currentQuizWord = computed(() => state.words[state.quizIndex]);
const quizResultForCurrent = computed(() => currentQuizWord.value ? state.quizAnswers[currentQuizWord.value.id] : null);
const wordDetailForCurrent = computed(() => currentQuizWord.value ? state.wordDetails[currentQuizWord.value.id] : null);
const currentReviewWord = computed(() => state.reviewWords[state.reviewIndex]);
const selectedWordDetail = computed(() => state.selectedWord ? state.wordDetails[state.selectedWord.id] : null);
const quizOptions = computed(() => state.quizOptions[state.quizIndex] || []);
const quizAnsweredCount = computed(() => Object.keys(state.quizAnswers || {}).length);
const quizProgress = computed(() => {
  const total = Math.max(1, state.words.length);
  return Math.round(((state.quizIndex + (quizResultForCurrent.value ? 1 : 0)) / total) * 100);
});
const reviewTotal = computed(() => Math.max(1, state.reviewTotal || state.reviewWords.length));
const reviewRemaining = computed(() => state.reviewWords.length);
const reviewProgress = computed(() => {
  const total = reviewTotal.value;
  const passed = Math.max(0, total - reviewRemaining.value);
  return Math.round((passed / total) * 100);
});
const reviewEmptyText = computed(() => {
  if (state.reviewMode === "favorites") return "还没有收藏词，先在背词或词库里点亮收藏。";
  if (state.reviewMode === "mastered") return "还没有已掌握的单词，继续完成背词和复习后会出现在这里。";
  return "这个复习队列暂时为空。";
});
const newsTopicOptions = computed(() => {
  const topics = new Map();
  curatedNewsTopics.forEach((topic) => topics.set(topic.value, topic));
  (state.newsTopics || [])
    .flatMap(splitArticleTopics)
    .forEach((topic) => {
      if (!topics.has(topic)) {
        topics.set(topic, { value: topic, label: newsTopicLabel(topic) });
      }
    });
  return Array.from(topics.values());
});
const readerProgress = computed(() => state.currentArticle?.readProgress ?? state.readerProgress ?? 0);
const wordBankTotalPages = computed(() => Math.max(1, Math.ceil((state.wordBank.total || 0) / state.wordBank.size)));
const wordDialogVisible = computed({
  get: () => Boolean(state.selectedWord),
  set: (value) => {
    if (!value) state.selectedWord = null;
  }
});

const journeySteps = computed(() => {
  const report = state.report || {};
  const answeredWords = Object.keys(state.quizAnswers || {}).length;
  const reviewDone = Number(report.reviewWords || 0) === 0 || state.reviewWords.length === 0;
  const steps = [
    ["chat", "chat", "开口练", "会话营地", "说", "green", 16, 63, state.messages.some((item) => item.role === "user"), "完成一次问答"],
    ["vocabulary", "vocabulary", "记单词", "词汇森林", "词", "blue", 34, 31, answeredWords >= Math.min(6, state.words.length || 6), `${answeredWords} 个已作答`],
    ["review", "review", "复习错词", "复习山口", "复", "amber", 53, 56, reviewDone, `${report.reviewWords || 0} 个待复习`],
    ["writing", "writing", "改作文", "写作工坊", "写", "coral", 72, 78, Number(report.essayCount || 0) > 0 || Boolean(state.writingResult), `${report.essayCount || 0} 篇已批改`],
    ["news", "news", "读文章", "阅读灯塔", "读", "violet", 82, 25, Number(report.readingCount || 0) > 0 || Boolean(state.currentArticle?.finished), `${report.readingCount || 0} 篇记录`]
  ];
  const firstPending = steps.find((item) => !item[8])?.[0];
  return steps.map(([id, section, title, landmark, symbol, tone, mapX, mapY, done, detail]) => ({
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
    recommended: id === firstPending
  }));
});

function routeStepAriaLabel(step) {
  const status = step.done ? "已完成" : step.recommended ? "推荐下一关" : "未完成";
  return `${step.title}，${step.landmark}，${step.detail}，${status}`;
}

const articleTokens = computed(() => {
  const text = state.currentArticle?.content || "";
  const parts = text.match(/[A-Za-z][A-Za-z'-]*|[^A-Za-z]+/g) || [];
  return parts.map((text, index) => ({ key: `${index}-${text}`, text, word: /^[A-Za-z][A-Za-z'-]*$/.test(text) }));
});

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
  return chatSceneOptions.find((item) => item.value === value)?.label || value || "日常交流";
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

async function handleLearningAction(action) {
  if (action.section === "review" && reviewModes.some((mode) => mode.id === action.query?.mode)) {
    state.reviewMode = action.query.mode;
    await loadReviews();
  }
  await goSection(action.section, action.query || {});
}

async function loadLearningCardPage(message, index, direction) {
  const card = message.learningCard;
  const page = nextVocabularyPage(card, direction);
  if (page === card.page) return;
  try {
    const result = await learningApi.advisorVocabulary({ group: card.group, page, size: card.size });
    state.messages[index] = learningMessageFromResponse(result);
  } catch (error) {
    ElMessage.error(error.message || "词库分页加载失败");
  }
}

async function logout() {
  await authApi.logout();
  router.replace("/login");
}

function sameLocalDay(value) {
  if (!value) return false;
  const date = new Date(String(value).replace(" ", "T"));
  const now = new Date();
  return date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate();
}

async function loadReport() {
  const [report, level] = await Promise.all([learningApi.report(), learningApi.level()]);
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
  for (let offset = 1; offset < state.words.length && distractors.length < 3; offset += 1) {
    const candidate = state.words[(index + offset) % state.words.length];
    if (candidate?.id !== word.id && candidate?.meaningCn && candidate.meaningCn !== word.meaningCn && !distractors.includes(candidate.meaningCn)) {
      distractors.push(candidate.meaningCn);
    }
  }
  ["清晰的；明确的", "自然的表达方式", "复习计划；学习安排", "阅读材料；文章"].forEach((item) => {
    if (distractors.length < 3 && item !== word.meaningCn && !distractors.includes(item)) {
      distractors.push(item);
    }
  });
  const options = [word.meaningCn, ...distractors.slice(0, 3)].filter(Boolean);
  const rotateBy = options.length ? Number(word.id || index) % options.length : 0;
  return [...options.slice(rotateBy), ...options.slice(0, rotateBy)];
}

async function loadQuizCategories() {
  state.quizCategories = await learningApi.quizCategories();
}

async function loadWords(updateToday = true) {
  state.words = await learningApi.todayWords({ limit: 8, category: state.quizCategory });
  state.quizIndex = 0;
  state.quizAnswers = {};
  state.quizOptions = state.words.map((word, index) => buildQuizOptions(word, index));
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
  if (!currentQuizWord.value || quizResultForCurrent.value) return;
  const result = await learningApi.quiz(currentQuizWord.value.id, { answer });
  state.quizAnswers[currentQuizWord.value.id] = { ...result, selectedAnswer: answer };
  applyWordProgress(result.wordId, result);
  await Promise.all([loadReport(), loadReviews()]);
}

async function loadWordDetail(word, force = false) {
  if (!word) return;
  if (state.wordDetails[word.id]?.status === "ready" && !force) return;
  state.wordDetails[word.id] = { status: "loading" };
  try {
    state.wordDetails[word.id] = { status: "ready", data: await learningApi.wordAiDetail(word.id) };
  } catch (error) {
    state.wordDetails[word.id] = {
      status: "error",
      message: error.message || "模型未配置或暂时不可用，请检查 AI 设置"
    };
    ElMessage.error(state.wordDetails[word.id].message);
  }
}

function applyWordProgress(wordId, result) {
  patchWord(wordId, (word) => ({
    ...word,
    familiarity: result.familiarity ?? word.familiarity,
    wrongCount: result.wrongCount ?? word.wrongCount,
    exampleEn: result.exampleEn || word.exampleEn,
    exampleCn: result.exampleCn || word.exampleCn
  }));
}

function patchWord(wordId, patcher) {
  const numericId = Number(wordId);
  const patchList = (words) => words.map((word) => (Number(word.id) === numericId ? patcher(word) : word));
  state.words = patchList(state.words);
  state.reviewWords = patchList(state.reviewWords);
  state.wordBank.words = patchList(state.wordBank.words);
  if (state.selectedWord && Number(state.selectedWord.id) === numericId) {
    state.selectedWord = patcher(state.selectedWord);
  }
}

function applyFavoriteUpdate(updated) {
  patchWord(updated.id, (word) => ({ ...word, favorite: updated.favorite }));
  if (state.reviewMode === "favorites" && !updated.favorite) {
    state.reviewWords = state.reviewWords.filter((word) => word.id !== updated.id);
    state.reviewTotal = state.reviewWords.length;
  }
}

async function toggleFavorite(word) {
  if (!word) return;
  try {
    const updated = await learningApi.toggleFavorite(word.id);
    applyFavoriteUpdate(updated);
    await loadReport();
  } catch (error) {
    ElMessage.error(error.message || "收藏失败");
  }
}

async function loadReviews() {
  const limit = state.reviewMode === "today" ? 8 : 20;
  state.reviewWords = await learningApi.reviews(state.reviewMode, limit);
  state.reviewIndex = 0;
  state.reviewTotal = state.reviewWords.length;
  state.lastReviewResult = null;
  state.reviewPendingResult = false;
  state.reviewCompleted = false;
  state.reviewForm.spellingAnswer = "";
  state.reviewForm.meaningAnswer = "";
}

async function changeReviewMode(mode) {
  state.reviewMode = mode;
  await loadReviews();
}

async function submitReview() {
  if (!currentReviewWord.value || state.reviewBusy || state.reviewPendingResult) return;
  if (!state.reviewForm.spellingAnswer || !state.reviewForm.meaningAnswer) {
    ElMessage.warning("请同时填写英文拼写和中文释义");
    return;
  }
  state.reviewBusy = true;
  try {
    state.lastReviewResult = await learningApi.submitReview(currentReviewWord.value.id, { ...state.reviewForm });
    applyWordProgress(currentReviewWord.value.id, state.lastReviewResult);
    state.reviewPendingResult = true;
    await loadReport();
  } catch (error) {
    state.lastReviewResult = {
      correct: false,
      spellingCorrect: false,
      meaningCorrect: false,
      expectedWord: currentReviewWord.value.word,
      expectedMeaning: error.message || "提交失败，请稍后重试",
      nextReviewHint: "检查网络或稍后再试"
    };
    state.reviewPendingResult = true;
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
  return result?.exampleEn || word?.exampleEn || `Try to use ${word?.word || "this word"} in a complete sentence.`;
}

function quizExampleCn(word, result = {}) {
  return result?.exampleCn || word?.exampleCn || "试着把这个单词放进一个完整句子里。";
}

function wordCardExampleEn(word) {
  return word?.exampleEn || `The word "${word?.word || "this word"}" is useful in English practice.`;
}

function wordCardExampleCn(word) {
  return word?.exampleCn || `单词“${word?.word || "这个单词"}”在英语练习中很有用。`;
}

function strictReviewParts(result = {}) {
  return [
    result.spellingCorrect ? "拼写正确" : "拼写待修正",
    result.meaningCorrect ? "释义正确" : "释义待加强"
  ];
}

function reviewCloze(word) {
  const example = word?.exampleEn || `Use ${word?.word || "this word"} in a complete sentence.`;
  if (!word?.word) return escapeHtml(example);
  const pieces = String(example).split(new RegExp(`(${escapeRegExp(word.word)})`, "gi"));
  return pieces.map((piece) => (
    piece.toLowerCase() === String(word.word).toLowerCase()
      ? `<mark class="review-blank">_____</mark>`
      : escapeHtml(piece)
  )).join("");
}

function highlightTerm(text, term) {
  if (!term) return escapeHtml(text || "");
  const pieces = String(text || "").split(new RegExp(`(${escapeRegExp(term)})`, "gi"));
  return pieces.map((piece) => (
    piece.toLowerCase() === String(term).toLowerCase()
      ? `<mark>${escapeHtml(piece)}</mark>`
      : escapeHtml(piece)
  )).join("");
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

function initAmbientCanvas() {
  const canvas = ambientCanvasRef.value;
  if (!canvas || window.matchMedia("(prefers-reduced-motion: reduce)").matches) return () => {};

  const cursorCanvas = cursorCanvasRef.value;
  const cursorOrb = cursorOrbRef.value;
  const ctx = canvas.getContext("2d");
  const cursorCtx = cursorCanvas?.getContext("2d");
  if (!ctx) return () => {};

  const tokens = ["context", "review", "essay", "listen", "speak", "read", "word", "grammar", "story", "phrase", "focus", "fluent"];
  const palette = [
    { core: "rgba(88, 204, 2, 0.18)", glow: "rgba(88, 204, 2, 0.07)", rim: "rgba(88, 204, 2, 0.38)", ink: "rgba(61, 146, 0, 0.68)" },
    { core: "rgba(28, 176, 246, 0.18)", glow: "rgba(28, 176, 246, 0.07)", rim: "rgba(28, 176, 246, 0.36)", ink: "rgba(7, 99, 146, 0.68)" },
    { core: "rgba(255, 200, 0, 0.18)", glow: "rgba(255, 200, 0, 0.07)", rim: "rgba(255, 200, 0, 0.36)", ink: "rgba(106, 82, 0, 0.66)" },
    { core: "rgba(139, 116, 255, 0.14)", glow: "rgba(139, 116, 255, 0.06)", rim: "rgba(139, 116, 255, 0.3)", ink: "rgba(77, 59, 165, 0.62)" }
  ];
  const bubbles = [];
  const trail = [];
  let width = 0;
  let height = 0;
  let ratio = 1;
  let hoveredBubble = null;
  let animationFrame = 0;
  let lastFrameTime = 0;

  function fontSize(bubble) {
    const base = width < 760 ? 12 : 15;
    return Math.round(base * Math.min(1.14, Math.max(0.94, bubble.scale)));
  }

  function radiusFor(bubble) {
    ctx.save();
    ctx.font = `900 ${fontSize(bubble)}px Segoe UI, Arial`;
    const textWidth = ctx.measureText(bubble.token).width;
    ctx.restore();
    const minRadius = (width < 760 ? 34 : 42) * bubble.scale;
    return Math.max(minRadius, textWidth / 2 + (width < 760 ? 18 : 23));
  }

  function collisionRadius(bubble) {
    return radiusFor(bubble) + 8;
  }

  function desiredCount() {
    const base = Math.round((width * height) / (width < 760 ? 62000 : 56000));
    return Math.max(width < 760 ? 8 : 15, Math.min(width < 760 ? 16 : 32, base));
  }

  function randomSpeed() {
    const min = width < 760 ? 0.2 : 0.28;
    const max = width < 760 ? 0.34 : 0.5;
    return min + Math.random() * (max - min);
  }

  function setVelocity(bubble) {
    const angle = Math.random() * Math.PI * 2;
    bubble.speed = randomSpeed();
    bubble.vx = Math.cos(angle) * bubble.speed;
    bubble.vy = Math.sin(angle) * bubble.speed;
  }

  function placeBubble(bubble) {
    const radius = collisionRadius(bubble);
    let best = null;
    let bestGap = -Infinity;
    for (let attempt = 0; attempt < 80; attempt += 1) {
      const x = radius + Math.random() * Math.max(1, width - radius * 2);
      const y = radius + Math.random() * Math.max(1, height - radius * 2);
      let nearestGap = Infinity;
      const clear = bubbles.every((other) => {
        if (other === bubble) return true;
        const gap = Math.hypot(x - other.x, y - other.y) - (radius + collisionRadius(other) + 10);
        nearestGap = Math.min(nearestGap, gap);
        return gap >= 0;
      });
      if (nearestGap > bestGap) {
        bestGap = nearestGap;
        best = { x, y };
      }
      if (clear) {
        bubble.x = x;
        bubble.y = y;
        return;
      }
    }
    bubble.x = best?.x ?? radius + Math.random() * Math.max(1, width - radius * 2);
    bubble.y = best?.y ?? radius + Math.random() * Math.max(1, height - radius * 2);
  }

  function prepareBubble(bubble, index, shouldPlace) {
    bubble.token = tokens[Math.floor(Math.random() * tokens.length)];
    bubble.theme = palette[(index + Math.floor(Math.random() * palette.length)) % palette.length];
    bubble.scale = 0.92 + Math.random() * 0.34;
    bubble.phase = Math.random() * Math.PI * 2;
    bubble.age = 0;
    bubble.life = 10 + Math.random() * 8;
    bubble.bump = 0;
    setVelocity(bubble);
    if (shouldPlace) placeBubble(bubble);
  }

  function createBubble(index) {
    const bubble = { x: 0, y: 0, vx: 0, vy: 0, speed: 0, phase: 0, scale: 1, token: "", theme: palette[index % palette.length], age: 0, life: 12, bump: 0 };
    prepareBubble(bubble, index, true);
    return bubble;
  }

  function containBubble(bubble) {
    const radius = collisionRadius(bubble);
    if (bubble.x < radius) {
      bubble.x = radius;
      bubble.vx = Math.abs(bubble.vx);
    } else if (bubble.x > width - radius) {
      bubble.x = width - radius;
      bubble.vx = -Math.abs(bubble.vx);
    }
    if (bubble.y < radius) {
      bubble.y = radius;
      bubble.vy = Math.abs(bubble.vy);
    } else if (bubble.y > height - radius) {
      bubble.y = height - radius;
      bubble.vy = -Math.abs(bubble.vy);
    }
  }

  function limitVelocity(bubble) {
    const minSpeed = width < 760 ? 0.18 : 0.25;
    const maxSpeed = width < 760 ? 0.4 : 0.56;
    const speed = Math.hypot(bubble.vx, bubble.vy);
    if (!speed) {
      setVelocity(bubble);
      return;
    }
    const nextSpeed = Math.min(maxSpeed, Math.max(minSpeed, speed));
    bubble.vx = (bubble.vx / speed) * nextSpeed;
    bubble.vy = (bubble.vy / speed) * nextSpeed;
    bubble.speed = nextSpeed;
  }

  function resolveCollisions(iterations = 2) {
    for (let pass = 0; pass < iterations; pass += 1) {
      for (let i = 0; i < bubbles.length; i += 1) {
        for (let j = i + 1; j < bubbles.length; j += 1) {
          const a = bubbles[i];
          const b = bubbles[j];
          const minDistance = collisionRadius(a) + collisionRadius(b) + 2;
          let dx = a.x - b.x;
          let dy = a.y - b.y;
          let distance = Math.hypot(dx, dy);
          if (!distance) {
            dx = 1;
            dy = 0;
            distance = 1;
          }
          if (distance >= minDistance) continue;
          const nx = dx / distance;
          const ny = dy / distance;
          const overlap = minDistance - distance;
          const aFixed = a === hoveredBubble;
          const bFixed = b === hoveredBubble;
          if (aFixed && !bFixed) {
            b.x -= nx * overlap;
            b.y -= ny * overlap;
          } else if (bFixed && !aFixed) {
            a.x += nx * overlap;
            a.y += ny * overlap;
          } else {
            a.x += nx * overlap * 0.5;
            a.y += ny * overlap * 0.5;
            b.x -= nx * overlap * 0.5;
            b.y -= ny * overlap * 0.5;
          }
          a.bump = 1;
          b.bump = 1;
          limitVelocity(a);
          limitVelocity(b);
          containBubble(a);
          containBubble(b);
        }
      }
    }
  }

  function seedBubbles() {
    const target = desiredCount();
    while (bubbles.length < target) bubbles.push(createBubble(bubbles.length));
    bubbles.length = target;
    bubbles.forEach((bubble, index) => {
      prepareBubble(bubble, index, false);
      containBubble(bubble);
    });
    resolveCollisions(8);
  }

  function resize() {
    ratio = Math.min(window.devicePixelRatio || 1, 2);
    width = window.innerWidth;
    height = window.innerHeight;
    canvas.width = Math.floor(width * ratio);
    canvas.height = Math.floor(height * ratio);
    canvas.style.width = `${width}px`;
    canvas.style.height = `${height}px`;
    ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
    if (cursorCanvas && cursorCtx) {
      cursorCanvas.width = Math.floor(width * ratio);
      cursorCanvas.height = Math.floor(height * ratio);
      cursorCanvas.style.width = `${width}px`;
      cursorCanvas.style.height = `${height}px`;
      cursorCtx.setTransform(ratio, 0, 0, ratio, 0, 0);
    }
    seedBubbles();
  }

  function findHoveredBubble(x, y) {
    for (let index = bubbles.length - 1; index >= 0; index -= 1) {
      const bubble = bubbles[index];
      if (Math.hypot(x - bubble.x, y - bubble.y) <= radiusFor(bubble)) return bubble;
    }
    return null;
  }

  function recordPointer(event) {
    if (cursorOrb && window.matchMedia("(pointer: fine)").matches) {
      document.body.classList.add("has-custom-cursor");
      cursorOrb.style.left = `${event.clientX}px`;
      cursorOrb.style.top = `${event.clientY}px`;
    }
    hoveredBubble = findHoveredBubble(event.clientX, event.clientY);
    trail.unshift({ x: event.clientX, y: event.clientY, life: 1 });
    if (trail.length > 42) trail.length = 42;
  }

  function lifeAlpha(bubble) {
    const fadeIn = Math.min(1, bubble.age / 1.2);
    const fadeOut = Math.min(1, Math.max(0, (bubble.life - bubble.age) / 1.6));
    return Math.max(0, Math.min(fadeIn, fadeOut));
  }

  function drawTrail() {
    if (!cursorCtx || trail.length < 2) return;
    cursorCtx.lineCap = "round";
    cursorCtx.lineJoin = "round";
    cursorCtx.save();
    cursorCtx.shadowColor = "rgba(88, 204, 2, 0.48)";
    cursorCtx.shadowBlur = 16;
    for (let index = 0; index < trail.length - 1; index += 1) {
      const point = trail[index];
      const next = trail[index + 1];
      const alpha = Math.max(0, point.life) * 0.82;
      cursorCtx.strokeStyle = `rgba(88, 204, 2, ${alpha})`;
      cursorCtx.lineWidth = 12 * point.life;
      cursorCtx.beginPath();
      cursorCtx.moveTo(point.x, point.y);
      cursorCtx.lineTo(next.x, next.y);
      cursorCtx.stroke();
      cursorCtx.strokeStyle = `rgba(28, 176, 246, ${alpha * 0.62})`;
      cursorCtx.lineWidth = 4 * point.life;
      cursorCtx.beginPath();
      cursorCtx.moveTo(point.x, point.y);
      cursorCtx.lineTo(next.x, next.y);
      cursorCtx.stroke();
    }
    cursorCtx.restore();
  }

  function drawBubbles(time) {
    const dt = Math.min(2.2, Math.max(0.5, (time - lastFrameTime) / 16.67 || 1));
    lastFrameTime = time;
    bubbles.forEach((bubble, index) => {
      if (bubble !== hoveredBubble) {
        bubble.age += dt / 60;
        if (bubble.age >= bubble.life) prepareBubble(bubble, index, true);
        bubble.x += bubble.vx * dt;
        bubble.y += bubble.vy * dt;
      }
      containBubble(bubble);
    });
    resolveCollisions();
    bubbles.forEach(limitVelocity);

    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    bubbles.forEach((bubble) => {
      const hovered = bubble === hoveredBubble;
      const pulse = hovered ? 1 : 0.78 + Math.sin(time / 1100 + bubble.phase) * 0.06;
      const bump = Math.max(0, bubble.bump || 0);
      const radius = radiusFor(bubble) * (1 + bump * 0.08);
      const theme = bubble.theme || palette[0];
      ctx.save();
      ctx.globalAlpha = lifeAlpha(bubble) * pulse * (width < 760 ? 0.72 : 0.88);
      if (hovered || bump > 0.02) {
        ctx.shadowColor = "rgba(35, 49, 67, 0.12)";
        ctx.shadowBlur = hovered ? 12 : 8;
      }
      const fill = ctx.createRadialGradient(bubble.x - radius * 0.36, bubble.y - radius * 0.42, 1, bubble.x, bubble.y, radius * 1.28);
      fill.addColorStop(0, "rgba(255, 255, 255, 0.96)");
      fill.addColorStop(0.2, "rgba(255, 255, 255, 0.5)");
      fill.addColorStop(0.62, theme.core);
      fill.addColorStop(1, theme.glow);
      ctx.fillStyle = fill;
      ctx.strokeStyle = theme.rim;
      ctx.lineWidth = hovered ? 2 : 1.4;
      ctx.beginPath();
      ctx.arc(bubble.x, bubble.y, radius, 0, Math.PI * 2);
      ctx.fill();
      ctx.stroke();
      ctx.strokeStyle = "rgba(255, 255, 255, 0.58)";
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.arc(bubble.x - radius * 0.08, bubble.y - radius * 0.1, radius * 0.67, Math.PI * 1.06, Math.PI * 1.73);
      ctx.stroke();
      ctx.fillStyle = "rgba(255, 255, 255, 0.62)";
      ctx.beginPath();
      ctx.arc(bubble.x - radius * 0.36, bubble.y - radius * 0.42, radius * 0.14, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = theme.ink;
      ctx.font = `900 ${fontSize(bubble)}px Segoe UI, Arial`;
      ctx.fillText(bubble.token, bubble.x, bubble.y);
      ctx.restore();
      bubble.bump = bump * 0.86;
    });
  }

  function render(time) {
    ctx.clearRect(0, 0, width, height);
    if (cursorCtx) cursorCtx.clearRect(0, 0, width, height);
    drawBubbles(time);
    drawTrail();
    for (let index = trail.length - 1; index >= 0; index -= 1) {
      trail[index].life -= 0.025;
      if (trail[index].life <= 0) trail.splice(index, 1);
    }
    animationFrame = window.requestAnimationFrame(render);
  }

  resize();
  window.addEventListener("resize", resize);
  window.addEventListener("pointermove", recordPointer, { passive: true });
  animationFrame = window.requestAnimationFrame(render);

  return () => {
    window.cancelAnimationFrame(animationFrame);
    window.removeEventListener("resize", resize);
    window.removeEventListener("pointermove", recordPointer);
    document.body.classList.remove("has-custom-cursor");
  };
}

async function loadConversations() {
  state.conversations = await learningApi.conversations();
  if (state.conversations.length && !state.conversationId) {
    await openConversation(state.conversations[0].id);
  } else if (!state.messages.length) {
    state.messages = [{ role: "assistant", content: "你好，我是 LinguaMate。可以陪你练对话、背单词、批改作文，也能根据配置好的模型回答问题。" }];
    state.trace = [
      { phase: "Observe", title: "等待输入", detail: "选择场景或直接输入问题。" },
      { phase: "Think", title: "准备回答", detail: "已载入本地学习工具和模型配置。" }
    ];
  }
  return state.conversations;
}

async function openConversation(id) {
  state.conversationId = id;
  const conversation = state.conversations.find((item) => Number(item.id) === Number(id));
  if (conversation?.scene) state.chatScene = conversation.scene;
  state.messages = await learningApi.messages(id);
}

async function handleConversationSelect(value) {
  if (value === "new") {
    newConversation();
    return;
  }
  const id = Number(value);
  if (Number.isFinite(id)) await openConversation(id);
}

function newConversation() {
  state.conversationId = null;
  state.messages = [];
  state.trace = [];
  state.chatInput = "";
}

async function sendChat() {
  if (!state.chatInput || state.chatBusy) return;
  const message = state.chatInput;
  state.chatInput = "";
  state.messages.push({ role: "user", content: message });
  state.chatBusy = true;
  try {
    const result = await learningApi.chat({
      conversationId: state.conversationId,
      scene: state.chatScene,
      message
    });
    state.conversationId = result.conversationId;
    state.messages.push(learningMessageFromResponse(result));
    state.trace = result.trace || [];
    await Promise.all([loadConversations(), loadReport()]);
  } finally {
    state.chatBusy = false;
  }
}

async function reviewEssay() {
  state.writingBusy = true;
  try {
    state.writingResult = await learningApi.writingReview({ ...state.writing });
    state.trace = state.writingResult.trace || state.trace;
    await loadReport();
  } finally {
    state.writingBusy = false;
  }
}

async function loadNewsTopics() {
  state.newsTopics = await learningApi.newsTopics();
}

async function loadNews() {
  state.newsList = await learningApi.newsRecommendations({
    difficulty: state.newsDifficulty,
    topic: state.newsTopic
  });
  const currentVisible = state.newsList.some((article) => article.id === state.currentArticle?.id);
  if (!state.newsList.length) {
    state.currentArticle = null;
    state.translation = {};
    state.readerProgress = 0;
    return;
  }
  if (!currentVisible) {
    await loadArticle(state.newsList[0].id);
  }
}

async function loadArticle(id) {
  state.currentArticle = await learningApi.article(id);
  state.translation = {};
  state.readerProgress = state.currentArticle.readProgress || 0;
  await nextTick();
  if (readerRef.value) readerRef.value.scrollTop = 0;
}

function calculateReaderProgress() {
  const el = readerRef.value;
  if (!el || !state.currentArticle) return state.readerProgress;
  const max = el.scrollHeight - el.clientHeight;
  if (max <= 0) return state.readerProgress;
  return Math.min(100, Math.max(state.readerProgress, Math.round((el.scrollTop / max) * 100)));
}

function handleReaderScroll() {
  if (!state.currentArticle) return;
  const nextProgress = calculateReaderProgress();
  state.readerProgress = nextProgress;
  clearTimeout(state.progressTimer);
  state.progressTimer = setTimeout(() => updateReadingProgress(nextProgress, false), 500);
}

async function updateReadingProgress(progress, finished) {
  if (!state.currentArticle) return;
  const result = await learningApi.articleProgress(state.currentArticle.id, {
    readProgress: progress,
    finished
  });
  state.currentArticle.readProgress = result.readProgress;
  state.currentArticle.finished = result.finished;
  state.readerProgress = result.readProgress;
  await loadReport();
}

async function finishReading() {
  await updateReadingProgress(100, true);
  ElMessage.success("阅读进度已完成");
}

async function translateArticle() {
  if (!state.currentArticle) return;
  state.translateBusy = true;
  try {
    state.translation = await learningApi.translateArticle({
      title: state.currentArticle.title,
      content: state.currentArticle.content
    });
  } finally {
    state.translateBusy = false;
  }
}

const lookupStopWords = new Set(["a", "an", "the", "and", "or", "but", "to", "of", "in", "on", "at", "by", "for", "with", "is", "are", "was", "were", "be", "been", "it", "its", "they", "them", "this", "that"]);

function normalizeLookupWord(word) {
  return String(word || "")
    .toLowerCase()
    .replace(/^[^a-z]+|[^a-z]+$/g, "");
}

function placeTooltip(event) {
  const width = 310;
  const height = 210;
  const margin = 14;
  const x = event.clientX + 14;
  const y = event.clientY + 16;
  state.tooltip.x = Math.max(margin, Math.min(x, window.innerWidth - width - margin));
  state.tooltip.y = Math.max(margin, Math.min(y, window.innerHeight - height - margin));
}

async function showTooltip(word, event) {
  const normalized = normalizeLookupWord(word);
  if (!normalized || lookupStopWords.has(normalized)) return;
  cancelTooltipHide();
  placeTooltip(event);
  state.tooltip.visible = true;
  state.tooltip.lookupKey = normalized;
  state.tooltip.loading = !state.lookupCache[normalized];
  state.tooltip.data = state.lookupCache[normalized] || { word: normalized, meaningCn: "查询中..." };
  if (state.lookupCache[normalized]) {
    return;
  }
  try {
    const result = await learningApi.lookupWord(normalized, state.currentArticle?.id);
    state.lookupCache[normalized] = result;
    if (state.tooltip.visible && state.tooltip.lookupKey === normalized) {
      state.tooltip.data = result;
    }
  } catch (error) {
    if (state.tooltip.visible && state.tooltip.lookupKey === normalized) {
      state.tooltip.data = { word: normalized, meaningCn: "查询失败，请稍后重试" };
    }
  } finally {
    if (state.tooltip.lookupKey === normalized) {
      state.tooltip.loading = false;
    }
  }
}

function scheduleHideTooltip() {
  clearTimeout(state.tooltipTimer);
  state.tooltipTimer = setTimeout(() => {
    state.tooltip.visible = false;
  }, 240);
}

function cancelTooltipHide() {
  clearTimeout(state.tooltipTimer);
}

async function saveReaderWord(word) {
  if (!word || state.tooltip.loading) return;
  const result = await learningApi.saveDictionaryWord(word);
  const normalized = normalizeLookupWord(word);
  state.lookupCache[normalized] = result;
  state.tooltip.data = result;
  ElMessage.success("已加入生词本");
  await loadReport();
}

let wordBankSearchTimer;

async function loadWordBank() {
  if (!state.wordBank.categories.length) {
    state.wordBank.categories = await learningApi.wordBankCategories();
  }
  await loadWordBankWords(state.wordBank.page);
  state.wordBank.loaded = true;
}

async function loadWordBankWords(page = 0) {
  state.wordBank.page = Math.max(0, page);
  try {
    const result = await learningApi.wordBankWords({
      page: state.wordBank.page,
      size: state.wordBank.size,
      category: state.wordBank.category,
      search: state.wordBank.search
    });
    state.wordBank.words = result.words || [];
    state.wordBank.total = result.total || 0;
    state.wordBank.page = result.page || 0;
    state.wordBank.size = result.size || state.wordBank.size;
  } catch (error) {
    state.wordBank.words = [];
    state.wordBank.total = 0;
    ElMessage.error(error.message || "词库加载失败");
  }
}

async function setWordBankCategory(category) {
  state.wordBank.category = category;
  await loadWordBankWords(0);
}

function scheduleWordBankSearch() {
  clearTimeout(wordBankSearchTimer);
  wordBankSearchTimer = setTimeout(() => {
    loadWordBankWords(0).catch((error) => ElMessage.error(error.message || "词库搜索失败"));
  }, 350);
}

async function openSettings() {
  state.settingsOpen = true;
  try {
    const settings = await aiApi.settings();
    state.ai.baseUrl = settings.baseUrl || "";
    state.ai.chatModel = settings.chatModel || "";
    state.ai.apiKey = "";
    state.ai.models = settings.chatModel ? [settings.chatModel] : [];
    state.ai.status = settings.hasApiKey ? "已保存密钥" : "未保存密钥";
  } catch (error) {
    state.ai.status = error.message || "加载设置失败";
  }
}

async function fetchModels() {
  state.aiBusy = true;
  state.ai.status = "正在获取模型...";
  try {
    const models = await aiApi.models({ baseUrl: state.ai.baseUrl, apiKey: state.ai.apiKey });
    state.ai.models = normalizeModelList(models);
    state.ai.chatModel = state.ai.models.includes(state.ai.chatModel) ? state.ai.chatModel : state.ai.models[0] || state.ai.chatModel;
    state.ai.status = state.ai.models.length ? `已获取 ${state.ai.models.length} 个模型` : "没有找到可用模型";
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
      chatModel: state.ai.chatModel
    });
    state.ai.apiKey = "";
    state.ai.chatModel = settings.chatModel || state.ai.chatModel;
    state.ai.status = `已应用 ${state.ai.chatModel || "当前模型"}`;
    ElMessage.success("AI 设置已保存");
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
    loadConversations()
  ]);
  if (activeSection.value === "wordbank") {
    await loadWordBank();
  }
  state.loading = false;
}

watch(activeSection, async (section) => {
  if (section === "wordbank" && !state.wordBank.loaded) await loadWordBank();
});

onMounted(() => {
  document.title = "LinguaMate Agent";
  stopAmbientScene = initAmbientCanvas();
  boot().catch((error) => {
    console.error(error);
    state.loading = false;
    ElMessage.error(error.message || "加载失败");
  });
});

onBeforeUnmount(() => {
  stopAmbientScene();
});
</script>
