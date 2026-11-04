package com.agentforge.mapper;

import com.agentforge.model.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMessageMapper {
    int insert(ChatMessage message);

    List<ChatMessage> selectBySessionId(@Param("sessionId") String sessionId);

    /** 最近 N 条（DESC LIMIT），调用方自行反转为时间正序 */
    List<ChatMessage> selectRecent(@Param("sessionId") String sessionId, @Param("limit") int limit);
}
