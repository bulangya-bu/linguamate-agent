<template>
  <div class="account-control">
    <button
      ref="triggerRef"
      class="account-trigger"
      :class="{ 'is-open': opened }"
      type="button"
      :aria-label="displayName + '，账户菜单'"
      aria-haspopup="menu"
      :aria-expanded="opened"
      :aria-controls="menuId"
      :disabled="busy"
      @click="toggleMenu"
      @keydown.down.prevent="openMenu('first')"
      @keydown.up.prevent="openMenu('last')"
    >
      <span class="account-avatar" aria-hidden="true">{{ displayName.slice(0, 1) }}</span>
      <span class="account-trigger-name">{{ displayName }}</span>
      <span class="account-chevron" aria-hidden="true"></span>
    </button>
    <Teleport to="body">
      <Transition name="account-popover" @before-leave="disableLeavingMenu">
        <div
          v-if="opened"
          :id="menuId"
          ref="menuRef"
          class="account-popover"
          role="menu"
          aria-label="账户操作"
          :style="{ top: position.top + 'px', left: position.left + 'px' }"
          @keydown="onMenuKey"
        >
          <div class="account-profile" role="presentation">
            <span class="account-avatar account-avatar--large" aria-hidden="true">{{ displayName.slice(0, 1) }}</span>
            <div class="account-profile-copy">
              <strong>{{ displayName }}</strong>
              <span>@{{ user.username || 'user' }}<small v-if="user.role === 'ADMIN'">管理员</small></span>
            </div>
          </div>
          <button v-if="showModelSettings" class="account-menu-item" role="menuitem" type="button" @click="selectSettings">
            <AppIcon name="settings" /><span>模型设置</span>
            <small class="account-model-status" :class="{ 'is-ready': modelStatus === '已配置' }">{{ modelStatus || '个人配置' }}</small>
          </button>
          <RouterLink v-if="portalPath" class="account-menu-item" role="menuitem" :to="portalPath" @click="closeMenu(false)">
            <AppIcon :name="user.role === 'ADMIN' && portalPath === '/admin' ? 'overview' : 'dashboard'" /><span>{{ portalLabel }}</span><AppIcon class="account-item-arrow" name="arrow" />
          </RouterLink>
          <div class="account-menu-divider" role="separator"></div>
          <button class="account-menu-item account-menu-logout" role="menuitem" type="button" :disabled="busy" @click="selectLogout">
            <AppIcon name="logout" /><span>退出登录</span>
          </button>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, useId, watch } from 'vue';
import { useRoute } from 'vue-router';
import AppIcon from '@/components/AppIcon.vue';

const props = defineProps({
  user: { type: Object, default: () => ({}) },
  busy: Boolean,
  showModelSettings: { type: Boolean, default: true },
  modelStatus: { type: String, default: '' },
  portalPath: { type: String, default: '' },
  portalLabel: { type: String, default: '' },
});
const emit = defineEmits(['open', 'settings', 'logout']);
const route = useRoute();
const opened = ref(false);
const triggerRef = ref(null);
const menuRef = ref(null);
const position = reactive({ top: 0, left: 0 });
const menuId = 'account-menu-' + useId();
const displayName = computed(() => props.user.nickname || props.user.username || '学习者');

function updatePosition() {
  if (!opened.value || !triggerRef.value) return;
  if (window.innerWidth < 981) { closeMenu(false); return; }
  const rect = triggerRef.value.getBoundingClientRect();
  position.top = rect.bottom + 10;
  position.left = Math.max(12, Math.min(rect.right - 268, window.innerWidth - 280));
}

function menuItems() {
  return [...(menuRef.value?.querySelectorAll('[role="menuitem"]:not(:disabled)') || [])];
}

async function openMenu(focus = 'first') {
  if (props.busy) return;
  if (!opened.value) {
    opened.value = true;
    updatePosition();
    emit('open');
  }
  await nextTick();
  const items = menuItems();
  (focus === 'last' ? items.at(-1) : items[0])?.focus();
}

function closeMenu(restoreFocus = false) {
  opened.value = false;
  if (restoreFocus) triggerRef.value?.focus();
}

function disableLeavingMenu(element) {
  element.inert = true;
  element.setAttribute('aria-hidden', 'true');
}

function toggleMenu() {
  if (opened.value) closeMenu(true);
  else openMenu();
}

function selectSettings() {
  closeMenu(true);
  emit('settings', triggerRef.value);
}

function selectLogout() {
  closeMenu(true);
  emit('logout');
}

