package com.agentforge.agent;

import com.agentforge.model.dto.ChatMessageDTO;
import com.agentforge.model.vo.ChatMessageVO;

/** Agent 产出消息的下沉通道：落库（agentCreateChatMessage，不发事件防递归）并回带可推送的 VO */
public interface AgentMessageSink {

    ChatMessageVO persist(ChatMessageDTO dto);
}
