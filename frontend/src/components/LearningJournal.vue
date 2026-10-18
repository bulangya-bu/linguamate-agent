<template>
  <div class="learning-journal" :aria-busy="loading">
    <div v-if="error" class="journal-load-state" role="alert"><AppIcon name="journal" /><h2>档案暂时没有打开</h2><p>{{ error }}</p><button type="button" @click="load">重新加载</button></div>
    <div v-else-if="!data" class="journal-load-state" role="status"><AppIcon name="journal" /><p>正在翻开你的学习手账…</p></div>
    <template v-else>
      <header class="journal-heading"><div><span class="journal-handwriting">My learning journal</span><p>{{ data.nickname || '学习者' }} · 加入的第 {{ data.membershipDays }} 天</p></div><div class="journal-heading-actions"><span class="journal-date-stamp"><AppIcon name="calendar" />{{ fullDate(data.today) }}</span><button type="button" aria-label="刷新学习档案" :disabled="loading" @click="load"><AppIcon name="review" /></button><button type="button" class="journal-about" @click="openNotes">统计说明</button></div></header>
      <div class="journal-metrics">
        <section><span>今日新学</span><strong>{{ today.newWords || 0 }}<small>词</small></strong><span class="journal-metric-note">首次作答的单词</span><svg viewBox="0 0 130 9" aria-hidden="true"><SketchShape kind="path" :args="['M3 5 Q60 1 125 4']" :options="{stroke:'var(--j-green)',strokeWidth:2.4}" /></svg></section>
        <section><span>今日复习</span><strong>{{ today.reviewWords || 0 }}<small>词</small></strong><span class="journal-metric-note">同一单词按天去重</span></section>
        <section><span>累计学过</span><strong>{{ data.totals.studiedWords }}<small>词</small></strong><span class="journal-metric-note">其中 {{ data.totals.masteredWords }} 词已掌握</span></section>
        <section><span>累计登录</span><strong>{{ data.loginDays }}<small>天</small></strong><span class="journal-metric-note">同一天进入只记一次</span><span class="journal-little-star" aria-hidden="true">✳</span></section>
      </div>
      <div class="journal-body">
        <div class="journal-observations">
          <section class="journal-trend" aria-labelledby="journal-trend-heading">
            <header class="journal-section-heading"><div><h2 id="journal-trend-heading">词汇的脚印<span class="journal-handwriting">little by little</span></h2><p>新学与复习，分别留下记录。</p></div><div class="journal-periods" role="group" aria-label="趋势周期"><button v-for="days in [7,30]" :key="days" type="button" :class="{active:period===days}" :aria-pressed="period===days" @click="period=days">近 {{ days }} 天</button></div></header>
            <div class="journal-chart-meta"><div class="journal-legend"><span><i></i>新学</span><span><i></i>复习</span></div><p aria-live="polite"><time>{{ shortDate(chartDay.date) }}</time><span>新学 <b>{{ chartDay.newWords }}</b></span><span>复习 <b>{{ chartDay.reviewWords }}</b></span></p></div>
            <div ref="chartWrap" class="journal-chart-wrap">
              <svg class="journal-trend-svg" :viewBox="`0 0 ${plotSize.width} ${plotSize.height}`" role="group" aria-label="每天新学和复习单词数量，聚焦日期查看数据">
                <g v-for="tick in scale.ticks" :key="tick"><line x1="37" :x2="plotSize.width-12" :y1="chartY(tick)" :y2="chartY(tick)" class="journal-grid-line" /><text x="27" :y="chartY(tick)+4" text-anchor="end" class="journal-axis-label">{{ tick }}</text></g>
                <g v-for="(day,index) in trend" :key="day.date" class="journal-chart-day" :class="{selected:chartDay.date===day.date}">
                  <rect :x="columnX(index)" y="10" :width="columnWidth-2" :height="plotSize.height-34" class="journal-selected-column" rx="4" />
                  <g class="journal-chart-ink"><SketchShape v-if="day.newWords" kind="rectangle" :args="[columnX(index)+columnWidth*.2,chartY(day.newWords),barWidth,chartHeight(day.newWords)]" :seed="dateSeed(day.date)" :options="{stroke:'var(--j-green-ink)',fill:'var(--j-green)',fillStyle:'hachure',hachureGap:4,fillWeight:1.6,roughness:.5}" /><SketchShape v-if="day.reviewWords" kind="rectangle" :args="[columnX(index)+columnWidth*.55,chartY(day.reviewWords),barWidth,chartHeight(day.reviewWords)]" :seed="dateSeed(day.date)+1" :options="{stroke:'var(--j-blue-ink)',fill:'var(--j-blue)',fillStyle:'hachure',hachureAngle:-35,hachureGap:4,fillWeight:1.4,roughness:.5}" /></g>
                  <text v-if="period===7 || index%5===0 || index===trend.length-1" :x="columnX(index)+columnWidth*.5" :y="plotSize.height-6" text-anchor="middle" class="journal-axis-label">{{ shortDate(day.date) }}</text>
                  <rect :ref="element=>dayRefs[index]=element" :x="columnX(index)" y="8" :width="columnWidth-2" :height="plotSize.height-10" fill="transparent" tabindex="0" role="button" :aria-label="`${fullDate(day.date)}，新学 ${day.newWords} 词，复习 ${day.reviewWords} 词`" @mouseenter="selectedDate=day.date" @focus="selectedDate=day.date" @click="selectedDate=day.date" @keydown.enter.prevent="selectedDate=day.date" @keydown.right.prevent="focusDay(index+1)" @keydown.left.prevent="focusDay(index-1)" />
                </g>
              </svg>
              <p v-if="!trend.some(day=>day.newWords || day.reviewWords)" class="journal-chart-empty">这几天还没有练词记录，下一次作答就会留下第一笔。</p>
            </div>
            <div class="journal-trend-footer"><span>这 {{ period }} 天，新学 <b>{{ periodNew }}</b> 词</span><button type="button" @click="$emit('navigate','vocabulary')">再学几个<AppIcon name="arrow" /></button></div>
          </section>
          <section class="journal-calendar" aria-labelledby="journal-calendar-heading"><header class="journal-section-heading"><div><h2 id="journal-calendar-heading">日子有迹可循</h2><p>近 12 周 · {{ shortDate(data.days[0].date) }} — {{ shortDate(data.today) }}</p></div><div class="journal-calendar-modes" role="group" aria-label="日历记录类型"><button type="button" :class="{active:calendarMode==='study'}" :aria-pressed="calendarMode==='study'" @click="calendarMode='study'">学习</button><button type="button" :class="{active:calendarMode==='login'}" :aria-pressed="calendarMode==='login'" @click="calendarMode='login'">登录</button></div></header>
            <div class="journal-calendar-content"><div class="journal-calendar-map"><div class="journal-calendar-layout"><div class="journal-weekdays" aria-hidden="true"><span v-for="(label,index) in ['一','','三','','五','','日']" :key="index">{{ label }}</span></div><div class="journal-day-grid" aria-label="活跃日历"><button v-for="(day,index) in data.days" :key="day.date" :ref="element=>calendarRefs[index]=element" type="button" :tabindex="selectedDate===day.date ? 0 : -1" :class="{selected:selectedDate===day.date,future:day.date>data.today,unrecorded:calendarMode==='login' && (!data.loginRecordedSince || day.date<data.loginRecordedSince)}" :style="{gridColumn:Math.floor(index/7)+1,gridRow:index%7+1}" :data-intensity="dayIntensity(day,calendarMode)" :disabled="day.date>data.today" :aria-label="calendarLabel(day)" :title="calendarLabel(day)" @mouseenter="selectedDate=day.date" @focus="selectedDate=day.date" @click="selectedDate=day.date" @keydown.left.prevent="focusCalendar(index-7)" @keydown.right.prevent="focusCalendar(index+7)" @keydown.up.prevent="focusCalendar(index-1)" @keydown.down.prevent="focusCalendar(index+1)"></button></div></div><div class="journal-calendar-key" aria-hidden="true"><span>少</span><i v-for="level in [0,1,2,3,4]" :key="level" :data-intensity="level"></i><span>多</span></div></div>
            <div class="journal-selected-note" aria-live="polite"><time class="journal-handwriting">{{ fullDate(selectedDay.date) }}</time><template v-if="calendarMode==='study'"><div class="journal-selected-numbers"><span><b>{{ selectedDay.newWords }}</b> 词新学</span><span><b>{{ selectedDay.reviewWords }}</b> 词复习</span></div><p>{{ selectedDay.studyEvents }} 条学习记录</p></template><p v-else class="journal-login-detail">{{ selectedDay.loggedIn ? '当天已登录' : (!data.loginRecordedSince || selectedDay.date<data.loginRecordedSince) ? '当天尚未开始记录登录' : '未记录登录' }}</p></div></div>
            <p class="journal-calendar-caption">{{ calendarMode==='login' ? `登录从 ${data.loginRecordedSince ? fullDate(data.loginRecordedSince) : '首次访问'} 起记录` : '每格一天 · 按已记录的学习活动着色' }}</p>
          </section>
        </div>
        <aside class="journal-growth" aria-labelledby="journal-growth-heading"><header class="journal-section-heading"><div><h2 id="journal-growth-heading">我的学习成长树</h2><p>点选分枝，继续这一项练习。</p></div><span class="journal-handwriting">keep growing</span></header>
          <svg class="journal-tree" viewBox="0 0 360 337" role="group" aria-label="累计学习记录，选择分枝进入对应功能">
            <g class="journal-tree-trunk"><SketchShape v-for="(path,index) in branches" :key="path" kind="path" :args="[path]" :seed="90+index" :options="{stroke:'var(--j-tree-ink)',strokeWidth:2.1,roughness:.8}" /></g>
            <SketchShape v-for="(leaf,index) in leaves" :key="index" kind="path" :args="[leaf]" :seed="140+index" :options="{stroke:'var(--j-green-ink)',fill:'var(--j-green)',fillStyle:'hachure',hachureGap:4,fillWeight:1.2,roughness:.7}" />
            <g v-for="node in treeNodes" :key="node.id" class="journal-tree-node" :class="{'has-records':node.count>0}" tabindex="0" role="button" :aria-label="`${node.label} ${node.count}${node.unit}，前往${node.destination}`" @click="$emit('navigate',node.id)" @keydown.enter.prevent="$emit('navigate',node.id)" @keydown.space.prevent="$emit('navigate',node.id)">
              <rect :x="node.x-51" :y="node.y-31" width="102" height="65" rx="16" class="journal-tree-node-bg" /><SketchShape kind="ellipse" :args="[node.x,node.y-2,104,65]" :seed="node.x+node.y" :options="{stroke:'var(--j-tree-node)',strokeWidth:1,roughness:.7}" /><text :x="node.x" :y="node.y-1" text-anchor="middle" class="journal-tree-count">{{ node.count }}<tspan dx="4" class="journal-tree-unit">{{ node.unit }}</tspan></text><text :x="node.x" :y="node.y+20" text-anchor="middle" class="journal-tree-label">{{ node.label }}</text>
            </g>
            <SketchShape kind="path" :args="['M150 298 Q166 292 180 302 Q194 292 210 298 L209 322 Q194 317 180 326 Q165 317 151 322 Z M180 302 L180 325']" :seed="22" :options="{stroke:'var(--j-tree-ink)',strokeWidth:1.5,fill:'var(--j-paper)',fillStyle:'solid'}" />
          </svg>
          <div class="journal-streak"><span class="journal-sun" aria-hidden="true"><svg viewBox="0 0 90 90"><SketchShape kind="circle" :args="[45,45,48]" :options="{stroke:'var(--j-green-ink)',strokeWidth:1.4,fill:'var(--j-green)',hachureGap:5,fillWeight:.6}" /><SketchShape v-for="(line,index) in sunRays" :key="index" kind="line" :args="line" :seed="index+36" :options="{stroke:'var(--j-green-ink)',strokeWidth:1.5}" /></svg></span><div><strong>{{ data.currentStreak }}<small>天</small></strong><span>连续登录</span></div><div><strong>{{ data.longestStreak }}<small>天</small></strong><span>最长连续</span></div></div>
          <div class="journal-footnotes"><span><b>{{ data.activeDays }}</b> 天留下学习记录</span><span>累计复习 <b>{{ data.totals.reviewWords }}</b> 词</span><span>复习正确率 <b>{{ data.totals.reviewAccuracy===null ? '—' : data.totals.reviewAccuracy+'%' }}</b><small>（{{ data.totals.reviewAttempts }} 次作答）</small></span></div>
          <footer class="journal-signature"><span class="journal-handwriting">a little, every day.</span><span>从 {{ fullDate(data.joinedOn) }} 开始</span></footer>
        </aside>
      </div>
      <dialog ref="notes" class="journal-notes" aria-labelledby="journal-notes-heading"><header><h2 id="journal-notes-heading">这些数字怎样记录</h2><button type="button" aria-label="关闭统计说明" @click="notes.close()"><AppIcon name="close" /></button></header><dl><dt>新学与累计学过</dt><dd>以单词练习、拼写＋释义复习中的实际作答记录为准。首次作答当天记作新学；累计单词去重，重复作答不会增加词数。</dd><dt>复习与掌握</dt><dd>复习词数按每天去重；正确率按每次复习作答计算，包含重试。已掌握沿用词库中的熟悉度与错词规则。</dd><dt>登录与连续天数</dt><dd>登录后进入学习空间记为一个登录日，保持登录的跨日访问也会记录。同一天多次进入只计一天。历史登录无法补推，从首次启用记录开始计算。</dd><dt>日历与时间</dt><dd>统一使用北京时间。学习格子来自练词、写作提交、发送对话和首次打开文章的已有记录；只登录不会增加学习活动。树中的阅读数为已完成篇数，写作数包含改写提交。</dd></dl><p>数据仅属于当前账号 · {{ data.timeZone }}</p></dialog>
    </template>
  </div>
