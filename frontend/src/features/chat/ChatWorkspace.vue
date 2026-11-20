<script setup>
import { useRouter } from "vue-router";
import { toRefs } from "vue";
import ChatStudio from "@/components/ChatStudio.vue";
const router = useRouter();
const props = defineProps({
  chat: Object,
  activeSection: String,
  openSettings: Function,
});
const {
  state: chatState,
  activeThread,
  canStartNew,
  visibleConversations,
  selectMode,
  handleLearningAction,
  loadLearningCardPage,
  handleConversationSelect,
  newConversation,
  sendChatPrompt,
  sendChat,
} = props.chat;
const { activeSection, openSettings } = toRefs(props);
</script>

<template>
  <section
    id="chat"
    class="section chat-workspace"
    :class="{ active: activeSection === 'chat' }"
  >
    <ChatStudio
      :active="activeSection === 'chat'"
      :messages="activeThread.messages"
      :conversations="visibleConversations"
      :conversation-id="activeThread.id"
      :can-start-new="canStartNew"
      :scene="activeThread.scene"
      :mode="chatState.chatMode"
      v-model:draft="activeThread.draft"
      :busy="chatState.sending"
      :loading-history="chatState.loadingHistory"
      :error="activeThread.error"
      :context-words="chatState.coachWords"
      :trace="activeThread.trace"
      @send="sendChat"
      @prompt="sendChatPrompt"
      @mode="selectMode"
      @scene="activeThread.scene = $event"
      @history="handleConversationSelect"
      @new="newConversation"
      @settings="openSettings"
      @review="router.push({ path: '/wordbank', query: { scope: 'review' } })"
    >
      <template #message-extra="{ message, index, showActions }">
        <section
          v-if="message.learningCard"
          class="learning-advisor-card"
          :class="`card-${message.learningCard.type}`"
        >
          <header>
            <div>
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
                message.learningCard.page >= message.learningCard.totalPages - 1
              "
              @click="loadLearningCardPage(message, index, 1)"
            >
              下一页
            </button>
          </footer>
        </section>
        <div v-if="showActions && message.actions?.length" class="learning-card-actions">
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
      <template #assistant-actions="{ actions }">
        <button
          v-for="action in actions"
          :key="action.id"
          type="button"
          @click="handleLearningAction(action)"
        >
          {{ action.label }}<span aria-hidden="true">→</span>
        </button>
      </template>
    </ChatStudio>
  </section>
</template>
