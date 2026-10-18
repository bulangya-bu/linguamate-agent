package com.example.javabigwork.report.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.auth.UserContext;
import com.example.javabigwork.mission.service.MissionService;
import com.example.javabigwork.report.dto.GrowthEvidenceView;
import com.example.javabigwork.report.dto.GrowthEvidenceView.*;
import com.example.javabigwork.vocabulary.entity.WordReviewLog;
import com.example.javabigwork.vocabulary.mapper.WordReviewLogMapper;
import com.example.javabigwork.vocabulary.mapper.WordMapper;
import com.example.javabigwork.writing.entity.EssaySubmission;
import com.example.javabigwork.writing.mapper.EssaySubmissionMapper;
import com.example.javabigwork.chat.entity.Message;
import com.example.javabigwork.chat.entity.Conversation;
import com.example.javabigwork.chat.mapper.MessageMapper;
import com.example.javabigwork.chat.mapper.ConversationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.javabigwork.agent.dto.CoachFeedback;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class GrowthEvidenceService {
    private final UserContext user;
    private final WordReviewLogMapper logs;
    private final WordMapper words;
    private final EssaySubmissionMapper essays;
    private final MissionService missions;
    private final MessageMapper messages;
    private final ConversationMapper conversations;
    private final ObjectMapper json;
    public GrowthEvidenceService(UserContext user,WordReviewLogMapper logs,WordMapper words,EssaySubmissionMapper essays,MissionService missions,
            MessageMapper messages,ConversationMapper conversations,ObjectMapper json) {
        this.user=user;this.logs=logs;this.words=words;this.essays=essays;this.missions=missions;
        this.messages=messages;this.conversations=conversations;this.json=json;
    }
    public GrowthEvidenceView evidence() {
        var recent=logs.selectList(Wrappers.<WordReviewLog>query().eq("user_id",user.userId()).in("review_type","QUIZ","STRICT_REVIEW")
            .orderByDesc("created_at","id").last("LIMIT 400"));
        Map<Long,List<WordReviewLog>> groups=new LinkedHashMap<>();
        for(var log:recent)groups.computeIfAbsent(log.wordId,key->new ArrayList<>()).add(log);
        List<WordEvidence> wordEvidence=new ArrayList<>();
        for(var entry:groups.entrySet()) {
            var word=words.selectById(entry.getKey());if(word==null)continue;
            var attempts=entry.getValue().stream().limit(8).sorted(Comparator.comparing((WordReviewLog item)->item.createdAt).thenComparing(item->item.id))
                .map(item->new Attempt(item.id,item.answer,Boolean.TRUE.equals(item.correct),item.reviewType,item.createdAt)).toList();
            wordEvidence.add(new WordEvidence(word.id,word.word,word.meaningCn,attempts));
            if(wordEvidence.size()==16)break;
        }
        var writing=essays.selectList(Wrappers.<EssaySubmission>query().eq("user_id",user.userId()).orderByDesc("created_at","id").last("LIMIT 8"))
            .stream().map(this::writing).toList();
        return new GrowthEvidenceView(wordEvidence,writing,missions.history().stream().limit(8).toList(),expressions());
    }
    private List<ExpressionEvidence> expressions() {
        var owned=conversations.selectList(Wrappers.<Conversation>query().eq("user_id",user.userId()).orderByDesc("updated_at","id").last("LIMIT 30"));
        if(owned.isEmpty())return List.of();
        var rows=messages.selectList(Wrappers.<Message>query().in("conversation_id",owned.stream().map(c->c.id).toList())
            .orderByDesc("created_at","id").last("LIMIT 120"));
        List<ExpressionEvidence> result=new ArrayList<>();
        for(var row:rows) {
            if(!"user".equals(row.role))continue;
            var next=rows.stream().filter(m->m.conversationId.equals(row.conversationId) && m.id>row.id)
                .min(Comparator.comparing(m->m.id)).orElse(null);
            if(next!=null && !"assistant".equals(next.role))next=null;
            CoachFeedback feedback=null;
            if(next!=null && next.metadata!=null)try {var node=json.readTree(next.metadata).path("coaching");if(!node.isMissingNode() && !node.isNull())feedback=json.treeToValue(node,CoachFeedback.class);}catch(Exception ignored){/* Legacy messages have no structured feedback. */}
            String title=owned.stream().filter(c->c.id.equals(row.conversationId)).map(c->c.title==null?"学习对话":c.title).findFirst().orElse("学习对话");
            result.add(new ExpressionEvidence(row.id,row.conversationId,title,row.content,next==null?null:next.content,feedback,row.createdAt));
            if(result.size()==12)break;
        }
        return result;
    }
    private WritingEvidence writing(EssaySubmission current) {
        var previous=current.revisionOfId==null?null:essays.selectOne(Wrappers.<EssaySubmission>query().eq("id",current.revisionOfId).eq("user_id",user.userId()));
        boolean comparable=previous!=null && current.reviewSource!=null && current.modelName!=null
            && Objects.equals(previous.reviewSource,current.reviewSource) && Objects.equals(previous.modelName,current.modelName)
            && Objects.equals(previous.examType,current.examType) && Objects.equals(previous.targetLevel,current.targetLevel);
        return new WritingEvidence(current.id,current.title,previous==null?null:previous.id,previous==null?"":previous.originalText,
            current.originalText,current.polishedText,previous==null?null:previous.totalScore,current.totalScore,comparable,
            current.reviewSource,current.modelName,current.createdAt);
    }
}
