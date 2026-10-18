package com.example.javabigwork.dictionary.service;

import com.example.javabigwork.vocabulary.entity.Word;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/** Read-only bilingual supplement. Entries enter the user's word bank only when saved. */
@Service
public class OfflineDictionaryService {
    private static final Logger log = LoggerFactory.getLogger(OfflineDictionaryService.class);
    public static final String SOURCE_TAG = "reading-dictionary";
    private final Map<String, Entry> entries;
    private final Map<String, String> forms;

    public OfflineDictionaryService(ObjectMapper json) {
        Map<String, Entry> loaded = new LinkedHashMap<>();
        Map<String, String> loadedForms = Map.of();
        try (var input = new GZIPInputStream(new ClassPathResource("dictionary/reading-core.json.gz").getInputStream())) {
            DictionaryData data = json.readValue(input, DictionaryData.class);
            for (List<String> row : data.entries()) {
                if (row.size() == 5 && !row.get(2).isBlank()) {
                    loaded.put(row.get(0), new Entry(row.get(0), row.get(1), row.get(2), row.get(3), row.get(4)));
                }
            }
            loadedForms = data.forms();
            log.info("Offline reading dictionary ready: {} entries, {} inflection mappings", loaded.size(), loadedForms.size());
        } catch (IOException exception) {
            log.error("Offline reading dictionary unavailable; retaining existing word-bank lookup", exception);
        }
        entries = Map.copyOf(loaded);
        forms = Map.copyOf(loadedForms);
    }

    public Optional<Word> lookup(String normalized) {
        if (normalized == null || normalized.isBlank()) return Optional.empty();
        String key = normalized.toLowerCase(Locale.ROOT);
        Entry entry = entries.get(key);
        String base = forms.get(key);
        if (entry == null && base != null) entry = entries.get(base);
        if (entry == null) return Optional.empty();
        if (!entry.lemma().isBlank() && entries.containsKey(entry.lemma())) entry = entries.get(entry.lemma());
        Word word = new Word();
        word.word = entry.word();
        // Upstream provides predominantly British IPA, not a separately verified US pronunciation.
        word.phoneticUs = "";
        word.phoneticUk = entry.phonetic().isBlank() ? "" : "/" + entry.phonetic().replaceAll("^/|/$", "") + "/";
        word.meaningCn = entry.meaningCn();
        word.partOfSpeech = entry.partOfSpeech();
        word.exampleEn = "";
        word.exampleCn = "";
        word.levelTag = SOURCE_TAG;
        return Optional.of(word);
    }

    private record Entry(String word, String phonetic, String meaningCn, String partOfSpeech, String lemma) {}
    private record DictionaryData(List<List<String>> entries, Map<String, String> forms) {}
}
