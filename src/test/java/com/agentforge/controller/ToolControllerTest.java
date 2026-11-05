package com.agentforge.controller;

import com.agentforge.service.ToolFacadeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/tools 契约（api-contract.md §6：data 直接是数组） */
@WebMvcTest(ToolController.class)
class ToolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ToolFacadeService toolFacadeService;

    @Test
    @DisplayName("返回 OPTIONAL 工具数组，data 即列表本身")
    void returnsOptionalTools() throws Exception {
        when(toolFacadeService.getOptionalTools()).thenReturn(List.of(
                new com.agentforge.model.vo.ToolVO("sqlQuery", "查询演示库", "OPTIONAL"),
                new com.agentforge.model.vo.ToolVO("getCurrentTime", "当前时间", "OPTIONAL")));

        mockMvc.perform(get("/api/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("sqlQuery"))
                .andExpect(jsonPath("$.data[0].type").value("OPTIONAL"));
    }
}
