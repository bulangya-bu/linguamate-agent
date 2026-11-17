<script setup>
import { computed, shallowRef } from 'vue';
import AppIcon from '../AppIcon.vue';
import { revisionDiff } from '@/writing-workspace';
import { evidenceDate, hasModelFeedback } from './evidenceRecords';

const props = defineProps({
  type: { type: String, required: true },
  record: { type: Object, required: true },
});

const turnId = shallowRef(props.record.example?.id);
const turn = computed(() => props.record.turns?.find(item => item.id === turnId.value));
const attempts = computed(() => (props.record.attempts ?? []).slice().reverse());
const writingParts = computed(() => props.type === 'writing' && props.record.previousId
  ? revisionDiff(props.record.before || '', props.record.after || '')
  : []);
const before = computed(() => writingParts.value.filter(part => part.type !== 'insert'));
const after = computed(() => writingParts.value.filter(part => part.type !== 'delete'));

function answer(value) {
  return value?.trim() || '未填写';
}
</script>

<template>
  <article class="growth-record-detail" :class="'growth-detail-' + type">
    <header class="growth-detail-hero">
      <h3>{{ record.title }}</h3>
      <p v-if="type === 'words' && record.meaning"><span class="growth-meaning-label">正确释义</span>{{ record.meaning }}</p>
      <div class="growth-detail-meta">
        <span v-if="type === 'words'" class="growth-chip" :data-tone="record.corrected ? 'success' : record.tone">
          {{ record.corrected ? '曾答错 → 复习答对' : record.status }}
        </span>
        <template v-else-if="type === 'reading'">
          <span class="growth-chip" :data-tone="record.tone">{{ record.finished ? '已读完' : '阅读中' }}</span>
          <span v-if="record.sourceName">{{ record.sourceName }}</span>
        </template>
        <span v-else class="growth-chip">{{ record.status }}</span>
        <time v-if="record.date" :datetime="record.date" :title="evidenceDate(record.date)">
          {{ evidenceDate(record.date).slice(0, 10) }}
        </time>
      </div>
    </header>

    <template v-if="type === 'words'">
      <h4 class="growth-detail-heading">作答记录</h4>
      <ol class="growth-attempts">
        <li v-for="attempt in attempts" :key="attempt.id">
          <AppIcon :name="attempt.correct ? 'check' : 'review'" :data-tone="attempt.correct ? 'success' : 'muted'" />
          <div class="growth-attempt-body">
            <div class="growth-attempt-heading">
              <strong>{{ attempt.type === 'STRICT_REVIEW' ? '拼写与释义复习' : '词义选择' }}</strong>
              <time v-if="attempt.createdAt" :datetime="attempt.createdAt">{{ evidenceDate(attempt.createdAt) }}</time>
              <span class="growth-chip" :data-tone="attempt.correct ? 'success' : 'muted'">{{ attempt.correct ? '答对' : '答错' }}</span>
            </div>
            <dl v-if="attempt.spellingAnswer != null || attempt.meaningAnswer != null || attempt.spellingCorrect != null || attempt.meaningCorrect != null" class="growth-answer-fields">
              <div>
                <dt>我的拼写</dt><dd>{{ answer(attempt.spellingAnswer) }}
                  <small v-if="attempt.spellingCorrect != null" :data-tone="attempt.spellingCorrect ? 'success' : 'muted'">{{ attempt.spellingCorrect ? '正确' : '有误' }}</small>
                </dd>
              </div>
              <div>
                <dt>我的释义</dt><dd>{{ answer(attempt.meaningAnswer) }}
                  <small v-if="attempt.meaningCorrect != null" :data-tone="attempt.meaningCorrect ? 'success' : 'muted'">{{ attempt.meaningCorrect ? '正确' : '有误' }}</small>
                </dd>
              </div>
            </dl>
            <dl v-else class="growth-answer-fields"><div><dt>我的答案</dt><dd>{{ attempt.answer || '旧记录未保留答案' }}</dd></div></dl>
          </div>
        </li>
      </ol>
    </template>

    <template v-else-if="type === 'expressions' && turn">
      <label v-if="record.turns.length > 1" class="growth-turn-picker">
        对话轮次
        <select v-model="turnId" aria-label="对话轮次">
          <option v-for="item in record.turns" :key="item.id" :value="item.id">
            {{ evidenceDate(item.createdAt) }} · {{ item.answer?.replace(/\s+/g, ' ').slice(0, 40) }}
          </option>
        </select>
      </label>
      <section :key="turn.id" class="growth-expression-detail">
        <h4>我的原句</h4>
        <p class="growth-text">{{ turn.answer }}</p>
        <template v-if="hasModelFeedback(turn)">
          <h4>AI 推荐表达</h4>
          <p class="growth-text growth-reference">{{ turn.feedback.improved }}</p>
          <template v-if="turn.feedback.explanation">
            <h4>为什么这样说</h4>
            <p class="growth-text growth-explanation">{{ turn.feedback.explanation }}</p>
          </template>
        </template>
        <details v-if="turn.reply" class="growth-expand">
          <summary>查看当时的回复</summary><p class="growth-text">{{ turn.reply }}</p>
        </details>
      </section>
    </template>

    <template v-else-if="type === 'writing'">
      <div v-if="record.comparable && record.previousScore != null && record.score != null" class="growth-score">
        <span>同一评分设置</span><strong>{{ record.previousScore }} → {{ record.score }} 分</strong>
      </div>
      <div class="growth-writing-pair" :class="{ 'has-revision': record.previousId }">
        <section v-if="record.previousId">
          <h4>上一次原稿</h4>
          <p class="growth-text"><template v-for="(part, index) in before" :key="index"><del v-if="part.type === 'delete'">{{ part.text }}</del><template v-else>{{ part.text }}</template></template></p>
        </section>
        <section>
          <h4>{{ record.previousId ? '我的修订稿' : '我的原稿' }}</h4>
          <p v-if="record.previousId" class="growth-text"><template v-for="(part, index) in after" :key="index"><ins v-if="part.type === 'insert'">{{ part.text }}</ins><template v-else>{{ part.text }}</template></template></p>
          <p v-else class="growth-text">{{ record.after }}</p>
        </section>
      </div>
      <details v-if="record.suggestion" class="growth-expand">
        <summary>查看{{ ['model', 'AI批改'].includes(record.source) ? ' AI' : '本地' }}参考稿</summary>
        <p class="growth-text">{{ record.suggestion }}</p>
      </details>
    </template>

    <template v-else-if="type === 'reading'">
      <dl class="growth-reading-stats">
        <div>
          <dt>阅读进度</dt>
          <dd><strong>{{ record.readProgress ?? 0 }}%</strong></dd>
          <progress :value="record.readProgress ?? 0" max="100" aria-label="阅读进度"></progress>
        </div>
        <div><dt>查词</dt><dd><strong>{{ record.lookedUpWords ?? 0 }}</strong> 次</dd></div>
        <div v-if="record.date"><dt>{{ record.finishedAt ? '读完日期' : '最近记录' }}</dt><dd>{{ evidenceDate(record.date).slice(0, 10) }}</dd></div>
      </dl>
      <section v-if="record.summary" class="growth-reading-summary">
        <h4>文章摘要</h4><p class="growth-text">{{ record.summary }}</p>
      </section>
    </template>
  </article>
</template>
