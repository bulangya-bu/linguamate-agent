export function trendDays(days,today,period=7) {
  return (days || []).filter(day=>day.date<=today).slice(-period);
}
export function chartScale(days) {
  const maximum=Math.max(1,...days.flatMap(day=>[Number(day.newWords)||0,Number(day.reviewWords)||0]));
  const raw=maximum/4;
  const unit=10**Math.floor(Math.log10(Math.max(1,raw)));
  const step=Math.max(1,Math.ceil(raw/unit)*unit);
  return {maximum:step*4,ticks:[0,step,step*2,step*3,step*4]};
}
export function shortDate(value) { return `${Number(value.slice(5,7))}.${Number(value.slice(8,10))}`; }
export function dayIntensity(day) {
  const events=Number(day.studyEvents)||0;
  return events===0?0:events<3?1:events<8?2:events<16?3:4;
}

export function shiftJournalMonth(month, step) {
  const date = new Date(month + "-01T00:00:00Z");
  date.setUTCMonth(date.getUTCMonth() + step);
  return date.toISOString().slice(0, 7);
}

export function journalMonthLayout(month) {
  const first = new Date(month + "-01T00:00:00Z");
  const last = new Date(first);
  last.setUTCMonth(first.getUTCMonth() + 1, 0);
  return {
    offset: (first.getUTCDay() + 6) % 7,
    dates: Array.from({ length: last.getUTCDate() }, (_, index) =>
      month + "-" + String(index + 1).padStart(2, "0")),
  };
}

export function journalReadingsThisWeek(journal) {
  // The journal calendar starts on a Monday and includes future cells.
  const todayIndex = journal.days.findIndex(day => day.date === journal.today);
  const week = todayIndex < 0 ? [] : journal.days.slice(todayIndex - todayIndex % 7, todayIndex + 1);
  return week.reduce((total, day) => total + (day.readingsFinished || 0), 0);
}
