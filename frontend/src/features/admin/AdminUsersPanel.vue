<script setup>
import AdminSketchIcon from "@/components/AdminSketchIcon.vue";
import { difficultyLabel, formatDate, runAction } from "./admin-utils";
const props = defineProps({ controller: { type: Object, required: true } });
const { state, profileMetrics, loadUserProfile, editUser, toggleUserStatus, resetUserPassword, debouncedLoadUsers } = props.controller;
</script>

<template>
<section
        id="users"
        class="section active"
        aria-label="用户管理"
      >
        <div class="admin-two-column admin-users-layout">
          <section class="panel admin-list-panel">
            <header class="admin-panel-heading">
              <div>
                <h2><AdminSketchIcon name="users" />账号列表</h2>
                <p>
                  {{
                    state.users.length >= 100
                      ? "显示最近 100 个匹配账号"
                      : `共 ${state.users.length} 个匹配账号`
                  }}
                </p>
              </div>
              <label class="admin-search-box"
                ><AdminSketchIcon name="search" /><input
                  v-model.trim="state.userSearch"
                  class="admin-search"
                  placeholder="搜索用户名或昵称"
                  aria-label="搜索用户名或昵称"
                  @input="debouncedLoadUsers"
              /></label>
            </header>
            <div class="admin-table" role="table" aria-label="用户列表">
              <div class="admin-row admin-row-head" role="row">
                <span role="columnheader">用户</span
                ><span role="columnheader">角色</span
                ><span role="columnheader">状态</span
                ><span role="columnheader">操作</span>
              </div>
              <div
                v-for="user in state.users"
                :key="user.id"
                class="admin-row"
                :class="{ 'is-selected': state.userProfile?.id === user.id }"
                role="row"
              >
                <span class="admin-user-cell" role="cell"
                  ><span class="admin-user-avatar" aria-hidden="true">{{
                    (user.nickname || user.username).slice(0, 1)
                  }}</span
                  ><span
                    ><strong>{{ user.nickname || user.username }}</strong
                    ><small>@{{ user.username }}</small></span
                  ></span
                ><span role="cell">{{
                  user.role === "ADMIN" ? "管理员" : "学习者"
                }}</span
                ><span role="cell"
                  ><mark :class="user.status === 'ACTIVE' ? 'ok' : 'danger'">{{
                    user.status === "ACTIVE" ? "已启用" : "已停用"
                  }}</mark></span
                ><span class="admin-actions" role="cell"
                  ><button
                    type="button"
                    @click="runAction(() => loadUserProfile(user.id))"
                  >
                    详情</button
                  ><button
                    type="button"
                    :disabled="user.role === 'ADMIN'"
                    @click="runAction(() => editUser(user))"
                  >
                    编辑</button
                  ><button
                    type="button"
                    :disabled="user.role === 'ADMIN'"
                    @click="runAction(() => toggleUserStatus(user))"
                  >
                    {{ user.status === "ACTIVE" ? "停用" : "启用" }}</button
                  ><button
                    type="button"
                    :disabled="user.role === 'ADMIN'"
                    @click="runAction(() => resetUserPassword(user))"
                  >
                    重置密码
                  </button></span
                >
              </div>
              <div v-if="!state.users.length" class="admin-empty-state">
                <AdminSketchIcon name="search" />
                <h3>没有找到匹配用户</h3>
              </div>
            </div>
          </section>
          <section class="panel admin-profile-panel">
            <header class="admin-panel-heading">
              <h2><AdminSketchIcon name="overview" />学习数据</h2>
            </header>
            <div v-if="state.userProfile" class="admin-profile">
              <div class="admin-profile-head">
                <div>
                  <strong>{{
                    state.userProfile.nickname || state.userProfile.username
                  }}</strong
                  ><small
                    >@{{ state.userProfile.username }} ·
                    {{
                      state.userProfile.role === "ADMIN" ? "管理员" : "学习者"
                    }}</small
                  >
                </div>
                <mark
                  :class="state.userProfile.aiConfigured ? 'ok' : 'neutral'"
                  >{{
                    state.userProfile.aiConfigured ? "已配置 AI" : "未配置 AI"
                  }}</mark
                >
              </div>
              <div class="admin-profile-grid">
                <div v-for="item in profileMetrics" :key="item.label">
                  <strong>{{ item.value }}</strong
                  ><span>{{ item.label }}</span>
                </div>
              </div>
              <section class="admin-readings">
                <header class="admin-panel-heading">
                  <h2>最近阅读</h2>
                  <span>{{ state.userReadings.length }} 条记录</span>
                </header>
                <div class="admin-table compact">
                  <div class="admin-row reading-row admin-row-head">
                    <span>文章</span><span>进度</span><span>查词</span
                    ><span>时间</span>
                  </div>
                  <div
                    v-for="record in state.userReadings"
                    :key="record.id"
                    class="admin-row reading-row"
                  >
                    <span
                      ><strong>{{ record.title }}</strong
                      ><small>{{
                        difficultyLabel(record.difficulty)
                      }}</small></span
                    ><span
                      ><mark :class="record.finished ? 'ok' : 'neutral'"
                        >{{ record.readProgress || 0 }}%</mark
                      ></span
                    ><span>{{ record.lookedUpWords || 0 }}</span
                    ><span>{{
                      formatDate(record.updatedAt || record.createdAt)
                    }}</span>
                  </div>
                  <p
                    v-if="!state.userReadings.length"
                    class="admin-inline-empty"
                  >
                    还没有阅读记录
                  </p>
                </div>
              </section>
            </div>
            <div v-else class="admin-empty-state">
              <AdminSketchIcon name="users" />
              <h3>请选择用户</h3>
            </div>
          </section>
        </div>
      </section>
</template>
