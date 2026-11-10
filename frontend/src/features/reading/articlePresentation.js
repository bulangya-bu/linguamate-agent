function splitArticleTopics(topicTags) {
  return String(topicTags || "")
    .split(/[,，;；\s]+/)
    .map((topic) => topic.trim().toLowerCase())
    .filter(Boolean);
}

function newsTopicLabel(topic) {
  return newsTopicLabelMap[topic] || topic;
}

function articleTopicText(topicTags) {
  const topics = splitArticleTopics(topicTags).map(newsTopicLabel);
  if (!topics.length) return "综合";
  const shown = topics.slice(0, 3).join(" / ");
  return topics.length > 3 ? `${shown} 等` : shown;
}

function articleReadingTime(article) {
  const words =
    (article?.content || article?.summary || "").match(
      /[A-Za-z][A-Za-z'-]*/g,
    ) || [];
  return Math.max(1, Math.ceil(words.length / 180));
}

function difficultyLabel(value) {
  const option = newsDifficultyOptions.find((item) => item.value === value);
  return option?.label || value || "全部";
}

const newsDifficultyOptions = [
  { value: "", label: "全部" },
  { value: "easy", label: "简单" },
  { value: "medium", label: "进阶" },
  { value: "hard", label: "挑战" },
];

const curatedNewsTopics = [
  { value: "", label: "全部" },
  { value: "story", label: "故事" },
  { value: "science", label: "科学" },
  { value: "technology", label: "科技" },
  { value: "culture", label: "文化" },
  { value: "health", label: "健康" },
  { value: "environment", label: "环境" },
  { value: "business", label: "商业" },
  { value: "education", label: "教育" },
  { value: "career", label: "职业" },
  { value: "friendship", label: "友谊" },
  { value: "honesty", label: "诚信" },
  { value: "kindness", label: "善意" },
  { value: "perseverance", label: "坚持" },
  { value: "wisdom", label: "智慧" },
  { value: "growth", label: "成长" },
];

const newsTopicLabelMap = {
  story: "故事",
  perseverance: "坚持",
  pride: "骄傲",
  honesty: "诚信",
  trust: "信任",
  kindness: "善意",
  strength: "力量",
  preparation: "准备",
  "hard-work": "勤奋",
  gratitude: "感恩",
  acceptance: "接纳",
  flexibility: "灵活",
  humility: "谦逊",
  friendship: "友谊",
  loyalty: "忠诚",
  greed: "贪婪",
  contentment: "知足",
  community: "社区",
  confidence: "自信",
  uniqueness: "独特",
  wisdom: "智慧",
  observation: "观察",
  courage: "勇气",
  integrity: "正直",
  freedom: "自由",
  clever: "机智",
  hope: "希望",
  sacrifice: "奉献",
  patience: "耐心",
  dedication: "投入",
  endurance: "耐力",
  "self-reflection": "自省",
  truth: "真相",
  growth: "成长",
  legacy: "传承",
  generosity: "慷慨",
  science: "科学",
  technology: "科技",
  culture: "文化",
  health: "健康",
  environment: "环境",
  business: "商业",
  education: "教育",
  career: "职业",
  travel: "旅行",
  lifestyle: "生活",
  history: "历史",
  responsibility: "责任",
  communication: "沟通",
  service: "服务",
  research: "研究",
};

export {
  splitArticleTopics,
  newsTopicLabel,
  articleTopicText,
  articleReadingTime,
  difficultyLabel,
  newsDifficultyOptions,
  curatedNewsTopics,
  newsTopicLabelMap,
};
