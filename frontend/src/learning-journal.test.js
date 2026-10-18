import test from 'node:test';
import assert from 'node:assert/strict';
import {chartScale,dayIntensity,shortDate,trendDays} from './learning-journal.js';
test('periods end at today and exclude calendar future cells',()=>{
  const days=Array.from({length:40},(_,index)=>({date:`2026-10-${String(index+1).padStart(2,'0')}`}));
  const recent=trendDays(days,'2026-10-18',7);
  assert.equal(recent.length,7);assert.equal(recent[0].date,'2026-10-12');assert.equal(recent.at(-1).date,'2026-10-18');
});
test('chart scale keeps zero baseline and all actual values in range',()=>{
  for(const value of [0,1,4,18,130,999]){const scale=chartScale([{newWords:value,reviewWords:2}]);assert.ok(scale.maximum>=value);assert.equal(scale.ticks[0],0);assert.equal(scale.ticks.at(-1),scale.maximum);}
  assert.equal(dayIntensity({studyEvents:0}),0);assert.equal(dayIntensity({studyEvents:20}),4);
  assert.equal(dayIntensity({studyEvents:20,loggedIn:false},'login'),0);
  assert.equal(dayIntensity({loggedIn:true},'login'),3);assert.equal(shortDate('2026-01-02'),'1.2');
});
