<script setup>
import AdminSketchIcon from "@/components/AdminSketchIcon.vue";
import { number, difficultyLabel, runAction } from "./admin-utils";
const props = defineProps({ controller: { type: Object, required: true } });
const { state, loadNews, fillNewsForm, clearNewsForm, saveNews, deleteNews, debouncedLoadNews } = props.controller;
</script>

<template>
<section
        id="news"
        class="section active"
        aria-label="阅读内容"
      >
        <div class="admin-two-column">
          <section class="panel admin-list-panel">
            <header class="admin-panel-heading">
              <div>
                <h2><AdminSketchIcon name="news" />阅读文章</h2>
                <p>共 {{ number(state.newsPage.total) }} 篇文章</p>
              </div>
              <label class="admin-search-box"
                ><AdminSketchIcon name="search" /><input
                  v-model.trim="state.newsSearch"
                  class="admin-search"
                  placeholder="搜索标题、来源或标签"
                  aria-label="搜索标题、来源或标签"
                  @input="debouncedLoadNews"
              /></label>
            </header>
            <div class="admin-table">
              <div class="admin-row admin-row-head">
                <span>标题</span><span>难度</span><span>来源</span
                ><span>操作</span>
              </div>
              <div
                v-for="article in state.newsPage.articles"
                :key="article.id"
                class="admin-row"
                :class="{ 'is-selected': state.newsForm.id === article.id }"
              >
                <span
                  ><strong>{{ article.title }}</strong
                  ><small>{{ article.topicTags }}</small></span
                ><span
                  ><mark class="neutral">{{
                    difficultyLabel(article.difficulty)
                  }}</mark></span
                ><span>{{ article.sourceName }}</span
                ><span class="admin-actions"
                  ><button type="button" @click="fillNewsForm(article)">
                    编辑</button
                  ><button
                    type="button"
                    class="is-danger"
                    @click="runAction(() => deleteNews(article))"
                  >
                    删除
                  </button></span
                >
              </div>
              <div
                v-if="!state.newsPage.articles.length"
                class="admin-empty-state"
              >
                <AdminSketchIcon name="search" />
                <h3>没有找到匹配文章</h3>
              </div>
            </div>
            <footer class="admin-pagination">
              <button
                type="button"
                :disabled="state.newsPage.page <= 0"
                @click="runAction(() => loadNews(state.newsPage.page - 1))"
              >
                上一页</button
              ><span
                >第 {{ state.newsPage.page + 1 }} 页 · 共
                {{ number(state.newsPage.total) }} 篇</span
              ><button
                type="button"
                :disabled="
                  (state.newsPage.page + 1) * state.newsPage.size >=
                  state.newsPage.total
                "
                @click="runAction(() => loadNews(state.newsPage.page + 1))"
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
                    :name="state.newsForm.id ? 'writing' : 'plus'"
                  />{{ state.newsForm.id ? "编辑阅读" : "新增阅读" }}
                </h2>
              </div>
              <button
                v-if="state.newsForm.id"
                class="admin-text-link"
                type="button"
                @click="clearNewsForm"
              >
                新增
              </button>
            </header>
            <form class="admin-form" @submit.prevent="runAction(saveNews)">
              <div class="admin-form-fields">
                <label class="admin-field span-all"
                  ><span>文章标题</span
                  ><input
                    v-model="state.newsForm.title"
                    required /></label
                ><label class="admin-field"
                  ><span>来源</span
                  ><input
                    v-model="state.newsForm.sourceName" /></label
                ><label class="admin-field"
                  ><span>难度</span
                  ><select v-model="state.newsForm.difficulty">
                    <option value="" disabled>选择难度</option>
                    <option value="easy">简单</option>
                    <option value="medium">进阶</option>
                    <option value="hard">挑战</option>
                  </select></label
                ><label class="admin-field span-all"
                  ><span>主题标签</span
                  ><input
                    v-model="state.newsForm.topicTags"
                    placeholder="用逗号分隔，如 科学,科技" /></label
                ><label class="admin-field span-all"
                  ><span>摘要</span
                  ><textarea
                    v-model="state.newsForm.summary"
                  ></textarea></label
                ><label class="admin-field span-all"
                  ><span>英文正文</span
                  ><textarea
                    v-model="state.newsForm.content"
                    class="admin-tall-textarea"
                    required
                  ></textarea>
                </label>
              </div>
              <footer class="admin-form-actions">
                <button
                  class="settings-secondary"
                  type="button"
                  @click="clearNewsForm"
                >
                  清空</button
                ><button class="settings-primary" type="submit">
                  保存阅读
                </button>
              </footer>
            </form>
          </section>
        </div>
      </section>
</template>