</template>

<script setup>
import {computed,nextTick,onBeforeUnmount,onMounted,ref,watch} from 'vue';
import '@fontsource/caveat/latin-400.css';
import AppIcon from './AppIcon.vue';
import SketchShape from './SketchShape.vue';
import {learningApi} from '@/api/modules';
import {chartScale,dayIntensity,shortDate,trendDays} from '@/learning-journal';
import '@/styles/learning-journal.css';
defineEmits(['navigate']);
const data=ref(null),loading=ref(false),error=ref(''),period=ref(7),selectedDate=ref(''),calendarMode=ref('study'),notes=ref(null),dayRefs=ref([]);
const calendarRefs=ref([]);
const chartWrap=ref(null),plotSize=ref({width:720,height:230});
let observer;
const trend=computed(()=>trendDays(data.value?.days,data.value?.today,period.value));
const today=computed(()=>data.value?.days.find(day=>day.date===data.value.today) || {});
const selectedDay=computed(()=>data.value?.days.find(day=>day.date===selectedDate.value) || today.value);
const chartDay=computed(()=>trend.value.find(day=>day.date===selectedDate.value) || today.value);
const periodNew=computed(()=>trend.value.reduce((sum,day)=>sum+day.newWords,0));
const scale=computed(()=>chartScale(trend.value));
const columnWidth=computed(()=>(plotSize.value.width-54)/Math.max(1,trend.value.length));
const barWidth=computed(()=>Math.max(3,Math.min(21,columnWidth.value*.25)));
function chartHeight(value){return value/scale.value.maximum*Math.max(40,plotSize.value.height-48);}
function chartY(value){return plotSize.value.height-26-chartHeight(value);}
function columnX(index){return 39+index*columnWidth.value;}
function dateSeed(date){return Number(date.replaceAll('-',''))%1000000+1;}
function fullDate(date){return date?.replaceAll('-','.') || '';}
function calendarLabel(day){return `${fullDate(day.date)}，${day.date>data.value.today?'未来日期':calendarMode.value==='login'?day.loggedIn?'已登录':!data.value.loginRecordedSince || day.date<data.value.loginRecordedSince?'尚未开始记录':'未记录登录':`${day.studyEvents} 条学习记录，新学 ${day.newWords} 词，复习 ${day.reviewWords} 词`}`;}
async function load(){if(loading.value)return;loading.value=true;error.value='';try{data.value=await learningApi.journal();selectedDate.value=data.value.today;}catch(exception){error.value=exception.message || '稍后再试一次。';}finally{loading.value=false;}}
async function focusDay(index){const safe=Math.max(0,Math.min(trend.value.length-1,index));selectedDate.value=trend.value[safe].date;await nextTick();dayRefs.value[safe]?.focus();}
async function focusCalendar(index){const last=data.value.days.findIndex(day=>day.date===data.value.today);const safe=Math.max(0,Math.min(last,index));selectedDate.value=data.value.days[safe].date;await nextTick();calendarRefs.value[safe]?.focus();}
function openNotes(){notes.value?.showModal();}
watch(period,()=>{if(!trend.value.some(day=>day.date===selectedDate.value))selectedDate.value=data.value.today;});
const branches=['M180 302 C169 252 191 216 181 172 C174 136 180 92 180 63','M181 174 C144 156 125 128 91 111','M181 168 C220 140 241 115 270 103','M177 239 C136 246 121 230 90 222','M179 235 C220 239 244 221 270 215'];
const leaves=['M176 278 Q148 273 147 250 Q170 253 176 278 Z','M184 259 Q214 251 215 231 Q189 235 184 259 Z','M151 155 Q127 154 125 135 Q146 137 151 155 Z','M212 147 Q210 122 231 116 Q237 137 212 147 Z','M180 118 Q155 109 157 91 Q176 91 180 118 Z','M187 204 Q207 192 207 173 Q188 178 187 204 Z','M131 238 Q118 254 103 249 Q105 231 131 238 Z','M230 231 Q243 247 258 240 Q252 225 230 231 Z'];
const treeNodes=computed(()=>[{id:'chat',label:'对话交流',count:data.value.totals.chatTurns,unit:'轮',x:180,y:36,destination:'对话练习'},{id:'vocabulary',label:'累计学过',count:data.value.totals.studiedWords,unit:'词',x:64,y:98,destination:'单词练习'},{id:'mastered',label:'已掌握',count:data.value.totals.masteredWords,unit:'词',x:295,y:96,destination:'我的词库'},{id:'news',label:'完成阅读',count:data.value.totals.readings,unit:'篇',x:62,y:217,destination:'阅读空间'},{id:'writing',label:'写作练习',count:data.value.totals.essays,unit:'次',x:297,y:209,destination:'写作工坊'}]);
const sunRays=Array.from({length:8},(_,index)=>{const angle=index*Math.PI/4;return [45+Math.cos(angle)*31,45+Math.sin(angle)*31,45+Math.cos(angle)*39,45+Math.sin(angle)*39];});
watch(chartWrap,element=>{observer?.disconnect();if(!element)return;observer=new ResizeObserver(([entry])=>{plotSize.value={width:Math.max(200,entry.contentRect.width),height:Math.max(100,entry.contentRect.height)};});observer.observe(element);});
onMounted(load);
onBeforeUnmount(()=>observer?.disconnect());
</script>
