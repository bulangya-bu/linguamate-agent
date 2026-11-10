<script setup>
import { useRouter } from "vue-router";
import { toRefs } from "vue";
import ChatStudio from "@/components/ChatStudio.vue";
const router = useRouter();
const props = defineProps({
  chat: Object,
  activeSection: String,
  openSettings: Function,
  modelName: String,
});
const {
  state: chatState,
  handleLearningAction,
  loadLearningCardPage,
  handleConversationSelect,
  newConversation,
  sendChatPrompt,
  sendChat,
} = props.chat;
const { activeSection, openSettings, modelName } = toRefs(props);
</script>

<template>
  <section
    id="chat"
    class="section chat-workspace"
    :class="{ active: activeSection === 'chat' }"
  >
    <ChatStudio
      :messages="chatState.messages"
      :conversations="chatState.conversations"
      :conversation-id="chatState.conversationId"
      :scene="chatState.chatScene"
      :mode="chatState.chatMode"
      v-model:draft="chatState.chatInput"
      :busy="chatState.chatBusy"
      :error="chatState.chatError"
      :model-name="modelName"
      :context-words="chatState.coachWords"
      :trace="chatState.trace"
      @send="sendChat"
      @prompt="sendChatPrompt"
      @mode="chatState.chatMode = $event"
      @scene="chatState.chatScene = $event"
      @history="handleConversationSelect"
      @new="newConversation"
      @settings="openSettings"
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
</template>
