package com.example.javabigwork.mission.service;

import com.example.javabigwork.mission.dto.MissionView.*;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MissionCatalog {
    private final List<Lesson> lessons = List.of(
        new Lesson("water", "让每一滴水，都有去处", "向交换生介绍一项校园节水设计", "Alex · 好奇的交换生", "校园环保讲解员",
            "A2–B1", "约 8 分钟", "LinguaMate 原创情境材料 · 虚构校园项目",
            List.of("Our campus is testing a simple way to reduce water waste. Some taps keep running when nobody is using them. The student team wants to change that.",
                "A small sensor detects a hand near the tap. Water flows while the hand is there and stops when it moves away. Students can also report a leaking tap to the team.",
                "The team will compare water use before and after the test. A new device is only one part of the plan: checking leaks and changing daily habits matter too."),
            List.of(word("reduce", "减少", "We want to reduce water waste on campus.", "我们希望减少校园中的水资源浪费。", "减少", "借用", "修理"),
                word("sensor", "传感器", "The sensor detects a hand near the tap.", "传感器检测水龙头附近的手。", "传感器", "容器", "水源"),
                word("waste", "浪费", "A leaking tap can waste water.", "漏水的水龙头会浪费水。", "浪费", "储存", "测量")),
            List.of(step("说明问题", "What problem is the student team trying to solve?", "说明校园遇到的问题，以及为什么值得改进。",
                    "试着带上 reduce / waste / water。", "The team wants to reduce ___ because ___.", "The team wants to reduce water waste because some taps keep running."),
                step("讲清原理", "How does the sensor help the tap save water?", "用自己的话解释装置如何工作。",
                    "按 hand → sensor → water 的顺序想一想。", "When the sensor detects ___, the tap ___.", "When the sensor detects a hand, water flows. It stops when the hand moves away."),
                step("提出行动", "What can you do if you find a leaking tap on campus?", "给出一项具体行动和理由。",
                    "可以考虑 report / check / fix。", "I would ___ the leaking tap so that ___.", "I would report the leaking tap to the team so that they can fix it and reduce waste."))),
        new Lesson("welcome", "把校园，介绍给新朋友", "帮助刚到校的国际学生完成第一天的探索", "Jamie · 第一次来校的新同学", "迎新校园向导",
            "A2–B1", "约 8 分钟", "LinguaMate 原创情境材料 · 虚构校园地图",
            List.of("Jamie is a new student on campus. The welcome desk is beside the main gate. Jamie needs to find the library and borrow a book before meeting a classmate.",
                "From the main gate, walk straight past the garden. Turn left at the student centre. The library is beside the cafe. The library is open until six this evening.",
                "Books and quiet study spaces are available in the library. A student card is needed to borrow a book. If Jamie needs more help, the welcome desk can explain the next step."),
            List.of(word("library", "图书馆", "The library is beside the cafe.", "图书馆在咖啡馆旁边。", "图书馆", "实验室", "操场"),
                word("direction", "方向；路线指引", "Can you give me directions to the library?", "你能告诉我去图书馆怎么走吗？", "方向；路线指引", "课程安排", "借阅记录"),
                word("available", "可用的", "Quiet study spaces are available in the library.", "图书馆有可用的安静学习空间。", "可用的", "遥远的", "昂贵的")),
            List.of(step("指明路线", "How can I get from the main gate to the library?", "给出至少一个方向动作和一个地点参照。",
                    "按 straight → left → beside 的顺序组织。", "Walk ___, then turn ___ at ___.", "Walk straight past the garden, then turn left at the student centre. The library is beside the cafe."),
                step("解释借阅", "What do I need to borrow a book, and what else is available there?", "说明借书条件，并介绍一种可用资源。",
                    "想想 student card / books / study spaces。", "You need a ___ to borrow a book. You can also ___.", "You need a student card to borrow a book. Quiet study spaces are also available there."),
                step("解决问题", "I arrive at seven this evening. Can I borrow a book then? What should I do?", "根据开放时间判断，并提供一个替代办法。",
                    "对比 six 和 seven，再想 tomorrow / welcome desk。", "The library closes at ___. You could ___.", "The library closes at six, so you cannot borrow a book at seven. You could come back tomorrow or ask the welcome desk for help.")))
    );
    private static WordTask word(String word, String meaning, String en, String cn, String... options) {
        var choices = new java.util.ArrayList<>(List.of(options));
        java.util.Collections.rotate(choices, Math.floorMod(word.hashCode(), choices.size()));
        return new WordTask(word, meaning, en, cn, choices);
    }
    private static SceneStep step(String title, String question, String purpose, String... hints) {
        return new SceneStep(title, question, purpose, List.of(hints));
    }
    public List<Lesson> all() { return lessons; }
    public Lesson require(String key) {
        return lessons.stream().filter(lesson -> lesson.key().equals(key)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("主题任务不存在"));
    }
    public List<Coverage> coverage(String key, int step, String answer) {
        List<List<String>> groups;
        List<String> labels;
        if ("water".equals(key)) {
            groups = switch (step) {
                case 0 -> List.of(List.of("reduce", "waste", "save", "saving"), List.of("water", "tap", "taps"));
                case 1 -> List.of(List.of("sensor", "sensors"), List.of("hand", "hands", "detect", "detects", "stop", "stops", "flow", "flows"));
                default -> List.of(List.of("report", "check", "fix", "repair"), List.of("tap", "leak", "leaking", "water"));
            };
            labels = switch (step) { case 0 -> List.of("问题与目标", "水或水龙头"); case 1 -> List.of("提到传感器", "描述检测或水流"); default -> List.of("具体行动", "行动对象"); };
        } else {
            groups = switch (step) {
                case 0 -> List.of(List.of("straight", "left", "right", "beside", "past"), List.of("garden", "centre", "center", "cafe", "library"));
                case 1 -> List.of(List.of("card"), List.of("study", "spaces", "books", "available"));
                default -> List.of(List.of("six", "6", "closed", "closes", "cannot", "can't"), List.of("tomorrow", "desk", "earlier", "morning"));
            };
            labels = switch (step) { case 0 -> List.of("方向表达", "地点参照"); case 1 -> List.of("借阅条件", "可用资源"); default -> List.of("开放时间或限制", "替代办法"); };
        }
        List<Coverage> result = new java.util.ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            List<String> evidence = groups.get(i).stream().filter(word -> contains(answer, word)).toList();
            result.add(new Coverage(labels.get(i), !evidence.isEmpty(), evidence));
        }
        return result;
    }
    public static boolean contains(String answer, String word) {
        return java.util.regex.Pattern.compile("(?i)(?<![a-z])" + java.util.regex.Pattern.quote(word) + "(?![a-z])").matcher(answer).find();
    }
}
