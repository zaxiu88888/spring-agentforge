package com.agentforge.controller;

import com.agentforge.model.common.ApiResponse;
import com.agentforge.model.request.KnowledgeBaseRequests.CreateKnowledgeBaseRequest;
import com.agentforge.model.request.KnowledgeBaseRequests.UpdateKnowledgeBaseRequest;
import com.agentforge.model.response.Responses.CreateKnowledgeBaseResponse;
import com.agentforge.model.response.Responses.GetKnowledgeBasesResponse;
import com.agentforge.service.KnowledgeBaseFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseFacadeService knowledgeBaseFacadeService;

    @GetMapping("/knowledge-bases")
    public ApiResponse<GetKnowledgeBasesResponse> getKnowledgeBases() {
        return ApiResponse.success(GetKnowledgeBasesResponse.builder()
                .knowledgeBases(knowledgeBaseFacadeService.getKnowledgeBases())
                .build());
    }

    @PostMapping("/knowledge-bases")
    public ApiResponse<CreateKnowledgeBaseResponse> createKnowledgeBase(
            @RequestBody CreateKnowledgeBaseRequest request) {
        return ApiResponse.success(CreateKnowledgeBaseResponse.builder()
                .knowledgeBaseId(knowledgeBaseFacadeService.createKnowledgeBase(request))
                .build());
    }

    @PatchMapping("/knowledge-bases/{knowledgeBaseId}")
    public ApiResponse<Void> updateKnowledgeBase(@PathVariable String knowledgeBaseId,
                                                 @RequestBody UpdateKnowledgeBaseRequest request) {
        knowledgeBaseFacadeService.updateKnowledgeBase(knowledgeBaseId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/knowledge-bases/{knowledgeBaseId}")
    public ApiResponse<Void> deleteKnowledgeBase(@PathVariable String knowledgeBaseId) {
        knowledgeBaseFacadeService.deleteKnowledgeBase(knowledgeBaseId);
        return ApiResponse.success();
    }
}
