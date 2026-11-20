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
export function hasStudyActivity(day) {
  // Older journals counted the start of an article but could omit a later completion.
  return day.studyEvents > 0 || day.readingsFinished > 0;
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
