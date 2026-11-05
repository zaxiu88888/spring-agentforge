package com.agentforge.service;

import com.agentforge.model.vo.ToolVO;

import java.util.List;

public interface ToolFacadeService {

    /** OPTIONAL 工具清单（GET /api/tools，Agent 表单勾选用） */
    List<ToolVO> getOptionalTools();
}
