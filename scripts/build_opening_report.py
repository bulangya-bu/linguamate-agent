# -*- coding: utf-8 -*-
from pathlib import Path

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.shared import Pt


PROJECT_ROOT = Path(__file__).resolve().parents[1]
TEMPLATE_DIR = Path(r"D:\TIM")
OUTPUT_PATH = PROJECT_ROOT / "output" / "软件工程_22215120234_周思汉_开题报告_智能英语学习辅助系统_参考文献中文增强版.docx"


TITLE = "基于Spring Boot和Vue的智能英语学习辅助系统的设计与实现"


SECTION_1 = [
    ("一、选题的意义与目的", "heading"),
    (
        "随着教育信息化和人工智能技术的快速发展，英语学习正在由传统的课堂讲授和单一题库练习，逐步转向数据驱动、即时反馈和个性化辅助相结合的新模式。大学生在英语学习过程中通常需要同时完成词汇记忆、口语表达、阅读理解、写作训练和阶段性复习等任务，但现有工具往往功能分散，口语练习缺少持续上下文，写作批改反馈不够及时，词汇复习也难以根据学习记录动态调整。尤其在备考大学英语四六级、研究生英语或日常英语能力提升时，学习者更需要一个能够集中管理学习过程、提供智能反馈并形成学习闭环的辅助平台。",
        "body",
    ),
    (
        "本课题拟设计并实现一个基于Spring Boot和Vue的智能英语学习辅助系统。系统面向高校学生和英语自学用户，围绕“练习、反馈、复习、提升”的学习流程，整合英语对话练习、CET词汇学习、错词复习、作文批改、英语新闻阅读、知识库问答和学习报告等功能。后端采用Spring Boot构建RESTful服务，结合MyBatis-Plus与MySQL完成数据持久化；前端采用Vue构建单页应用界面；智能服务部分接入OpenAI兼容大模型接口，并通过轻量级RAG检索和工具调用机制，为用户提供更有针对性的学习建议。",
        "body",
    ),
    (
        "本课题的研究目的主要体现在以下几个方面：第一，构建一个功能相对完整的英语学习辅助平台，使用户能够在同一系统中完成词汇、阅读、写作和对话训练；第二，利用智能问答、作文反馈和阅读查词等功能提高学习反馈效率，缓解传统英语学习中反馈滞后的问题；第三，通过学习记录、错词统计、复习计划和学习报告，实现学习过程的数据化管理；第四，在系统开发过程中综合运用Spring Boot、Vue、MyBatis-Plus、MySQL、RAG和大模型API接入等技术，提升本人在企业级Web系统设计、开发、测试和部署方面的综合实践能力。",
        "body",
    ),
    (
        "因此，本课题不仅具有一定的应用价值，也具有较强的工程实践意义。系统的设计与实现能够为英语学习者提供一个便捷、可扩展、可演示的智能学习环境，同时也为人工智能技术在语言学习场景中的落地应用提供一个具体案例。",
        "body",
    ),
]


