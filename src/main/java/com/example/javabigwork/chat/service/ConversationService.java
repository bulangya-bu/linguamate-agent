package com.example.javabigwork.chat.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.chat.dto.ConversationView;
import com.example.javabigwork.chat.entity.Conversation;
import com.example.javabigwork.chat.mapper.ConversationMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {
    private final ConversationMapper conversationMapper;

    public ConversationService(ConversationMapper conversationMapper) {
        this.conversationMapper = conversationMapper;
    }

    public Conversation getOrCreate(Long conversationId, String scene, String firstMessage) {
        if (conversationId != null) {
            Conversation existing = conversationMapper.selectById(conversationId);
            if (existing != null) {
                return existing;
            }
        }
        Conversation conversation = new Conversation();
        conversation.scene = scene == null || scene.isBlank() ? "daily" : scene;
        conversation.title = firstMessage.length() > 24 ? firstMessage.substring(0, 24) + "..." : firstMessage;
        conversation.createdAt = LocalDateTime.now();
        conversation.updatedAt = LocalDateTime.now();
        conversationMapper.insert(conversation);
        return conversation;
    }

    public List<ConversationView> list() {
        return conversationMapper.selectList(Wrappers.<Conversation>query().orderByDesc("updated_at"))
                .stream()
                .map(item -> new ConversationView(item.id, item.scene, item.title, item.updatedAt))
                .toList();
    }

    public void touch(Long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            conversation.updatedAt = LocalDateTime.now();
            conversationMapper.updateById(conversation);
        }
    }
}
