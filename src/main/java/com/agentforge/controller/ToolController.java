package com.agentforge.controller;

import com.agentforge.model.common.ApiResponse;
import com.agentforge.model.vo.ToolVO;
import com.agentforge.service.ToolFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ToolController {

    private final ToolFacadeService toolFacadeService;

    /** GET /api/tools → ToolVO[]（仅 OPTIONAL，api-contract.md §6） */
    @GetMapping("/tools")
    public ApiResponse<List<ToolVO>> getTools() {
        return ApiResponse.success(toolFacadeService.getOptionalTools());
    }
}
