package com.agentforge.controller;

import com.agentforge.model.common.ApiResponse;
import com.agentforge.model.response.Responses.CreateDocumentResponse;
import com.agentforge.model.response.Responses.GetDocumentsResponse;
import com.agentforge.service.DocumentFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentFacadeService documentFacadeService;

    @GetMapping("/documents/kb/{kbId}")
    public ApiResponse<GetDocumentsResponse> getDocumentsByKbId(@PathVariable String kbId) {
        return ApiResponse.success(GetDocumentsResponse.builder()
                .documents(documentFacadeService.getDocumentsByKbId(kbId))
                .build());
    }

    /** md 上传 + 分块 + 向量化（M6 实现，当前占位报错说明） */
    @PostMapping("/documents/upload")
    public ApiResponse<CreateDocumentResponse> uploadDocument(@RequestParam("kbId") String kbId,
                                                              @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(CreateDocumentResponse.builder()
                .documentId(documentFacadeService.uploadDocument(kbId, file))
                .build());
    }

    @DeleteMapping("/documents/{documentId}")
    public ApiResponse<Void> deleteDocument(@PathVariable String documentId) {
        documentFacadeService.deleteDocument(documentId);
        return ApiResponse.success();
    }
}
