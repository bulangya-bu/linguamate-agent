<script setup>
import AdminMobileDetails from "./AdminMobileDetails.vue";
import { useAdminRecordPane } from "./useAdminRecordPane";
import AdminSketchIcon from "@/components/AdminSketchIcon.vue";
import { formatDate, runAction } from "./admin-utils";
const props = defineProps({ controller: { type: Object, required: true } });
const { state, uploadKnowledge } = props.controller;
const { pane, selected, open, finish, back } = useAdminRecordPane(() => state.knowledge);
async function submit() {
  await uploadKnowledge();
  if (pane.value === "edit") await finish();
}
</script>

<template>
<section
        id="knowledge"
        class="section active"
        aria-label="知识库管理"
      >
        <div class="admin-two-column" :data-pane="pane">
          <section class="panel admin-list-panel">
            <header class="admin-panel-heading">
              <div>
                <h2><AdminSketchIcon name="knowledge" />知识文档</h2>
                <p>共 {{ state.knowledge.length }} 篇</p>
              </div>
              <button class="admin-mobile-action" type="button" @click="open('edit')">导入</button>
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
                <button class="admin-row-open" type="button" :aria-label="'查看' + doc.title" @click="open('detail', doc)"><AdminSketchIcon name="arrow" /></button>
              </div>
              <div v-if="!state.knowledge.length" class="admin-empty-state">
                <AdminSketchIcon name="knowledge" />
                <h3>还没有知识文档</h3>
              </div>
            </div>
          </section>
          <AdminMobileDetails v-if="pane === 'detail' && selected" :title="selected.title"
            :fields="[['类型', selected.sourceType], ['标签', selected.tags], ['导入时间', formatDate(selected.createdAt)], ['内容', selected.content]]" @back="back">
          </AdminMobileDetails>
          <section class="panel admin-editor-panel">
            <button class="admin-mobile-back" type="button" @click="back"><AdminSketchIcon name="arrow" />返回</button>
            <header class="admin-panel-heading">
              <div>
                <h2><AdminSketchIcon name="plus" />导入知识</h2>
              </div>
            </header>
            <form
              class="admin-form"
              @submit.prevent="runAction(submit)"
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
                    required
                  ></textarea>
                </label>
              </div>
              <footer class="admin-form-actions">
                <button class="settings-primary" type="submit">
                  导入
                </button>
              </footer>
            </form>
          </section>
        </div>
      </section>
</template>
