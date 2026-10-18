<template>
  <dialog ref="dialog" class="learning-evidence-dialog" aria-labelledby="evidence-title" @cancel.prevent="$emit('close')" @click="backdropClose">
    <header><div><span>YOUR LEARNING, IN VIEW</span><h2 id="evidence-title">看见学习的变化</h2></div><button type="button" aria-label="关闭学习变化" @click="$emit('close')"><AppIcon name="close" /></button></header>
    <section class="evidence-accuracy"><span>近 7 天 · 复习正确率</span><strong>{{ evidence.recentAccuracy === null || evidence.recentAccuracy === undefined ? '—' : evidence.recentAccuracy + '%' }}</strong><p>根据 {{ evidence.recentAttempts || 0 }} 次拼写＋释义复习计算，包含重试。</p><div class="evidence-periods"><div><span>前 7 天</span><b>{{ evidence.previousAttempts ? evidence.previousAccuracy + '%' : '暂无记录' }}</b><i>{{ evidence.previousAttempts || 0 }} 次作答</i></div><AppIcon name="arrow" /><div><span>近 7 天</span><b>{{ evidence.recentAttempts ? evidence.recentAccuracy + '%' : '暂无记录' }}</b><i>{{ evidence.recentAttempts || 0 }} 次作答</i></div></div><p v-if="evidence.recentAttempts && evidence.previousAttempts" class="evidence-delta">{{ delta > 0 ? `提高 ${delta}` : delta < 0 ? `下降 ${Math.abs(delta)}` : '持平，变化 0' }} 个百分点<span v-if="evidence.recentAttempts < 10 || evidence.previousAttempts < 10"> · 样本较少，继续积累</span></p><p v-else class="evidence-delta">积累两个周期的记录后，再观察变化。</p></section>
    <div class="evidence-details"><section><AppIcon name="review" /><strong>{{ evidence.recurringWords || 0 }}<small>词</small></strong><h3>近 7 天反复出错</h3><p>同一单词至少答错两次</p></section><section><AppIcon name="writing" /><strong>{{ evidence.revisedEssays || 0 }}<small>次</small></strong><h3>完成改写复评</h3><p>关联到原稿的再次提交</p></section></div>
    <footer><span>仅使用你的实际学习记录</span><button type="button" @click="$emit('practice'); $emit('close')">继续练习<AppIcon name="arrow" /></button></footer>
  </dialog>
</template>
<script setup>
import {computed,ref,watch,nextTick} from 'vue';
import AppIcon from './AppIcon.vue';
const props=defineProps({open:Boolean,evidence:{type:Object,default:()=>({})}});
const emit=defineEmits(['close','practice']);
const dialog=ref(null);
const delta=computed(()=>props.evidence.recentAccuracy-props.evidence.previousAccuracy);
watch(()=>props.open,async open=>{await nextTick(); if(open && !dialog.value.open)dialog.value.showModal(); else if(!open && dialog.value.open)dialog.value.close();});
function backdropClose(event) {if(event.target!==dialog.value)return; const r=dialog.value.getBoundingClientRect(); if(event.clientX<r.left || event.clientX>r.right || event.clientY<r.top || event.clientY>r.bottom)emit('close');}
</script>
