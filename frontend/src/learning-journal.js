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
export function dayIntensity(day,mode='study') {
  if(mode==='login')return day.loggedIn?3:0;
  const events=Number(day.studyEvents)||0;
  return events===0?0:events<3?1:events<8?2:events<16?3:4;
}
