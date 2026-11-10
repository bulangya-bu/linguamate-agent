<script setup>
import AdminSketchIcon from "@/components/AdminSketchIcon.vue";
import { number, runAction } from "./admin-utils";
const props = defineProps({ controller: { type: Object, required: true } });
const { state, loadWords, fillWordForm, clearWordForm, saveWord, deleteWord, debouncedLoadWords } = props.controller;
</script>

<template>
<section
        id="words"
        class="section active"
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
                  ></textarea></label
                ><label class="admin-field span-all"
                  ><span>例句翻译</span
                  ><textarea
                    v-model="state.wordForm.exampleCn"
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
</template>
