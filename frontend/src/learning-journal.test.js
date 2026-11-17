import test from 'node:test';
import assert from 'node:assert/strict';
import {chartScale,dayIntensity,journalMonthLayout,journalReadingsThisWeek,shiftJournalMonth,shortDate,trendDays} from './learning-journal.js';
test('periods end at today and exclude calendar future cells',()=>{
  const days=Array.from({length:40},(_,index)=>({date:`2026-10-${String(index+1).padStart(2,'0')}`}));
  const recent=trendDays(days,'2026-10-18',7);
  assert.equal(recent.length,7);assert.equal(recent[0].date,'2026-10-12');assert.equal(recent.at(-1).date,'2026-10-18');
});

test('weekly reading totals count only the current week up to today', () => {
  const journal = {
    today: '2026-11-18',
    days: Array.from({ length: 14 }, (_, index) => ({
      date: `2026-11-${String(9 + index).padStart(2, '0')}`,
      readingsFinished: index < 8 ? 2 : 7,
    })),
  };
  assert.equal(journalReadingsThisWeek(journal), 16);
  assert.equal(journalReadingsThisWeek({ ...journal, today: '2026-11-16' }), 2);
});

test('calendar months start on Monday and include only the selected month', () => {
  const november = journalMonthLayout('2026-11');
  assert.equal(november.offset, 6);
  assert.equal(november.dates[0], '2026-11-01');
  assert.equal(november.dates.at(-1), '2026-11-30');
  assert.equal(journalMonthLayout('2024-02').dates.at(-1), '2024-02-29');
  assert.equal(journalMonthLayout('2026-02').dates.length, 28);
});

test('month navigation crosses the end of the year in both directions', () => {
  assert.equal(shiftJournalMonth('2027-01', -1), '2026-12');
  assert.equal(shiftJournalMonth('2026-12', 1), '2027-01');
});

test('chart scale keeps zero baseline and all actual values in range',()=>{
  for(const value of [0,1,4,18,130,999]){const scale=chartScale([{newWords:value,reviewWords:2}]);assert.ok(scale.maximum>=value);assert.equal(scale.ticks[0],0);assert.equal(scale.ticks.at(-1),scale.maximum);}
  assert.equal(dayIntensity({studyEvents:0,loggedIn:true}),0);
  assert.equal(dayIntensity({studyEvents:20,loggedIn:false}),4);
  assert.equal(shortDate('2026-01-02'),'1.2');
});
