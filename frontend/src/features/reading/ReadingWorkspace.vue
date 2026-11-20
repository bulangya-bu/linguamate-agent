<script setup>
import { BRAND_NAME } from "@/brand";
import { toRefs } from "vue";
import AppIcon from "@/components/AppIcon.vue";
import ParagraphTranslation from "./ParagraphTranslation.vue";
import {
  splitArticleTopics,
  newsTopicLabel,
  articleTopicText,
  articleReadingTime,
  difficultyLabel,
  newsDifficultyOptions,
} from "../reading/articlePresentation";
const props = defineProps({
  reading: Object,
  activeSection: String,
  chatBusy: Boolean,
});
const {
  state: readingState,
  isPhone,
  phoneArticle,
  selectArticle,
  readerRef,
  newsLibraryOpen,
  newsLibraryButtonRef,
  newsLibraryCloseRef,
  showAllNewsTopics,
  newsTopicOptions,
  visibleNewsTopics,
  readerProgress,
  canResumeReading,
  resumeReading,
  startReadingOver,
  articleParagraphs,
  readerFocus,
  toggleReaderFocus,
  dimReaderToken,
  focusReaderWord,
  articleWordCount,
  setNewsDifficulty,
  setNewsTopic,
  loadNews,
  loadArticle,
  handleReaderScroll,
  finishReading,
  translateArticle,
  normalizeLookupWord,
  isLookupWord,
  activeReaderWord,
  focusNavigationWord,
  navigateReaderWords,
  showTooltip,
  focusLookupActions,
  scheduleHideTooltip,
  discussArticle,
} = props.reading;
const { activeSection, chatBusy } = toRefs(props);
</script>