SECTION_2 = [
    ("二、文献综述", "heading"),
    (
        "近年来，国内外关于英语学习平台、前后端分离系统、在线学习、计算机辅助语言学习和学习反馈机制等方向的研究不断增多，为本课题提供了理论基础和技术参考。结合本系统的功能特点，文献综述主要从国内研究现状、国外研究现状和相关技术研究三个方面展开。",
        "body",
    ),
    ("一、关于国内的研究现状", "subheading"),
    (
        "国内关于英语学习平台的研究多集中在词汇训练、在线测评、写作反馈、口语练习和阅读推广等方向。余植铖等设计的单词学习系统从词汇记忆、练习检测和学习记录等方面对背单词流程进行了系统化设计，对本课题的词汇模块和复习模块具有参考价值[6]。严天钦等探讨了人工智能赋能大学英语口语教学的方式，认为智能技术可以在口语练习、即时反馈和学习兴趣提升方面发挥作用[7]。刘秀梅等围绕生成式人工智能辅助大学生英语写作开展研究，指出生成式AI能够帮助学习者获得更及时的修改建议和表达优化方案[8]。张可昀关于在线考试系统的研究，则对本系统练习检测、学习反馈和测试管理功能具有借鉴意义[11]。这些研究说明，英语学习系统在词汇、口语、写作、测试和反馈等具体场景中已经形成了一定研究基础。",
        "body",
    ),
    (
        "与此同时，国内针对Spring Boot、Vue等技术的教育类和管理类系统研究也较为丰富。邱璐璐、陈俊仁设计的高校代领系统采用Spring Boot和Vue构建前后端分离应用，在系统结构、接口交互和业务流程设计方面具有一定参考价值[5]。王科翔等设计的智慧学习平台、杜瑞庆等设计的C语言程序在线测评系统，以及王龙军等关于数字阅读推广系统问卷模块的研究，均体现了前后端分离架构在学习平台、测评系统和阅读服务场景中的适用性[9-10][12]。此类研究说明，Spring Boot和Vue组合适合中小型Web系统的快速开发，但在英语学习场景中，仍需要进一步结合学习记录、复习计划、阅读查词和写作反馈等业务特点进行针对性设计。",
        "body",
    ),
    ("二、关于国外的研究现状", "subheading"),
    (
        "国外研究和技术资料更强调工程方法、框架实践和学习效果验证。Walls对Spring Boot的自动配置、应用开发流程和服务构建方式进行了系统介绍，为后端框架选型提供了参考[1]；Hanchett和Listwon围绕Vue响应式界面、组件化开发和单页应用实践进行了阐述，为前端交互设计提供了依据[2]；Pressman和Maxim从软件工程过程、需求分析、设计、测试和维护等方面提出了较完整的方法体系，对本课题的系统开发流程具有指导意义[3]；Chukharev-Hudilainen和Klepikova关于计算机辅助间隔重复学习的研究，则为词汇复习计划、错词回顾和学习效果跟踪设计提供了依据[4]。",
        "body",
    ),
    (
        "总体来看，国外资料在框架实践、软件工程方法和学习机制研究方面较为系统；国内研究更贴近高校在线学习、英语学习和Spring Boot+Vue应用开发实践。将两者结合，可以为本课题提供较完整的研究思路：一方面采用Spring Boot和Vue实现稳定的业务系统，另一方面围绕词汇复习、写作反馈、阅读查词、练习测评和学习报告等具体学习任务进行功能整合，从而提高系统的实用性、可靠性和可扩展性。",
        "body",
    ),
]


SECTION_3 = [
    ("三、研究方案", "heading"),
    ("（一）研究框架或设计思路", "subheading"),
    (
        "本课题拟开发的智能英语学习辅助系统采用前后端分离架构。前端基于Vue构建单页应用，负责用户界面展示、学习任务交互、阅读查词浮层、作文批改结果展示和学习报告可视化等功能；后端基于Spring Boot提供统一的RESTful API，负责用户认证、业务处理、数据持久化、智能服务编排和系统配置管理。数据库采用MySQL存储用户、会话、词汇、作文、阅读、知识库和学习报告等结构化数据，MyBatis-Plus用于简化数据访问层开发。",
        "body",
    ),
    (
        "系统整体分为用户与权限管理、英语对话练习、词汇学习、错词复习、作文批改、英语新闻阅读、知识库检索、智能Agent编排、学习报告和后台管理等模块。用户登录后可在学习工作台查看今日任务和学习概况；在对话模块中选择日常、考试、面试、旅行等场景进行英语交流；在词汇模块中完成CET-4/CET-6词汇练习、收藏、掌握标记和错词复习；在写作模块中提交作文，系统返回总分、分项反馈、逐句建议和润色结果；在阅读模块中查看分级英语新闻，支持悬停查词、阅读记录和推荐；在报告模块中查看练习次数、错词变化、作文得分趋势和阅读进度。",
        "body",
    ),
    (
        "智能服务层是本课题的重点之一。系统设计了Observe、Think、Act、Answer四阶段Agent流程：Observe阶段记录用户输入和当前学习上下文；Think阶段判断任务类型，如词汇查询、作文批改、阅读推荐或语法解释；Act阶段调用词典工具、作文评分工具、复习计划工具、新闻推荐工具或知识库检索服务；Answer阶段生成面向用户的反馈内容并保存学习记录。该流程能够在前端展示一定的处理轨迹，使智能反馈不只是黑盒结果，而是具有可解释性和可追踪性。",
        "body",
    ),
    (
        "RAG知识库采用轻量级检索方案，主要存储作文评分标准、常见语法错误、英语学习规则和阅读材料等内容。系统通过DocumentChunkService对知识文档进行切分，通过RetrievalService按关键词或标签召回相关知识片段，再由提示词工厂将检索内容注入大模型请求。考虑到毕业设计演示环境可能存在外部API不可用的情况，系统还设计本地兜底逻辑：当大模型接口不可用时，对话和写作模块仍能返回基础反馈，保证系统可运行、可测试、可演示。",
        "body",
    ),
    ("（二）研究主要内容（大纲）", "subheading"),
    ("第1章 引言", "outline"),
    ("1.1 课题背景及意义", "outline"),
    ("1.2 国内外研究现状", "outline"),
    ("1.3 课题研究内容与论文组织结构", "outline"),
    ("第2章 相关技术介绍", "outline"),
    ("2.1 Spring Boot框架", "outline"),
    ("2.2 Vue前端框架", "outline"),
    ("2.3 MyBatis-Plus与MySQL数据库", "outline"),
    ("2.4 大模型API、Agent流程与RAG检索增强生成", "outline"),
    ("第3章 系统需求分析", "outline"),
    ("3.1 用户角色与业务场景分析", "outline"),
    ("3.2 系统功能需求分析", "outline"),
    ("3.3 系统非功能需求分析", "outline"),
    ("第4章 系统总体设计", "outline"),
    ("4.1 系统架构设计", "outline"),
    ("4.2 功能模块设计", "outline"),
    ("4.3 数据库设计", "outline"),
    ("4.4 Agent流程与RAG知识库设计", "outline"),
    ("第5章 系统详细实现", "outline"),
    ("5.1 用户认证与学习工作台实现", "outline"),
    ("5.2 英语对话与智能反馈实现", "outline"),
    ("5.3 词汇学习与错词复习实现", "outline"),
    ("5.4 作文批改与英语新闻阅读实现", "outline"),
    ("5.5 学习报告与后台管理实现", "outline"),
    ("第6章 系统测试与总结", "outline"),
    ("6.1 功能测试", "outline"),
    ("6.2 兼容性与异常场景测试", "outline"),
    ("6.3 总结与展望", "outline"),
    ("（三）研究方法", "subheading"),
    (
        "1.文献研究法。通过查阅智能英语学习、自动写作评价、计算机辅助语言学习、RAG和Spring Boot/Vue系统开发相关文献，分析国内外研究现状，明确本课题的研究价值和功能定位。",
        "body",
    ),
    (
        "2.需求分析法。围绕大学生英语学习中的词汇记忆、写作反馈、阅读查词、口语练习和阶段性复习等需求，梳理用户使用场景，确定系统功能模块、数据结构和交互流程。",
        "body",
    ),
    (
        "3.系统设计与开发法。采用前后端分离思想进行系统设计，后端使用Spring Boot完成接口、业务逻辑、智能服务编排和数据访问，前端使用Vue完成页面组件、路由和交互体验，数据库使用MySQL完成持久化存储。",
        "body",
    ),
    (
        "4.测试与迭代法。通过接口测试、功能测试和异常场景测试验证系统可用性，重点覆盖词汇练习、作文批改、知识库检索、新闻推荐、模型接口失败兜底等场景，并根据测试结果优化提示词、工具调用和页面交互。",
        "body",
    ),
]


