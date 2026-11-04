package com.agentforge.mapper;

import com.agentforge.model.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatSessionMapper {
    int insert(ChatSession session);

    ChatSession selectById(String id);

    List<ChatSession> selectAll();

    List<ChatSession> selectByAgentId(@Param("agentId") String agentId);

    int deleteById(String id);

    int updateById(ChatSession session);
}
