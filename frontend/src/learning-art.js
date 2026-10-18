export const discoveryWords = [
  { en: "wonder", cn: "好奇；惊奇" },
  { en: "unfold", cn: "展开；显现" },
  { en: "bloom", cn: "绽放；成长" },
  { en: "discover", cn: "发现；探索" },
  { en: "curiosity", cn: "好奇心" },
  { en: "wander", cn: "漫步；探索" },
  { en: "radiance", cn: "光辉；神采" },
  { en: "resonate", cn: "共鸣；回响" },
  { en: "embrace", cn: "拥抱；接纳" },
  { en: "whisper", cn: "低语；轻声说" },
  { en: "glimmer", cn: "微光；希望" },
  { en: "thrive", cn: "茁壮成长" },
  { en: "kindred", cn: "志趣相投的" },
  { en: "solace", cn: "慰藉；安慰" },
  { en: "lucid", cn: "清晰的；明澈的" },
  { en: "luminous", cn: "明亮的；发光的" },
  { en: "horizon", cn: "地平线；眼界" },
  { en: "rhythm", cn: "节奏；韵律" },
  { en: "gentle", cn: "温柔的；轻柔的" },
  { en: "savor", cn: "品味；细细体会" },
  { en: "serene", cn: "宁静的；安然的" },
  { en: "empathy", cn: "共情；同理心" },
  { en: "weave", cn: "编织；交织" },
  { en: "deepen", cn: "加深；深入" },
  { en: "renew", cn: "重焕生机" },
  { en: "dwell", cn: "居住；沉思" },
  { en: "haven", cn: "避风港" },
  { en: "ripple", cn: "涟漪；波纹" },
  { en: "cherish", cn: "珍惜；爱护" },
  { en: "envision", cn: "想象；展望" },
  { en: "clarity", cn: "清晰；明晰" },
  { en: "insight", cn: "洞察；领悟" },
];

// Shuffle a whole deck so words do not repeat until every page has been seen.
export function shuffledWords(previous = "", random = Math.random) {
  const deck = [...discoveryWords];
  for (let index = deck.length - 1; index > 0; index--) {
    const other = Math.floor(random() * (index + 1));
    [deck[index], deck[other]] = [deck[other], deck[index]];
  }
  if (deck[0].en === previous && deck.length > 1)
    [deck[0], deck[1]] = [deck[1], deck[0]];
  return deck;
}
