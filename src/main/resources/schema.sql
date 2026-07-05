CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64),
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL DEFAULT 1,
    scene VARCHAR(64),
    title VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_setting (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL DEFAULT 1,
    provider VARCHAR(64),
    base_url VARCHAR(255),
    api_key TEXT,
    chat_model VARCHAR(128),
    updated_at TIMESTAMP,
    UNIQUE KEY uk_ai_setting_user (user_id)
);

CREATE TABLE IF NOT EXISTS message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT,
    role VARCHAR(32),
    content TEXT,
    metadata TEXT,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS word (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(80) NOT NULL UNIQUE,
    phonetic_us VARCHAR(120),
    phonetic_uk VARCHAR(120),
    meaning_cn VARCHAR(255),
    part_of_speech VARCHAR(64),
    example_en TEXT,
    example_cn TEXT,
    level_tag VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS user_word_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL DEFAULT 1,
    word_id BIGINT NOT NULL,
    familiarity INT,
    wrong_count INT,
    review_count INT,
    favorite BOOLEAN DEFAULT FALSE,
    next_review_at TIMESTAMP,
    last_review_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS word_review_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL DEFAULT 1,
    word_id BIGINT NOT NULL,
    answer VARCHAR(255),
    correct BOOLEAN,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS word_ai_detail_cache (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word_id BIGINT NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    content_json TEXT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE KEY uk_word_ai_detail (word_id, model_name)
);

CREATE TABLE IF NOT EXISTS essay_submission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL DEFAULT 1,
    title VARCHAR(255),
    exam_type VARCHAR(64),
    original_text TEXT,
    polished_text TEXT,
    total_score DOUBLE,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS essay_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    essay_id BIGINT,
    category VARCHAR(64),
    original_sentence TEXT,
    suggestion TEXT,
    corrected_sentence TEXT
);

CREATE TABLE IF NOT EXISTS news_article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    summary TEXT,
    content TEXT,
    source_name VARCHAR(120),
    difficulty VARCHAR(32),
    topic_tags VARCHAR(255),
    created_at TIMESTAMP,
    UNIQUE KEY uk_news (title, source_name)
);

CREATE TABLE IF NOT EXISTS reading_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL DEFAULT 1,
    article_id BIGINT,
    read_progress INT,
    looked_up_words INT,
    finished BOOLEAN,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    source_type VARCHAR(64),
    tags VARCHAR(255),
    content TEXT,
    created_at TIMESTAMP,
    UNIQUE KEY uk_doc (title, source_type)
);

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT,
    chunk_index INT,
    content TEXT,
    keywords VARCHAR(255),
    embedding_ref VARCHAR(255),
    created_at TIMESTAMP,
    UNIQUE KEY uk_chunk (document_id, chunk_index)
);

CREATE TABLE IF NOT EXISTS user_level (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL DEFAULT 1,
    level INT DEFAULT 1,
    experience INT DEFAULT 0,
    total_experience INT DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
