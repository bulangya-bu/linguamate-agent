<script setup>
import AdminSketchIcon from "@/components/AdminSketchIcon.vue";
import { runAction } from "./admin-utils";
const props = defineProps({ controller: { type: Object, required: true } });
const { state, fetchModels, saveAiSettings } = props.controller;
</script>

<template>
<section
        id="ai"
        class="section active"
        aria-label="模型连接"
      >
        <div class="admin-ai-workspace">
          <section class="panel admin-ai-panel">
            <header class="admin-panel-heading">
              <div>
                <h2><AdminSketchIcon name="ai" />连接 AI 模型</h2>
              </div>
              <span class="settings-status" role="status">{{
                state.ai.status
              }}</span>
            </header>
            <div class="settings-form">
              <label class="settings-field"
                ><span>模型地址（兼容 OpenAI）</span
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
            <h2>配置账号</h2>

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
</template>