function onMenuKey(event) {
  const items = menuItems();
  const index = items.indexOf(document.activeElement);
  if (['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) {
    event.preventDefault();
    const target = event.key === 'Home' ? 0 : event.key === 'End' ? items.length - 1
      : (index + (event.key === 'ArrowDown' ? 1 : -1) + items.length) % items.length;
    items[target]?.focus();
  } else if (event.key === 'Escape') {
    event.preventDefault();
    event.stopPropagation();
    closeMenu(true);
  } else if (event.key === 'Tab') {
    event.preventDefault();
    const controls = [...document.querySelectorAll('button, a[href], input, select, textarea, [tabindex="0"]')]
      .filter(el => el.tabIndex >= 0 && !el.disabled && el.getClientRects().length && !el.closest('[inert]') && !menuRef.value?.contains(el));
    const triggerIndex = controls.indexOf(triggerRef.value);
    const next = controls[(triggerIndex + (event.shiftKey ? -1 : 1) + controls.length) % controls.length];
    closeMenu(false);
    next?.focus();
  }
}

function onPointerDown(event) {
  if (!triggerRef.value?.contains(event.target) && !menuRef.value?.contains(event.target)) closeMenu(false);
}

function onFocusIn(event) {
  if (opened.value && !triggerRef.value?.contains(event.target) && !menuRef.value?.contains(event.target)) closeMenu(false);
}

watch(() => route.fullPath, () => closeMenu(false));
watch(() => props.busy, busy => { if (busy) closeMenu(false); });
onMounted(() => {
  document.addEventListener('pointerdown', onPointerDown);
  document.addEventListener('focusin', onFocusIn);
  window.addEventListener('resize', updatePosition);
});
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onPointerDown);
  document.removeEventListener('focusin', onFocusIn);
  window.removeEventListener('resize', updatePosition);
});
</script>

<style scoped>
.account-control { display: none; }
@media (min-width: 981px) {
  .account-control { display: block; flex-shrink: 0; }
  :global(.desktop-account-legacy) { display: none !important; }
}
.account-trigger {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  min-height: 40px;
  padding: 4px 9px 4px 5px;
  border: 1px solid transparent;
  border-radius: 12px;
  font-size: 13px;
}
.account-trigger:hover,
.account-trigger.is-open { border-color: #dce8ef; background: #f0f7fb; }
.account-avatar {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 30px;
  height: 30px;
  border: 1px solid #e0edcf;
  border-radius: 50%;
  background: #edf8e2;
  color: #50852e;
  font-size: 13px;
  font-weight: 600;
}
.account-trigger-name {
  max-width: 120px;
  overflow: hidden;
  color: var(--ink);
  font-weight: 550;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.account-chevron {
  width: 6px;
  height: 6px;
  margin-left: 3px;
  border-right: 1.5px solid #8ba0b0;
  border-bottom: 1.5px solid #8ba0b0;
  transform: rotate(45deg) translateY(-2px);
  transition: transform 200ms var(--ease);
}
.is-open .account-chevron { transform: rotate(225deg) translate(-1px, -1px); }
.account-popover {
  position: fixed;
  z-index: 180;
  width: 268px;
  max-height: calc(100dvh - 80px);
  padding: 8px;
  overflow-y: auto;
  border: 1px solid #dce7ee;
  border-radius: 16px;
  background: #fffffff7;
  box-shadow: 0 16px 48px #2d52641a, 0 3px 12px #2d526409;
  backdrop-filter: blur(16px);
  transform-origin: top right;
}
.account-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 7px;
  padding: 14px 10px 17px;
  border-bottom: 1px solid #e9eff3;
}
.account-avatar--large { width: 38px; height: 38px; font-size: 16px; }
.account-profile-copy { flex: 1; min-width: 0; }
.account-profile-copy > strong { display: block; font-size: 14px; font-weight: 600; overflow-wrap: anywhere; }
.account-profile-copy > span { display: flex; align-items: center; flex-wrap: wrap; gap: 9px; margin-top: 5px; color: var(--muted); font-size: 11px; overflow-wrap: anywhere; }
.account-profile-copy small { padding: 2px 5px; border-radius: 4px; color: var(--blue-ink); background: #eaf4fb; font-size: 10px; }
.account-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 42px;
  padding: 10px 12px;
  border-radius: 9px;
  color: #40576a;
  text-align: left;
  font-size: 13px;
}
.account-menu-item > svg { width: 17px; height: 17px; color: #7d96a8; }
.account-menu-item > span { flex: 1; }
.account-menu-item:hover,
.account-menu-item:focus-visible { background: #edf6fc; color: #236d95; }
.account-model-status { padding: 3px 6px; border-radius: 5px; color: #8092a0; background: #f0f4f7; font-size: 10px; }
.account-model-status.is-ready { color: #548039; background: #edf7e4; }
.account-menu-item > .account-item-arrow { width: 14px; height: 14px; }
.account-menu-divider { height: 1px; margin: 7px 10px; background: #e9eff3; }
.account-menu-logout { color: #a86b60; }
.account-menu-logout > svg { color: #ba8c83; }
.account-menu-logout:hover,
.account-menu-logout:focus-visible { background: #fff2ee; color: #ab5546; }
.account-popover-enter-active,
.account-popover-leave-active { transition: opacity 160ms ease, transform 220ms var(--ease); }
.account-popover-leave-active { pointer-events: none; }
.account-popover-enter-from,
.account-popover-leave-to { opacity: 0; transform: translateY(-5px) scale(.98); }
@media (prefers-reduced-motion: reduce) {
  .account-chevron,
  .account-popover-enter-active,
  .account-popover-leave-active { transition: none; }
}
</style>