SECTION_4 = [
    ("四、工作进度计划", "heading"),
    ("2025年09月10日 - 2025年09月18日，确定选题，完成英语学习辅助系统的需求调研与可行性分析。", "body"),
    ("2025年09月20日 - 2025年10月08日，查阅智能英语学习、Spring Boot、Vue、RAG等相关资料，完成开题报告。", "body"),
    ("2025年10月10日 - 2025年10月20日，完成系统需求分析、功能模块划分和数据库初步设计。", "body"),
    ("2025年10月21日 - 2025年11月20日，完成Spring Boot后端框架搭建、用户认证、词汇、会话、作文、阅读和知识库等核心接口开发。", "body"),
    ("2025年11月21日 - 2025年12月20日，完成Vue前端页面设计与开发，实现学习工作台、对话练习、词汇学习、作文批改、新闻阅读和学习报告等界面。", "body"),
    ("2025年12月22日 - 2026年01月20日，完成大模型接口接入、Agent流程、RAG检索、工具调用和本地兜底逻辑优化。", "body"),
    ("2026年01月21日 - 2026年03月01日，进行系统测试、功能完善、异常场景处理和毕业论文初稿撰写。", "body"),
    ("2026年03月05日 - 2026年04月05日，完成论文修改、系统演示材料整理、定稿和答辩准备。", "body"),
]


