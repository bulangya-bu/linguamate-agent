package com.example.javabigwork.chat.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.agent.AgentTraceRecorder;
import com.example.javabigwork.agent.LinguaMateAgent;
import com.example.javabigwork.agent.service.EnglishCoachService;
import com.example.javabigwork.agent.dto.CoachFeedback.TargetWord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.javabigwork.chat.dto.ChatRequest;
import com.example.javabigwork.chat.dto.ChatResponse;
import com.example.javabigwork.chat.entity.Conversation;
import com.example.javabigwork.chat.entity.Message;
import com.example.javabigwork.chat.mapper.MessageMapper;
import com.example.javabigwork.vocabulary.service.ExperienceService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final ConversationService conversationService;
    private final MessageMapper messageMapper;
    private final LinguaMateAgent agent;
    private final ExperienceService experienceService;
    private final AgentTraceRecorder traceRecorder;
    private final EnglishCoachService coach;
    private final ObjectMapper json;

    public ChatService(ConversationService conversationService, MessageMapper messageMapper, LinguaMateAgent agent, ExperienceService experienceService, AgentTraceRecorder traceRecorder, EnglishCoachService coach, ObjectMapper json) {
        this.conversationService = conversationService;
        this.messageMapper = messageMapper;
        this.agent = agent;
        this.experienceService = experienceService;
        this.traceRecorder = traceRecorder;
        this.coach = coach;
        this.json = json;
    }

    public ChatResponse chat(ChatRequest request) {
        Conversation conversation = conversationService.getOrCreate(request.conversationId(), request.scene(), request.message());
        List<Message> history = messageMapper.selectList(Wrappers.<Message>query()
                .eq("conversation_id", conversation.id)
                .orderByDesc("created_at", "id")
                .last("LIMIT 12"));
        Collections.reverse(history);
        boolean practice = "practice".equals(request.mode());
        saveMessage(conversation.id, "user", request.message(), practice ? "{\"mode\":\"practice\"}" : "{\"mode\":\"assistant\"}");
        log.info("Chat request: conversationId={}, scene={}, message={}", conversation.id, conversation.scene, request.message());
        ChatResponse response;
        if (practice) {
            var result = coach.practice(conversation.scene, request.message(), history);
            response = new ChatResponse(conversation.id, result.reply(), result.trace(), List.of(), null, List.of(), result.coaching());
        } else {
            var result = agent.chat(conversation.scene, request.message(), history);
            response = new ChatResponse(conversation.id, result.reply(), result.trace(), result.suggestions(), result.learningCard(), result.actions(), null);
        }
        saveMessage(conversation.id, "assistant", response.reply(), metadata(response));
        traceRecorder.flush(response.trace(), String.valueOf(conversation.id));
        conversationService.touch(conversation.id);
        experienceService.addXp(5);
        return response;
    }

    public List<TargetWord> practiceContext() { return coach.context(); }

    private String metadata(ChatResponse response) {
        try { return json.writeValueAsString(response); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("无法保存对话反馈", exception); }
    }

    public List<Message> messages(Long conversationId) {
        conversationService.requireOwned(conversationId);
        return messageMapper.selectList(Wrappers.<Message>query()
                .eq("conversation_id", conversationId)
                .orderByAsc("created_at", "id"));
    }

    private void saveMessage(Long conversationId, String role, String content, String metadata) {
        Message message = new Message();
        message.conversationId = conversationId;
        message.role = role;
        message.content = content;
        message.metadata = metadata;
        message.createdAt = LocalDateTime.now();
        messageMapper.insert(message);
    }
}