<template>
  <section
    id="news"
    class="section"
    :class="{
      active: activeSection === 'news',
      'is-browsing': isPhone ? !phoneArticle : newsLibraryOpen,
    }"
  >
    <div class="news-layout">
      <section id="news-library" class="panel news-library">
        <div class="panel-heading news-heading">
          <h2><AppIcon name="news" /> 阅读推荐</h2>
          <span class="news-library-count"
            >{{ readingState.newsList.length }} 篇</span
          >
          <button v-if="!isPhone"
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
                  'is-active': readingState.newsDifficulty === item.value,
                }"
                :aria-pressed="readingState.newsDifficulty === item.value"
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
                :class="{ 'is-active': readingState.newsTopic === topic.value }"
                :aria-pressed="readingState.newsTopic === topic.value"
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
                {{ showAllNewsTopics ? "收起" : "更多" }}
              </button>
            </div>
          </div>
        </div>
        <div class="news-list">
          <article
            tabindex="0"
            role="button"
            @keydown.enter="selectArticle(article.id)"
            @keydown.space.prevent="selectArticle(article.id)"
            v-for="article in readingState.newsList"
            :key="article.id"
            class="news-item"
            :class="{
              'is-active': readingState.currentArticle?.id === article.id,
            }"
            :aria-pressed="readingState.currentArticle?.id === article.id"
            @click="selectArticle(article.id)"
          >
            <div class="news-item-topline">
              <span>{{
                newsTopicLabel(splitArticleTopics(article.topicTags)[0]) ||
                "精选"
              }}</span>
              <span v-if="article.finished" class="news-item-finished"
                ><AppIcon name="check" />已读</span
              >
              <span v-else>{{ articleReadingTime(article) }} 分钟</span>
            </div>
            <strong>{{ article.title }}</strong>
            <p>{{ article.summary }}</p>
            <div class="news-item-bottomline">
              <span class="news-level" :data-level="article.difficulty">{{
                difficultyLabel(article.difficulty)
              }}</span>
              <span
                v-if="article.readProgress && !article.finished"
                class="news-item-progress"
                >已读 {{ article.readProgress }}%</span
              >
              <AppIcon name="arrow" />
            </div>
          </article>
          <p v-if="isPhone && readingState.newsListStatus === 'error'" class="phone-reader-status" role="status">
            选文加载失败 <button type="button" @click="loadNews()">重试</button>
          </p>
          <p v-else-if="isPhone && readingState.newsListStatus === 'loading' && !readingState.newsList.length" class="news-empty" role="status">
            正在读取选文…
          </p>
          <p v-else-if="!readingState.newsList.length" class="news-empty">
            暂无匹配文章
          </p>
        </div>
      </section>
      <section
        class="panel reader-panel"
        :class="{ 'is-reading-focus': readerFocus }"
        :aria-busy="readingState.articleBusy"
      >
        <div v-if="isPhone && !readingState.currentArticle" class="phone-reader-status" role="status">
          {{ readingState.articleError || "正在读取文章…" }}
          <button v-if="readingState.articleError" type="button" @click="loadArticle(phoneArticle)">重试</button>
        </div>
        <template v-else>
        <header class="reader-cover">
          <div class="reader-cover-meta">
            <span class="reader-source">{{
              readingState.currentArticle?.sourceName || `${BRAND_NAME}阅读`
            }}</span>
            <span v-if="readingState.currentArticle" class="reader-topic">{{
              articleTopicText(readingState.currentArticle.topicTags)
            }}</span>
          </div>
          <div class="reader-cover-title">
            <div>
              <h2 :key="readingState.currentArticle?.id">
                {{
                  readingState.currentArticle?.title || "从一个感兴趣的主题开始"
                }}
              </h2>
              <p
                v-if="readingState.currentArticle?.summary"
                class="reader-deck"
              >
                {{ readingState.currentArticle.summary }}
              </p>
              <div v-if="readingState.currentArticle" class="reader-byline">
                <span
                  class="news-level"
                  :data-level="readingState.currentArticle.difficulty"
                  >{{
                    difficultyLabel(readingState.currentArticle.difficulty)
                  }}</span
                >
                <span>{{ articleWordCount }} 词</span>
                <span
                  >约
                  {{ articleReadingTime(readingState.currentArticle) }}
                  分钟</span
                >
              </div>
            </div>
          </div>
        </header>
        <div class="reader-tools">
          <div v-if="canResumeReading" class="reader-position-actions" role="group" aria-label="阅读位置">
            <button type="button" aria-label="继续阅读" :disabled="readingState.articleBusy" @click="resumeReading">续读</button>
            <button type="button" aria-label="从头阅读" :disabled="readingState.articleBusy" @click="startReadingOver">从头</button>
          </div>
          <div class="reader-toolbar">
            <button
              class="reader-focus-action"
              type="button"
              :aria-pressed="readerFocus"
              aria-label="句子聚焦"
              :disabled="!readingState.currentArticle || readingState.articleBusy"
              @click="toggleReaderFocus"
            >聚焦</button>
            <button
              class="reader-discuss"
              type="button"
              :disabled="
                !readingState.currentArticle ||
                chatBusy ||
                readingState.articleBusy
              "
              @click="discussArticle"
            >
              <AppIcon name="chat" />讨论
            </button>
            <button
              v-if="!isPhone"
              ref="newsLibraryButtonRef"
              class="reading-library-toggle settings-secondary"
              type="button"
              aria-controls="news-library"
              :aria-expanded="newsLibraryOpen"
              @click="newsLibraryOpen = true"
            >
              换一篇
            </button>
            <button
              v-if="!isPhone"
              class="translate-btn"
              type="button"
              :disabled="
                !readingState.currentArticle ||
                readingState.translateBusy ||
                readingState.articleBusy
              "
              @click="translateArticle"
            >
              {{
                readingState.translateBusy
                  ? "翻译中…"
                  : readingState.translation.contentCn ||
                      readingState.translation.titleCn
                    ? "收起译文"
                    : "翻译"
              }}
            </button>
          </div>
        </div>
        <span id="reader-keyboard-hint" class="reader-keyboard-hint">方向键选词，回车查词。</span>
        <div
          ref="readerRef"
          class="reader-scroll-area"
          @scroll="handleReaderScroll"
        >
          <article
            :key="readingState.currentArticle?.id"
            class="reader-content"
            aria-label="英文正文，可点选单词查词"
          >
            <template v-for="(paragraph, index) in articleParagraphs" :key="index">
            <p class="reader-paragraph">
              <template v-for="token in paragraph" :key="token.key">
                <span
                  v-if="token.word"
                  class="reader-word"
                  :class="{
                    'is-looked-up':
                      readingState.tooltip.visible &&
                      (!isPhone || activeReaderWord === token.key) &&
                      readingState.tooltip.lookupKey ===
                        normalizeLookupWord(token.text),
                    'reader-art-dim': dimReaderToken(token),
                  }"
                  :role="isLookupWord(token.text) ? 'button' : undefined"
                  :tabindex="isLookupWord(token.text) ? (activeReaderWord === token.key ? 0 : -1) : undefined"
                  :aria-describedby="activeReaderWord === token.key ? 'reader-keyboard-hint' : undefined"
                  :data-reader-key="isLookupWord(token.text) ? token.key : undefined"
                  :data-sentence="token.sentence"
                  @focus="focusNavigationWord(token)"
                  @blur="scheduleHideTooltip"
                  @click="showTooltip(token.text, $event)"
                  @keydown="navigateReaderWords($event, token)"
                  @keydown.enter.prevent="focusLookupActions(token.text, $event)"
                  @keydown.space.prevent="focusLookupActions(token.text, $event)"
                  @mouseenter="focusReaderWord(token, $event)"
                  @mouseleave="scheduleHideTooltip"
                  >{{ token.text }}</span
                >
                <span
                  v-else
                  :class="{ 'reader-art-dim': dimReaderToken(token) }"
                  >{{ token.text }}</span
                >
              </template>
            </p>
            <ParagraphTranslation v-if="isPhone" :text="paragraph.map(token => token.text).join('')" />
            </template>
          </article>
          <div
            v-if="!isPhone && (
              readingState.translation.titleCn ||
              readingState.translation.contentCn)
            "
            class="translation-area"
          >
            <h3>{{ readingState.translation.titleCn }}</h3>
            <p>{{ readingState.translation.contentCn }}</p>
          </div>
        </div>
        <footer class="reader-progress">
          <span class="reader-progress-label">{{
            readingState.currentArticle?.finished ? "本篇已读完" : "阅读进度"
          }}</span>
          <div
            class="reader-progress-track"
            role="progressbar"
            aria-label="阅读进度"
            :aria-valuenow="readerProgress"
            aria-valuemin="0"
            aria-valuemax="100"
          >
            <span :style="{ width: readerProgress + '%' }"></span>
          </div>
          <span>{{ readerProgress }}%</span>
          <button
            class="reader-finish-btn"
            type="button"
            :disabled="
              !readingState.currentArticle ||
              readingState.readingBusy ||
              readingState.articleBusy ||
              readingState.currentArticle.finished
            "
            @click="finishReading"
          >
            <AppIcon name="check" />{{
              readingState.readingBusy
                ? "保存中…"
                : readingState.currentArticle?.finished
                  ? "已完成"
                  : "完成阅读"
            }}
          </button>
        </footer>
        </template>
      </section>
    </div>
  </section>
</template>
