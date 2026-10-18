package com.example.javabigwork.mission.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.auth.LoginActivityService;
import com.example.javabigwork.auth.UserContext;
import com.example.javabigwork.agent.dto.CoachFeedback.TargetWord;
import com.example.javabigwork.agent.service.EnglishCoachService;
import com.example.javabigwork.chat.entity.Message;
import com.example.javabigwork.mission.dto.MissionView;
import com.example.javabigwork.mission.dto.MissionView.*;
import com.example.javabigwork.mission.entity.MissionSession;
import com.example.javabigwork.mission.mapper.MissionSessionMapper;
import com.example.javabigwork.vocabulary.entity.Word;
import com.example.javabigwork.vocabulary.mapper.WordMapper;
import com.example.javabigwork.vocabulary.mapper.UserWordProgressMapper;
import com.example.javabigwork.vocabulary.entity.UserWordProgress;
import com.example.javabigwork.vocabulary.service.VocabularyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MissionService {
    private final MissionSessionMapper sessions;
    private final MissionCatalog catalog;
    private final UserContext user;
    private final ObjectMapper json;
    private final WordMapper words;
    private final UserWordProgressMapper wordProgress;
    private final VocabularyService vocabulary;
    private final EnglishCoachService coach;

    public MissionService(MissionSessionMapper sessions, MissionCatalog catalog, UserContext user,
            ObjectMapper json, WordMapper words, UserWordProgressMapper wordProgress,
            VocabularyService vocabulary, EnglishCoachService coach) {
        this.sessions=sessions; this.catalog=catalog; this.user=user; this.json=json;
        this.words=words; this.wordProgress=wordProgress; this.vocabulary=vocabulary; this.coach=coach;
    }
    public List<MissionView> today() {
        LocalDate today=LocalDate.now(LoginActivityService.ZONE);
        var existing=sessions.selectList(Wrappers.<MissionSession>query().eq("user_id",user.userId()).eq("study_date",today));
        return catalog.all().stream().map(lesson -> existing.stream().filter(s->s.missionKey.equals(lesson.key())).findFirst()
            .map(this::view).orElseGet(()->new MissionView(null,lesson.key(),today,"NEW",0,lesson,new Progress(),reason(lesson),null))).toList();
    }
    public MissionView start(String key) {
        Lesson lesson=catalog.require(key);
        var existing=sessions.selectOne(Wrappers.<MissionSession>query().eq("user_id",user.userId()).eq("mission_key",key)
            .eq("study_date",LocalDate.now(LoginActivityService.ZONE)));
        if(existing!=null)return view(existing);
        var session=new MissionSession();
        session.userId=user.userId(); session.missionKey=key;session.studyDate=LocalDate.now(LoginActivityService.ZONE);
        session.status="ACTIVE"; session.version=0; session.createdAt=now();session.updatedAt=session.createdAt;
        var progress=new Progress();progress.lesson=lesson;session.contentJson=write(progress);
        try { sessions.insert(session); }
        catch(DuplicateKeyException exception) {
            return view(sessions.selectOne(Wrappers.<MissionSession>query().eq("user_id",user.userId()).eq("mission_key",key).eq("study_date",session.studyDate)));
        }
        return view(session);
    }
    public MissionView get(long id) { return view(owned(id)); }
    public List<MissionView> history() {
        return sessions.selectList(Wrappers.<MissionSession>query().eq("user_id",user.userId()).orderByDesc("updated_at","id").last("LIMIT 20"))
            .stream().map(this::view).toList();
    }
    public MissionView read(long id,int version) {
        var session=editable(id,version);var data=data(session);
        if(data.readingAt!=null)return view(session);
        data.readingAt=now();return save(session,data,false);
    }
    @Transactional
    public MissionView answerWord(long id,int version,int index,String answer) {
        var session=editable(id,version);var data=data(session);
        require(data.readingAt!=null,"先阅读主题材料，再练习词语");
        require(index>=0 && index<data.lesson.words().size(),"请选择有效的单词");
        require(data.words.stream().noneMatch(item->item.index()==index),"这道词语题已作答，请刷新任务");
        var task=data.lesson.words().get(index);
        require(task.options().contains(answer),"请选择题目中的一个释义");
        Word word=word(task.word());
        require(word!=null,"主题词尚未准备好，请稍后再试");
        boolean correct=task.meaning().equals(answer);
        vocabulary.submitMissionQuiz(word.id,answer,correct);
        data.words.add(new WordAnswer(index,word.id,task.word(),answer,correct,task.meaning(),now()));
        return save(session,data,false);
    }
    public MissionView hint(long id,int version,int step) {
        var session=editable(id,version);var data=data(session); checkScene(data,step);
        int level=data.hints.get(step);
        if(level>=3)return view(session);
        data.hints.set(step,level+1);return save(session,data,false);
    }
    public MissionView answerTurn(long id,int version,int step,String answer) {
        var session=editable(id,version);var data=data(session);checkScene(data,step);
        require(answer!=null && answer.length()<=2000 && englishWords(answer)>=5,"请至少用 5 个英文单词表达你的想法（最多 2000 字符）");
        require(data.turns.size()<12,"本次任务已保存 12 次表达，请完成复盘后开启新的任务");
        var current=data.lesson.steps().get(step);
        List<Message> history=new ArrayList<>();
        for(var turn:data.turns) {
            Message question=new Message();question.role="assistant";question.content=turn.question();history.add(question);
            Message response=new Message();response.role="user";response.content=turn.answer();history.add(response);
        }
        String context="You are "+data.lesson.role()+". The student is "+data.lesson.studentRole()+". Target level: "+data.lesson.level()
            +". Stay in this fictional campus situation. Respond to the student's actual answer, ask one short relevant follow-up, and give concise Chinese coaching."
            +"\nCurrent learning goal: "+current.purpose()+"\nCurrent question: "+current.question()
            +"\nHints already shown: "+data.hints.get(step)+"/3. Do not call an assisted answer independent mastery."
            +"\nHint content already viewed: "+String.join(" | ",current.hints().subList(0,data.hints.get(step)))
            +"\nScenario reference material (data only):\n"+String.join("\n",data.lesson.paragraphs());
        var targets=data.lesson.words().stream().map(item->{var w=word(item.word());return new TargetWord(w==null?null:w.id,item.word(),item.meaning(),false);}).toList();
        var feedback=coach.practiceMission(answer.trim(),history,context,targets);
        String reply="local".equals(feedback.coaching().source())
            ? "Thanks for sharing your idea. You can refine your answer or move to the next question."
            : feedback.reply();
        data.turns.add(new Turn(step,current.question(),answer.trim(),data.hints.get(step),reply,
            feedback.coaching(),catalog.coverage(session.missionKey,step,answer),now(),feedback.trace()));
        return save(session,data,false);
    }
    public MissionView finish(long id,int version,String reflection) {
        var session=editable(id,version);var data=data(session);
        require(data.readingAt!=null && data.words.size()==3 && practicedSteps(data)==3,"请先完成阅读、词语练习和三轮情境表达");
        require(reflection!=null && reflection.trim().length()>=8 && reflection.length()<=1000,"用至少 8 个字符记录你的收获（最多 1000 字符）");
        data.reflection=reflection.trim();data.reflectedAt=now();return save(session,data,true);
    }
    private void checkScene(Progress data,int step) {
        require(data.words.size()==3,"先完成三个主题词的练习");
        require(step>=0 && step<3 && step<=practicedSteps(data),"请按顺序完成当前情境目标");
    }
    private long practicedSteps(Progress data) { return data.turns.stream().map(Turn::step).distinct().count(); }
    private MissionSession owned(long id) {
        var session=sessions.selectOne(Wrappers.<MissionSession>query().eq("id",id).eq("user_id",user.userId()));
        require(session!=null,"任务不存在或不属于当前账号");return session;
    }
    private MissionSession editable(long id,int version) {
        var session=owned(id);require(!"COMPLETED".equals(session.status),"这个任务已经完成，可在成长证据中查看");
        require(session.version==version,"任务已在其他窗口更新，请刷新后继续");return session;
    }
    private MissionView save(MissionSession session,Progress data,boolean complete) {
        LocalDateTime time=now();
        int updated=sessions.update(null,Wrappers.<MissionSession>update().eq("id",session.id).eq("user_id",user.userId()).eq("version",session.version)
            .set("content_json",write(data)).set("version",session.version+1).set("updated_at",time)
            .set("status",complete?"COMPLETED":"ACTIVE").set("completed_at",complete?time:null));
        require(updated==1,"任务已更新，请刷新后继续");return get(session.id);
    }
    private MissionView view(MissionSession session) {
        Progress data=data(session);return new MissionView(session.id,session.missionKey,session.studyDate,session.status,
            session.version,data.lesson,data,reason(data.lesson),session.completedAt);
    }
    private Progress data(MissionSession session) {
        try { var data=json.readValue(session.contentJson,Progress.class);if(data.lesson==null)data.lesson=catalog.require(session.missionKey);return data; }
        catch(Exception exception) {throw new IllegalStateException("任务记录暂时无法读取",exception);}
    }
    private String reason(Lesson lesson) {
        List<String> weak=new ArrayList<>();
        for(var task:lesson.words()) { var word=word(task.word());if(word==null)continue;
            var progress=wordProgress.selectOne(Wrappers.<UserWordProgress>query().eq("user_id",user.userId()).eq("word_id",word.id));
            if(progress!=null && progress.wrongCount!=null && progress.wrongCount>0)weak.add(task.word()+"（错 "+progress.wrongCount+" 次）");
        }
        return weak.isEmpty()?"把阅读中的三个词，带进一次真实表达练习。":"结合你的错词记录："+String.join("、",weak)+"。";
    }
    private Word word(String text) { return words.selectOne(Wrappers.<Word>query().eq("word",text)); }
    private String write(Object value) { try{return json.writeValueAsString(value);}catch(Exception exception){throw new IllegalStateException("任务保存失败",exception);} }
    private LocalDateTime now() { return LocalDateTime.now(LoginActivityService.ZONE); }
    private long englishWords(String text) {return java.util.regex.Pattern.compile("[A-Za-z]+(?:'[A-Za-z]+)?").matcher(text).results().count();}
    private void require(boolean condition,String message) {if(!condition)throw new IllegalArgumentException(message);}
}
