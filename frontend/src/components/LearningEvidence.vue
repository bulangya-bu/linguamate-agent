<template>
  <dialog ref="dialog" class="learning-evidence-dialog" aria-labelledby="evidence-title" @cancel.prevent="$emit('close')" @click="backdropClose">
    <header>
      <h2 id="evidence-title">学习变化</h2>
      <button class="panel-close" type="button" aria-label="关闭学习变化" @click="$emit('close')"><AppIcon name="close" /></button>
    </header>
    <section class="evidence-accuracy">
      <span>拼写与释义复习正确率</span>
      <div class="evidence-periods">
        <div><span>前 7 天</span><b>{{ evidence.previousAttempts ? evidence.previousAccuracy + '%' : '暂无记录' }}</b><i>{{ evidence.previousAttempts || 0 }} 次作答</i></div>
        <AppIcon name="arrow" />
        <div><span>近 7 天</span><b>{{ evidence.recentAttempts ? evidence.recentAccuracy + '%' : '暂无记录' }}</b><i>{{ evidence.recentAttempts || 0 }} 次作答</i></div>
      </div>
      <p v-if="evidence.recentAttempts && evidence.previousAttempts" class="evidence-delta">
        {{ delta > 0 ? `提高 ${delta} 个百分点` : delta < 0 ? `下降 ${Math.abs(delta)} 个百分点` : '持平（0 个百分点）' }}
      </p>
    </section>
    <div class="evidence-details">
      <section><AppIcon name="review" /><strong>{{ evidence.recurringWords || 0 }}<small>词</small></strong><h3>近 7 天答错至少两次</h3></section>
      <section><AppIcon name="writing" /><strong>{{ evidence.revisedEssays || 0 }}<small>次</small></strong><h3>改写复评</h3></section>
    </div>
    <footer><button type="button" @click="$emit('practice'); $emit('close')">继续练习<AppIcon name="arrow" /></button></footer>
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
<style scoped>
.learning-evidence-dialog h2 { margin: 0; }
.learning-evidence-dialog > footer { justify-content: flex-end; }
</style>
