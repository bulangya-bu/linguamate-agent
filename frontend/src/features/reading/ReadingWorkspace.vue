<script setup>
import { BRAND_NAME } from "@/brand";
import { toRefs } from "vue";
import AppIcon from "@/components/AppIcon.vue";
import ReadingArt from "@/components/ReadingArt.vue";
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
  readerRef,
  newsLibraryOpen,
  newsLibraryButtonRef,
  newsLibraryCloseRef,
  showAllNewsTopics,
  newsTopicOptions,
  visibleNewsTopics,
  readerProgress,
  articleParagraphs,
  readerFocus,
  toggleReaderFocus,
  dimReaderToken,
  focusReaderWord,
  articleWordCount,
  setNewsDifficulty,
  setNewsTopic,
  loadArticle,
  handleReaderScroll,
  finishReading,
  translateArticle,
  normalizeLookupWord,
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
      'is-browsing': newsLibraryOpen,
    }"
  >
    <div class="news-layout">
      <section id="news-library" class="panel news-library">
        <div class="panel-heading news-heading">
          <h2><AppIcon name="news" /> 阅读推荐</h2>
          <span class="news-library-count"
            >{{ readingState.newsList.length }} 篇</span
          >
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
            v-for="article in readingState.newsList"
            :key="article.id"
            class="news-item"
            :class="{
              'is-active': readingState.currentArticle?.id === article.id,
            }"
            :aria-pressed="readingState.currentArticle?.id === article.id"
            @click="loadArticle(article.id)"
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
          <p v-if="!readingState.newsList.length" class="news-empty">
            暂无匹配文章
          </p>
        </div>
      </section>
      <section
        class="panel reader-panel"
        :class="{ 'is-reading-focus': readerFocus }"
        :aria-busy="readingState.articleBusy"
      >
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
            <div class="reader-cover-art">
              <ReadingArt
                :active="readerFocus"
                :disabled="
                  !readingState.currentArticle || readingState.articleBusy
                "
                @toggle="toggleReaderFocus"
              />
            </div>
          </div>
        </header>
        <div class="reader-tools">
          <div class="reader-toolbar">
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
              <AppIcon name="chat" />和 AI 讨论
            </button>
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
              :disabled="
                !readingState.currentArticle ||
                readingState.translateBusy ||
                readingState.articleBusy
              "
              @click="translateArticle"
            >
              <span class="reader-translate-symbol" aria-hidden="true">译</span>
              {{
                readingState.translateBusy
                  ? "翻译中…"
                  : readingState.translation.contentCn ||
                      readingState.translation.titleCn
                    ? "收起译文"
                    : "全文翻译"
              }}
            </button>
          </div>
        </div>
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
            <p
              v-for="(paragraph, index) in articleParagraphs"
              :key="index"
              class="reader-paragraph"
            >
              <template v-for="token in paragraph" :key="token.key">
                <span
                  v-if="token.word"
                  class="reader-word"
                  :class="{
                    'is-looked-up':
                      readingState.tooltip.visible &&
                      readingState.tooltip.lookupKey ===
                        normalizeLookupWord(token.text),
                    'reader-art-dim': dimReaderToken(token),
                  }"
                  tabindex="0"
                  :data-sentence="token.sentence"
                  @focus="focusReaderWord(token, $event)"
                  @blur="scheduleHideTooltip"
                  @click="showTooltip(token.text, $event)"
                  @keydown.enter.prevent="
                    focusLookupActions(token.text, $event)
                  "
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
          </article>
          <div
            v-if="readingState.currentArticle"
            class="reader-endmark"
            aria-hidden="true"
          >
            <span></span>✦<span></span>
          </div>
          <div
            v-if="
              readingState.translation.titleCn ||
              readingState.translation.contentCn
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
      </section>
    </div>
  </section>
</template>
