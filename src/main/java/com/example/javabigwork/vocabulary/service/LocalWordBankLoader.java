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
 * Loads CET-4/CET-6 word bank JSON files into the H2 database on startup.
 * Skips words that already exist (unique constraint safe).
 * <p>
 * JSON source: KyleBing/english-vocabulary (kajweb/dict format).
 * Files expected at: classpath:word-bank/CET4_3.json, classpath:word-bank/CET6_3.json
 */
@Component
public class LocalWordBankLoader {
    private static final Logger log = LoggerFactory.getLogger(LocalWordBankLoader.class);
    private static final Pattern ENGLISH_WORD = Pattern.compile("^[a-zA-Z'\\-]+$");
    private static final String[][] BANK_FILES = {
            {"word-bank/CET4_3.json", "cet4-bank"},
            {"word-bank/CET6_3.json", "cet6-bank"}
    };

    private final WordMapper wordMapper;
    private final ObjectMapper json = new ObjectMapper();

    public LocalWordBankLoader(WordMapper wordMapper) {
        this.wordMapper = wordMapper;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void loadWordBank() {
        int total = 0;
        int skipped = 0;
        Set<String> seen = new HashSet<>();
        for (String[] bankFile : BANK_FILES) {
            String path = bankFile[0];
            String tag = bankFile[1];
            int fileInserted = 0;
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
                        if (!seen.add(key) || exists(key)) {
                            skipped++;
                            continue;
                        }
                        wordMapper.insert(word);
                        total++;
                        fileInserted++;
                    }
                }
                log.info("Loaded {} new words from {} [{}] ({} in file)", fileInserted, path, tag, entries.size());
            } catch (Exception e) {
                log.error("Failed to load word bank: {}", path, e);
            }
        }
        log.info("Word bank ready: {} new words inserted, {} duplicates skipped", total, skipped);
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

    private boolean exists(String word) {
        Long count = wordMapper.selectCount(
                Wrappers.<Word>query().eq("word", word));
        return count != null && count > 0;
    }
}
