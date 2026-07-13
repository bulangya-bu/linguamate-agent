package com.example.javabigwork.vocabulary.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.vocabulary.entity.Word;
import com.example.javabigwork.vocabulary.mapper.WordMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Loads local exam word bank JSON files into the database on startup.
 * Skips words that already exist (unique constraint safe).
 * <p>
 * JSON source: KyleBing/english-vocabulary (kajweb/dict format).
 * Files expected under classpath:word-bank/.
 */
@Component
public class LocalWordBankLoader {
    private static final Logger log = LoggerFactory.getLogger(LocalWordBankLoader.class);
    private static final Pattern ENGLISH_WORD = Pattern.compile("^[a-zA-Z'\\-]+$");
    private static final String[][] BANK_FILES = {
            {"word-bank/CET4_3.json", "cet4-bank"},
            {"word-bank/CET6_3.json", "cet6-bank"},
            {"word-bank/KaoYan_3.json", "kaoyan-bank"},
            {"word-bank/IELTS_3.json", "ielts-bank"}
    };

    private final WordMapper wordMapper;
    private final ObjectMapper json = new ObjectMapper();

    public LocalWordBankLoader(WordMapper wordMapper) {
        this.wordMapper = wordMapper;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void loadWordBank() {
        if (allLocalBanksLoaded()) {
            enrichExistingExamples();
            log.info("Word bank already initialized; skipping local JSON import");
            return;
        }

        int total = 0;
        int skipped = 0;
        int enriched = 0;
        Set<String> seen = new HashSet<>();
        for (String[] bankFile : BANK_FILES) {
            String path = bankFile[0];
            String tag = bankFile[1];
            int fileInserted = 0;
            int fileEnriched = 0;
            try {
                ClassPathResource resource = new ClassPathResource(path);
                if (!resource.exists()) {
                    log.warn("Word bank file not found: {}", path);
                    continue;
                }
                List<Map<String, Object>> entries;
                try (InputStream in = resource.getInputStream()) {
                    entries = json.readValue(in,
                            new TypeReference<List<Map<String, Object>>>() {});
                    for (Map<String, Object> entry : entries) {
                        Word word = parseEntry(entry, tag);
                        if (word == null) continue;
                        String key = word.word.toLowerCase(Locale.ROOT);
                        if (!seen.add(key)) {
                            skipped++;
                            continue;
                        }
                        Word existing = findExisting(key);
                        if (existing != null) {
                            if (fillMissingExamples(existing, word)) {
                                wordMapper.updateById(existing);
                                enriched++;
                                fileEnriched++;
                            } else {
                                skipped++;
                            }
                            continue;
                        }
                        wordMapper.insert(word);
                        total++;
                        fileInserted++;
                    }
                }
                log.info("Loaded {} new words and enriched {} words from {} [{}] ({} in file)",
                        fileInserted, fileEnriched, path, tag, entries.size());
            } catch (Exception e) {
                log.error("Failed to load word bank: {}", path, e);
            }
        }
        log.info("Word bank ready: {} new words inserted, {} words enriched, {} duplicates skipped",
                total, enriched, skipped);
    }

    private void enrichExistingExamples() {
        int enriched = 0;
        for (String[] bankFile : BANK_FILES) {
            String path = bankFile[0];
            String tag = bankFile[1];
            try {
                ClassPathResource resource = new ClassPathResource(path);
                if (!resource.exists()) {
                    continue;
                }
                try (InputStream in = resource.getInputStream()) {
                    List<Map<String, Object>> entries = json.readValue(in,
                            new TypeReference<List<Map<String, Object>>>() {});
                    for (Map<String, Object> entry : entries) {
                        Word source = parseEntry(entry, tag);
                        if (source == null) continue;
                        Word existing = findExisting(source.word);
                        if (existing != null && fillMissingExamples(existing, source)) {
                            wordMapper.updateById(existing);
                            enriched++;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to enrich word examples from {}", path, e);
            }
        }
        int fallbackEnriched = fillFallbackExamples();
        if (enriched > 0 || fallbackEnriched > 0) {
            log.info("Word bank examples enriched: {} from local JSON, {} fallback generated",
                    enriched, fallbackEnriched);
        }
    }

    private int fillFallbackExamples() {
        int enriched = 0;
        List<Word> missing = wordMapper.selectList(Wrappers.<Word>query()
                .and(w -> w.isNull("example_en").or().eq("example_en", "")
                        .or().isNull("example_cn").or().eq("example_cn", "")));
        for (Word word : missing) {
            boolean changed = false;
            if (word.exampleEn == null || word.exampleEn.isBlank()) {
                word.exampleEn = WordExampleFallback.englishFor(word.word);
                changed = true;
            }
            if (word.exampleCn == null || word.exampleCn.isBlank()) {
                word.exampleCn = WordExampleFallback.chineseFor(word.word);
                changed = true;
            }
            if (changed) {
                wordMapper.updateById(word);
                enriched++;
            }
        }
        return enriched;
    }

    private boolean allLocalBanksLoaded() {
        for (String[] bankFile : BANK_FILES) {
            String tag = bankFile[1];
            Long count = wordMapper.selectCount(Wrappers.<Word>query().eq("level_tag", tag));
            if (count == null || count == 0) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private Word parseEntry(Map<String, Object> entry, String tag) {
        try {
            String headWord = (String) entry.getOrDefault("headWord", "");
            if (headWord == null || headWord.length() < 2 || headWord.length() > 30) return null;
            if (!ENGLISH_WORD.matcher(headWord).matches()) return null;

            Map<String, Object> content = (Map<String, Object>) entry.get("content");
            if (content == null) return null;
            Map<String, Object> wordObj = (Map<String, Object>) content.get("word");
            if (wordObj == null) return null;
            Map<String, Object> wordContent = (Map<String, Object>) wordObj.get("content");
            if (wordContent == null) return null;

            String usphone = (String) wordContent.getOrDefault("usphone", "");
            String ukphone = (String) wordContent.getOrDefault("ukphone", "");

            // First Chinese translation
            List<Map<String, Object>> trans = (List<Map<String, Object>>) wordContent.get("trans");
            String meaningCn = "";
            String pos = "";
            if (trans != null) {
                for (Map<String, Object> t : trans) {
                    Object tranCn = t.get("tranCn");
                    if (tranCn != null && !tranCn.toString().isBlank()) {
                        meaningCn = tranCn.toString().trim();
                        pos = Objects.toString(t.getOrDefault("pos", ""), "").trim();
                        break;
                    }
                }
            }
            if (meaningCn.isEmpty()) return null;

            Word word = new Word();
            word.word = headWord.toLowerCase(Locale.ROOT);
            word.phoneticUs = formatPhonetic(usphone);
            word.phoneticUk = formatPhonetic(ukphone);
            word.meaningCn = meaningCn;
            word.partOfSpeech = pos;
            ExampleSentence example = firstExample(wordContent);
            word.exampleEn = example.hasEnglish()
                    ? example.en()
                    : WordExampleFallback.englishFor(word.word);
            word.exampleCn = example.hasChinese()
                    ? example.cn()
                    : WordExampleFallback.chineseFor(word.word);
            word.levelTag = tag;
            return word;
        } catch (Exception e) {
            return null;
        }
    }

    private String formatPhonetic(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String trimmed = raw.trim();
        if (trimmed.startsWith("/") && trimmed.endsWith("/")) return trimmed;
        return "/" + trimmed + "/";
    }

    @SuppressWarnings("unchecked")
    private ExampleSentence firstExample(Map<String, Object> wordContent) {
        ExampleSentence sentence = firstSentenceFromBlock((Map<String, Object>) wordContent.get("sentence"));
        if (sentence.hasEnglish()) {
            return sentence;
        }
        return firstSentenceFromBlock((Map<String, Object>) wordContent.get("realExamSentence"));
    }

    @SuppressWarnings("unchecked")
    private ExampleSentence firstSentenceFromBlock(Map<String, Object> block) {
        if (block == null) return ExampleSentence.empty();
        Object sentencesObj = block.get("sentences");
        if (!(sentencesObj instanceof List<?> sentences)) {
            return ExampleSentence.empty();
        }
        for (Object item : sentences) {
            if (!(item instanceof Map<?, ?> rawSentence)) {
                continue;
            }
            Map<String, Object> sentence = (Map<String, Object>) rawSentence;
            String en = cleanText(sentence.get("sContent"));
            if (en.isBlank()) {
                en = cleanText(sentence.get("content"));
            }
            String cn = cleanText(sentence.get("sCn"));
            if (en.isBlank()) {
                continue;
            }
            return new ExampleSentence(en, cn);
        }
        return ExampleSentence.empty();
    }

    private String cleanText(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private boolean fillMissingExamples(Word target, Word source) {
        boolean changed = false;
        if ((target.exampleEn == null || target.exampleEn.isBlank())
                && source.exampleEn != null && !source.exampleEn.isBlank()) {
            target.exampleEn = source.exampleEn;
            changed = true;
        }
        if ((target.exampleCn == null || target.exampleCn.isBlank())
                && source.exampleCn != null && !source.exampleCn.isBlank()) {
            target.exampleCn = source.exampleCn;
            changed = true;
        }
        return changed;
    }

    private Word findExisting(String word) {
        return wordMapper.selectList(Wrappers.<Word>query().eq("word", word).last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private record ExampleSentence(String en, String cn) {
        static ExampleSentence empty() {
            return new ExampleSentence("", "");
        }

        boolean hasEnglish() {
            return en != null && !en.isBlank();
        }

        boolean hasChinese() {
            return cn != null && !cn.isBlank();
        }
    }
}
