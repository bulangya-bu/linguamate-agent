package com.example.javabigwork.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.javabigwork.chat.dto.ConversationView;
import com.example.javabigwork.chat.entity.Conversation;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ConversationMapper extends BaseMapper<Conversation> {
    @Select("""
            SELECT c.id, c.scene, c.title, c.updated_at,
              COALESCE(
                CASE WHEN JSON_VALID(u.metadata) THEN
                  CASE JSON_UNQUOTE(JSON_EXTRACT(u.metadata, '$.mode'))
                    WHEN 'practice' THEN 'practice' WHEN 'assistant' THEN 'assistant'
                  END
                END,
                CASE WHEN JSON_VALID(a.metadata) THEN
                  CASE WHEN JSON_TYPE(JSON_EXTRACT(a.metadata, '$.coaching')) = 'OBJECT' THEN 'practice' END
                END,
                'assistant'
              ) AS mode
            FROM conversation c
            LEFT JOIN (
              SELECT m.conversation_id,
                MAX(CASE WHEN m.role = 'user' THEN m.id END) AS user_message_id,
                MAX(CASE WHEN m.role = 'assistant' THEN m.id END) AS assistant_message_id
              FROM message m JOIN conversation owned ON owned.id = m.conversation_id
              WHERE owned.user_id = #{userId}
              GROUP BY m.conversation_id
            ) recent ON recent.conversation_id = c.id
            LEFT JOIN message u ON u.id = recent.user_message_id
            LEFT JOIN message a ON a.id = recent.assistant_message_id
            WHERE c.user_id = #{userId}
            ORDER BY c.updated_at DESC, c.id DESC
            """)
    List<ConversationView> selectViews(@Param("userId") Long userId);
}
