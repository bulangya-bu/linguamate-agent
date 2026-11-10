<script setup>
import AdminSketchIcon from "@/components/AdminSketchIcon.vue";
import AdminTrendChart from "@/components/AdminTrendChart.vue";
import SketchShape from "@/components/SketchShape.vue";
import { number } from "./admin-utils";
const props = defineProps({ controller: { type: Object, required: true } });
const { state, inactiveUsers, activePercent, weekReviews, overviewMetrics, contentAssets, activityTotals } = props.controller;
const emit = defineEmits(["navigate"]);
const showSection = (id) => emit("navigate", id);
</script>

<template>
<section
        id="overview"
        class="section active"
        aria-label="管理概览"
      >
        <div class="admin-overview-workspace">
          <div class="admin-metrics-band">
            <section
              v-for="metric in overviewMetrics"
              :key="metric.label"
              :class="metric.tone"
            >
              <span class="admin-metric-label"
                ><AdminSketchIcon :name="metric.icon" />{{ metric.label }}</span
              ><strong
                >{{ number(metric.value)
                }}<small>{{ metric.unit }}</small></strong>
            </section>
          </div>
          <div class="admin-overview-body">
            <article class="admin-trend-panel panel">
              <header class="admin-panel-heading">
                <div>
                  <h2><AdminSketchIcon name="overview" />近 7 天练词趋势</h2>
                </div>
                <span class="admin-period-label"
                  >近 7 天 <b>{{ number(weekReviews) }}</b> 次</span
                >
              </header>
              <AdminTrendChart
                v-if="state.section === 'overview'"
                :points="state.summary.dailyWordStats || []"
              />
              <div class="admin-activity-totals">
                <div v-for="item in activityTotals" :key="item.label">
                  <AdminSketchIcon :name="item.icon" /><span>{{
                    item.label
                  }}</span
                  ><strong
                    >{{ number(item.value)
                    }}<small>{{ item.unit }}</small></strong
                  >
                </div>
              </div>
            </article>
            <aside class="admin-overview-rail">
              <section class="admin-status-panel panel">
                <header class="admin-panel-heading">
                  <h2><AdminSketchIcon name="users" />账号状态</h2>
                  <button
                    class="admin-text-link"
                    type="button"
                    @click="showSection('users')"
                  >
                    管理用户<AdminSketchIcon name="arrow" />
                  </button>
                </header>
                <div class="admin-status-body">
                  <div
                    class="admin-status-ring"
                    role="img"
                    :aria-label="`启用账号 ${state.summary.activeUsers || 0} 个，共 ${state.summary.users || 0} 个，占比 ${activePercent}%`"
                  >
                    <svg viewBox="0 0 180 180" aria-hidden="true">
                      <circle cx="90" cy="90" r="66" class="admin-ring-track" />
                      <circle
                        v-if="state.summary.users"
                        cx="90"
                        cy="90"
                        r="66"
                        class="admin-ring-progress"
                        pathLength="100"
                        :stroke-dasharray="`${activePercent} ${100 - activePercent}`"
                        transform="rotate(-90 90 90)"
                      />
                      <SketchShape
                        kind="circle"
                        :args="[90, 90, 149]"
                        :options="{
                          stroke: 'var(--admin-tree-line)',
                          strokeWidth: 1,
                          roughness: 0.75,
                        }"
                      />
                      <SketchShape
                        kind="circle"
                        :args="[90, 90, 115]"
                        :options="{
                          stroke: 'var(--admin-tree-line)',
                          strokeWidth: 0.8,
                          roughness: 0.65,
                        }"
                      />
                    </svg>
                    <div>
                      <strong>{{
                        state.summary.users ? activePercent + "%" : "—"
                      }}</strong
                      ><span>账号启用占比</span>
                    </div>
                  </div>
                  <dl class="admin-status-legend">
                    <div>
                      <dt><i></i>启用账号</dt>
                      <dd>{{ number(state.summary.activeUsers) }}</dd>
                    </div>
                    <div>
                      <dt><i class="is-muted"></i>停用账号</dt>
                      <dd>{{ number(inactiveUsers) }}</dd>
                    </div>
                  </dl>
                </div>
              </section>
              <section class="admin-assets-panel panel">
                <header class="admin-panel-heading">
                  <h2><AdminSketchIcon name="knowledge" />内容资产</h2>
                  <span>累计</span>
                </header>
                <div class="admin-asset-list">
                  <button
                    v-for="item in contentAssets"
                    :key="item.id"
                    type="button"
                    @click="showSection(item.id)"
                  >
                    <span class="admin-asset-icon" :class="item.id"
                      ><AdminSketchIcon :name="item.id" /></span
                    ><span
                      >{{ item.label }}</span
                    ><strong
                      >{{ number(item.value)
                      }}<small>{{ item.unit }}</small></strong
                    ><AdminSketchIcon name="arrow" />
                  </button>
                </div>
              </section>
            </aside>
          </div>
        </div>
      </section>
</template>