SECTION_5 = [
    ("五、参考文献", "heading"),
    ("[1]Walls C. Spring Boot in Action[M]. Shelter Island: Manning Publications, 2016.", "reference"),
    ("[2]Hanchett E, Listwon B. Vue.js in Action[M]. Shelter Island: Manning Publications, 2018.", "reference"),
    ("[3]Pressman R S, Maxim B R. Software Engineering: A Practitioner's Approach[M]. 9th ed. New York: McGraw-Hill Education, 2020.", "reference"),
    ("[4]Chukharev-Hudilainen E, Klepikova T A. The effectiveness of computer-based spaced repetition in foreign language vocabulary instruction: a double-blind study[J]. CALICO Journal, 2016, 33(3): 334-354.", "reference"),
    ("[5]邱璐璐, 陈俊仁. 基于SpringBoot和Vue框架的高校代领系统设计与实现[J]. 信息技术与信息化, 2021(04): 25-27.", "reference"),
    ("[6]余植铖, 邓晓磊, 伍梦琴, 等. 单词学习系统的设计与实现[J]. 计算机科学与应用, 2024, 14(12): 36-43.", "reference"),
    ("[7]严天钦, 郑启奎, 黄金瑞. 人工智能赋能大学英语口语教学探究[J]. 社会科学前沿, 2024, 13(8): 38-43.", "reference"),
    ("[8]刘秀梅, 董静, 田静莹. 生成式人工智能辅助大学生英语写作效能研究[J]. 教育进展, 2025, 15(6): 934-941.", "reference"),
    ("[9]王科翔, 刘星含, 娄志城, 等. 基于Spring Boot+Vue框架的智慧学习平台的设计与实现[J]. 无线互联科技, 2023, 20(14): 65-68.", "reference"),
    ("[10]杜瑞庆, 李一诺, 黎作鹏. 基于Vue和SpringBoot的C语言程序在线测评系统的设计与实现[J]. 唐山师范学院学报, 2023, 45(03): 58-63.", "reference"),
    ("[11]张可昀. 基于Web的在线考试系统设计与实现[J]. 软件工程与应用, 2022, 11(4): 690-700.", "reference"),
    ("[12]王龙军, 王晶, 许靖唯. 基于Spring Boot与Vue.js的问卷模块在数字阅读推广系统中的设计与实现[J]. 内蒙古科技与经济, 2023(18): 115-118.", "reference"),
]


def find_template() -> Path:
    for path in TEMPLATE_DIR.glob("*.docx"):
        if "开题报告" in path.name and not path.name.startswith("~$"):
            return path
    raise FileNotFoundError("未找到开题报告模板")


def set_run_font(run, size=12, bold=False):
    run.font.name = "宋体"
    run._element.rPr.rFonts.set(qn("w:eastAsia"), "宋体")
    run.font.size = Pt(size)
    run.bold = bold


def clear_cell(cell):
    tc = cell._tc
    for child in list(tc):
        if child.tag != qn("w:tcPr"):
            tc.remove(child)


def add_para(cell, text, kind="body", style=None):
    p = cell.add_paragraph()
    if style is not None:
        p.style = style
    pf = p.paragraph_format
    pf.space_before = Pt(0)
    pf.space_after = Pt(0)
    pf.line_spacing = 1.5

    if kind == "heading":
        pf.first_line_indent = None
        run = p.add_run(text)
        set_run_font(run, size=12, bold=True)
    elif kind == "subheading":
        pf.first_line_indent = None
        run = p.add_run(text)
        set_run_font(run, size=12, bold=True)
    elif kind in {"outline", "reference"}:
        pf.first_line_indent = None
        run = p.add_run(text)
        set_run_font(run, size=11, bold=False)
    else:
        pf.first_line_indent = Pt(24)
        run = p.add_run(text)
        set_run_font(run, size=11, bold=False)
    return p


def set_title_cell(cell):
    clear_cell(cell)
    p = cell.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.line_spacing = 1.2
    run = p.add_run(TITLE)
    set_run_font(run, size=11, bold=False)


def fill_section(cell, items, style=None):
    clear_cell(cell)
    for text, kind in items:
        add_para(cell, text, kind=kind, style=style)


def fill_blank_opinion(cell, label, blank_lines=3):
    clear_cell(cell)
    add_para(cell, label, kind="heading")
    for _ in range(blank_lines):
        add_para(cell, "", kind="body")
    add_para(cell, "签字：                         年      月      日", kind="body")


def main():
    template = find_template()
    doc = Document(str(template))
    table = doc.tables[0]

    body_style = table.rows[3].cells[0].paragraphs[1].style

    set_title_cell(table.rows[2].cells[1])
    fill_section(table.rows[3].cells[0], SECTION_1, style=body_style)
    fill_section(table.rows[4].cells[0], SECTION_2, style=body_style)
    fill_section(table.rows[5].cells[0], SECTION_3, style=body_style)
    fill_section(table.rows[6].cells[0], SECTION_4, style=body_style)
    fill_section(table.rows[7].cells[0], SECTION_5, style=body_style)
    fill_blank_opinion(table.rows[8].cells[0], "指导教师意见：", blank_lines=2)
    fill_blank_opinion(table.rows[9].cells[0], "专业意见：", blank_lines=1)

    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    doc.save(str(OUTPUT_PATH))
    print(OUTPUT_PATH)


if __name__ == "__main__":
    main()
