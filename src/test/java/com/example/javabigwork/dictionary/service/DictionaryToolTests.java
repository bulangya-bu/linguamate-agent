package com.example.javabigwork.dictionary.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.javabigwork.auth.UserContext;
import com.example.javabigwork.cache.RedisCacheService;
import com.example.javabigwork.tool.DictionaryTool;
import com.example.javabigwork.vocabulary.entity.Word;
import com.example.javabigwork.vocabulary.mapper.UserWordProgressMapper;
import com.example.javabigwork.vocabulary.mapper.WordMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

class DictionaryToolTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final OfflineDictionaryService OFFLINE = new OfflineDictionaryService(JSON);
    private final WordMapper words = mock(WordMapper.class);
    private final UserWordProgressMapper progress = mock(UserWordProgressMapper.class);
    private final OnlineWordService online = mock(OnlineWordService.class);
    private final UserContext user = mock(UserContext.class);
    private final Map<String, Word> stored = new HashMap<>();
    private DictionaryTool tool;

    @BeforeEach void setup() {
        tool = new DictionaryTool(words, progress, online, user,
                new RedisCacheService(mock(StringRedisTemplate.class), JSON, false), OFFLINE);
        when(user.userId()).thenReturn(77L);
        when(words.selectList(any())).thenAnswer(call -> {
            QueryWrapper<?> query = call.getArgument(0);
            query.getSqlSegment();
            String name = query.getParamNameValuePairs().values().iterator().next().toString();
            return stored.containsKey(name) ? List.of(stored.get(name)) : List.of();
        });
        when(progress.selectCount(any())).thenReturn(0L);
    }

    @Test void missingWordsAndKnownFormsResolveOfflineWithoutWritingOnHover() {
        assertTrue(tool.lookup("language").meaningCn().contains("语言"));
        assertEquals("scientist", tool.lookup("scientists").word());
        assertEquals("go", tool.lookup("went").word());
        assertTrue(tool.lookup("don’t").meaningCn().matches("(?s).*[\\p{IsHan}].*"));
        assertTrue(tool.lookup("scientist's").meaningCn().contains("科学家"));
        verifyNoInteractions(online);
        verify(words, never()).insert(any(Word.class));
        verify(progress, never()).insert(any(com.example.javabigwork.vocabulary.entity.UserWordProgress.class));
    }

    @Test void incompleteStoredDefinitionsAreRepairedWithoutChangingTheirId() {
        Word old = new Word(); old.id = 42L; old.word = "language"; old.meaningCn = "词库暂未收录"; old.levelTag = "online-fallback";
        stored.put("language", old);
        var result = tool.saveToReviewBook("language");
        assertTrue(result.meaningCn().contains("语言"));
        verify(words, never()).insert(any(Word.class));
        verify(words).updateById(argThat((Word word) -> word.id.equals(42L) && word.meaningCn.contains("语言")));
        verify(progress).insert(argThat((com.example.javabigwork.vocabulary.entity.UserWordProgress row) -> row.userId.equals(77L) && row.wordId.equals(42L)));
        verifyNoInteractions(online);
    }

    @Test void placeholderResultsCannotBecomeSavedWordsOrInventedPronunciations() {
        Word placeholder = new Word(); placeholder.word = "notarealwordxyzxyz"; placeholder.meaningCn = "在线词典暂未收录"; placeholder.levelTag = "online-fallback";
        when(online.resolveWord("notarealwordxyzxyz", false)).thenReturn(placeholder);
        var missing = tool.lookup("notarealwordxyzxyz");
        assertTrue(missing.phoneticUk().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> tool.saveToReviewBook("notarealwordxyzxyz"));
        verify(words, never()).insert(any(Word.class));
        verify(progress, never()).insert(any(com.example.javabigwork.vocabulary.entity.UserWordProgress.class));
    }
}
