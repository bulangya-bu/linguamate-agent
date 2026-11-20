import { computed, reactive } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { adminApi } from "@/api/modules";
import { number, runAction, useAdminSearch } from "./admin-utils";



export function useAdminUsers() {
  const state = reactive({
    users: [],
    userSearch: "",
    userProfile: null,
    userReadings: []
  });

  const profileMetrics = computed(() => {
    const profile = state.userProfile || {};
    return [
      { label: "学习等级", value: `Lv.${profile.level || 1}` },
      { label: "当前经验", value: number(profile.experience) },
      { label: "已背单词", value: number(profile.learnedWords) },
      { label: "收藏单词", value: number(profile.favoriteWords) },
      { label: "已掌握", value: number(profile.masteredWords) },
      { label: "错词", value: number(profile.reviewWords) },
      { label: "写作提交", value: number(profile.essays) },
      { label: "阅读记录", value: number(profile.readings) },
      { label: "会话", value: number(profile.conversations) },
    ];
  });

  async function loadUsers() {
    state.users = await adminApi.users(state.userSearch);
  }

  async function loadUserProfile(id) {
    const [profile, readings] = await Promise.all([
      adminApi.userProfile(id),
      adminApi.userReadings(id),
    ]);
    state.userProfile = profile;
    state.userReadings = readings || [];
  }

  async function editUser(user) {
    if (user.role === "ADMIN") {
      ElMessage.warning("管理员账号不在这里编辑");
      return;
    }
    const { value } = await ElMessageBox.prompt("输入新的昵称", "编辑用户", {
      inputValue: user.nickname || user.username,
      confirmButtonText: "保存",
      cancelButtonText: "取消",
    });
    await adminApi.updateUser(user.id, { nickname: value });
    ElMessage.success("用户昵称已更新");
    await loadUsers();
    if (state.userProfile?.id === user.id) await loadUserProfile(user.id);
  }

  async function toggleUserStatus(user) {
    if (user.role === "ADMIN") {
      ElMessage.warning("管理员账号不能禁用");
      return;
    }
    const status = user.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    if (status === "DISABLED") {
      await ElMessageBox.confirm(`停用 @${user.username}？该用户将无法登录。`, "停用账号", {
        confirmButtonText: "停用",
        cancelButtonText: "取消",
        type: "warning",
      });
    }
    await adminApi.updateUserStatus(user.id, status);
    ElMessage.success(status === "ACTIVE" ? "用户已启用" : "用户已禁用");
    await loadUsers();
  }

  async function resetUserPassword(user) {
    if (user.role === "ADMIN") {
      ElMessage.warning("管理员密码不在这里重置");
      return;
    }
    const { value } = await ElMessageBox.prompt(
      `为 @${user.username} 输入新密码`,
      "重置密码",
      {
        inputType: "password",
        inputPattern: /^[\s\S]{6,64}$/,
        inputErrorMessage: "密码须为 6–64 位",
        confirmButtonText: "重置",
        cancelButtonText: "取消",
      },
    );
    await adminApi.resetPassword(user.id, value);
    ElMessage.success("密码已重置");
  }

  const debouncedLoadUsers = useAdminSearch(() => runAction(() => loadUsers(0)));

  return { state, profileMetrics, loadUsers, loadUserProfile, editUser, toggleUserStatus, resetUserPassword, debouncedLoadUsers };
}
